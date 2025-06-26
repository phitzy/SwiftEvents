package com.swiftevents.events;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.api.events.*;
import com.swiftevents.chat.ChatManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EventManager {
    
    private final SwiftEventsPlugin plugin;
    
    // Optimization: Use more efficient collections with proper sizing
    private final Map<String, Event> activeEvents;
    private final Map<String, Event> allEvents;
    
    // Task management
    private BukkitTask autoSaveTask;
    private BukkitTask eventUpdateTask;
    
    // Optimization: Cache frequently accessed values and reduce object creation
    private long lastHookUpdate = 0;
    private long lastCacheCleanup = 0;
    private static final long HOOK_UPDATE_INTERVAL = 60000; // 60 seconds
    private static final long EVENT_UPDATE_INTERVAL = 1000; // 1 second
    private static final long CACHE_CLEANUP_INTERVAL = 300000; // 5 minutes
    
    // Optimization: Pre-allocated collections to reduce GC pressure
    private final List<Event> eventUpdateBuffer = new ArrayList<>(32);
    private final Set<UUID> playersToNotify = new HashSet<>(16);
    private final List<Event> eventsToSave = new ArrayList<>(16);
    
    // Optimization: Object pooling for string operations
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = 
        ThreadLocal.withInitial(() -> new StringBuilder(128));
    
    // Optimization: Cache for frequently computed values
    private final Map<String, String> hudMessageCache = new HashMap<>(64);
    private final Map<String, Long> hudCacheTimestamps = new HashMap<>(64);
    private static final long HUD_CACHE_DURATION = 5000; // 5 seconds
    
    public EventManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        
        // Optimization: Initialize with appropriate sizes and load factors
        this.activeEvents = new ConcurrentHashMap<>(16, 0.75f);
        this.allEvents = new ConcurrentHashMap<>(64, 0.75f);
        
        // Load existing events
        loadAllEvents();
        
        // Start auto-save task
        startAutoSave();
        
        // Start event update task
        startEventUpdater();
    }
    
    private void loadAllEvents() {
        plugin.getDatabaseManager().loadAllEvents().thenAccept(events -> {
            // Synchronize with the main thread to avoid race conditions
            Bukkit.getScheduler().runTask(plugin, () -> {
                int loadedCount = 0;
                for (Event event : events) {
                    // Only add if not already present (to avoid overwriting newly created events)
                    if (!allEvents.containsKey(event.getId())) {
                        allEvents.put(event.getId(), event);
                        if (event.isActive()) {
                            activeEvents.put(event.getId(), event);
                        }
                        loadedCount++;
                    }
                }
                plugin.getLogger().info("Loaded " + loadedCount + " events from storage");
                
                // Clean up any invalid events after loading
                cleanupInvalidEvents();
            });
        });
    }
    
    private void cleanupInvalidEvents() {
        Iterator<Map.Entry<String, Event>> iterator = allEvents.entrySet().iterator();
        int cleanedCount = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<String, Event> entry = iterator.next();
            Event event = entry.getValue();
            
            // Remove events that are very old and completed/cancelled
            if ((event.isCompleted() || event.isCancelled()) && 
                System.currentTimeMillis() - event.getCreatedAt() > 2592000000L) { // 30 days
                iterator.remove();
                activeEvents.remove(event.getId());
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            plugin.getLogger().info("Cleaned up " + cleanedCount + " old events");
        }
    }
    
    private void startAutoSave() {
        long interval = (long) plugin.getConfigManager().getAutoSaveInterval() * 20L; // Convert to ticks
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
                this::saveAllEvents, interval, interval);
    }
    
    private void startEventUpdater() {
        // Update events every second
        eventUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, 
                this::updateEvents, 20L, 20L);
    }
    
    // Optimized update method with batching and reduced allocations
    private void updateEvents() {
        long currentTime = System.currentTimeMillis();
        
        // Clear and reuse buffers
        eventUpdateBuffer.clear();
        eventUpdateBuffer.addAll(activeEvents.values());
        
        // Process events in batches to reduce lock contention
        for (Event event : eventUpdateBuffer) {
            try {
                updateSingleEvent(event, currentTime);
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating event " + event.getName() + ": " + e.getMessage());
            }
        }
        
        // Update HUD periodically for time-sensitive information
        if (plugin.getConfigManager().isHUDEnabled()) {
            plugin.getHUDManager().updateActiveEvents();
        }
        
        // Call hooks periodically instead of every update
        if (currentTime - lastHookUpdate > HOOK_UPDATE_INTERVAL) {
            lastHookUpdate = currentTime;
            callHookUpdates();
        }
        
        // Cleanup caches periodically
        if (currentTime - lastCacheCleanup > CACHE_CLEANUP_INTERVAL) {
            lastCacheCleanup = currentTime;
            cleanupCaches();
        }
    }
    
    private void updateSingleEvent(Event event, long currentTime) {
        boolean stateChanged = false;
        
        // Check if event should start
        if (event.isScheduled() && event.hasStarted()) {
            startEvent(event.getId());
            stateChanged = true;
        }
        
        // Check if event should end
        if (event.isActive() && event.hasEnded()) {
            endEvent(event.getId());
            stateChanged = true;
        }
        
        // Update HUD only if enabled and if there are participants
        if (plugin.getConfigManager().isHUDEnabled() && !event.getParticipants().isEmpty()) {
            updateEventHUD(event, currentTime);
        }
    }
    
    private void callHookUpdates() {
        // Batch hook calls to reduce overhead
        if (!activeEvents.isEmpty()) {
            // Reuse collection to avoid allocations
            eventUpdateBuffer.clear();
            eventUpdateBuffer.addAll(activeEvents.values());
            
            for (Event event : eventUpdateBuffer) {
                try {
                    plugin.getHookManager().callEventUpdate(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in hook update for event " + event.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    private void updateEventHUD(Event event, long currentTime) {
        String cacheKey = event.getId();
        
        // Check cache first
        String cachedMessage = hudMessageCache.get(cacheKey);
        Long cacheTime = hudCacheTimestamps.get(cacheKey);
        
        String message;
        if (cachedMessage != null && cacheTime != null && 
            (currentTime - cacheTime) < HUD_CACHE_DURATION) {
            message = cachedMessage;
        } else {
            message = formatEventHUD(event);
            hudMessageCache.put(cacheKey, message);
            hudCacheTimestamps.put(cacheKey, currentTime);
        }
        
        // Clear and reuse set
        playersToNotify.clear();
        playersToNotify.addAll(event.getParticipants());
        
        if (playersToNotify.isEmpty()) return;
        
        // Batch player updates
        for (UUID participantId : playersToNotify) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                plugin.getHUDManager().sendActionBarMessage(player, message);
            }
        }
    }
    
    private String formatEventHUD(Event event) {
        // Optimization: Use ThreadLocal StringBuilder for string concatenation
        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0); // Clear the builder
        
        if (event.getStatus() == Event.EventStatus.SCHEDULED) {
            sb.append("§6").append(event.getName())
                    .append(" §7| §aWaiting for players...")
                    .append(" §7| §e").append(event.getFormattedRemainingTime())
                    .append(" to start");
        } else {
            sb.append("§6").append(event.getName())
                    .append(" §7| §a").append(event.getStatus().name())
                    .append(" §7| §e").append(event.getFormattedRemainingTime())
                    .append(" remaining");
        }
        
        return sb.toString();
    }
    
    private void cleanupCaches() {
        long currentTime = System.currentTimeMillis();
        
        // Clean HUD cache
        Iterator<Map.Entry<String, Long>> iterator = hudCacheTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime - entry.getValue() > HUD_CACHE_DURATION * 2) { // Keep cache 2x longer than validity
                iterator.remove();
                hudMessageCache.remove(entry.getKey());
            }
        }
    }
    
    // Event creation and management methods
    public Event createEvent(String name, String description, Event.EventType type, UUID creatorId) {
        if (activeEvents.size() >= plugin.getConfigManager().getMaxConcurrentEvents()) {
            return null; // Too many active events
        }
        
        Event event = new Event(name, description, type);
        event.setCreatedBy(creatorId);
        
        allEvents.put(event.getId(), event);
        
        // Fire Bukkit event
        SwiftEventCreateEvent createEvent = new SwiftEventCreateEvent(event);
        Bukkit.getPluginManager().callEvent(createEvent);
        
        // Save the event
        plugin.getDatabaseManager().saveEvent(event);
        
        // Call hooks after creation
        plugin.getHookManager().callEventCreated(event);

        // Announce the event creation if it's scheduled
        if (event.isScheduled()) {
            plugin.getChatManager().announceEvent(event, ChatManager.EventAnnouncement.CREATED);
        }
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();
        
        return event;
    }
    
    public boolean deleteEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null) {
            return false;
        }
        
        allEvents.remove(eventId);
        activeEvents.remove(eventId);
        
        // Clean up caches for this event
        hudMessageCache.remove(eventId);
        hudCacheTimestamps.remove(eventId);
        
        // Fire Bukkit event
        SwiftEventEndEvent endEvent = new SwiftEventEndEvent(event, "deleted");
        Bukkit.getPluginManager().callEvent(endEvent);
        
        // Delete from database asynchronously
        plugin.getDatabaseManager().deleteEvent(eventId);
        
        // Call hooks after deletion
        plugin.getHookManager().callEventEnded(event, "deleted");
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();
        
        return true;
    }
    
    public boolean startEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null || !event.canStart()) {
            return false;
        }
        
        // Call hooks before starting
        if (!plugin.getHookManager().callEventPreStart(event)) {
            return false; // Hook cancelled start
        }
        
        event.setStatus(Event.EventStatus.ACTIVE);
        if (event.getStartTime() <= 0) {
            event.setStartTime(System.currentTimeMillis());
        }
        
        activeEvents.put(eventId, event);
        
        // Teleport all participants to the event location
        if (event.hasLocation()) {
            for (UUID participantId : event.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null) {
                    teleportToEvent(participant, eventId);
                }
            }
        }
        
        // Fire Bukkit event
        SwiftEventStartEvent startEvent = new SwiftEventStartEvent(event);
        Bukkit.getPluginManager().callEvent(startEvent);

        // Announce the event start
        plugin.getChatManager().announceEvent(event, ChatManager.EventAnnouncement.STARTING);
        
        // Notify participants
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event_name", event.getName());
        notifyParticipants(event, "event_started", placeholders);
        
        // Save the updated event
        plugin.getDatabaseManager().saveEvent(event);
        
        // Call hooks after starting
        plugin.getHookManager().callEventStarted(event);
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();
        
        return true;
    }
    
    public boolean endEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null || (!event.isActive() && !event.isScheduled())) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.COMPLETED);
        if (event.getEndTime() <= 0) {
            event.setEndTime(System.currentTimeMillis());
        }
        
        activeEvents.remove(eventId);
        
        // Clean up caches for this event
        hudMessageCache.remove(eventId);
        hudCacheTimestamps.remove(eventId);
        
        // Fire Bukkit event
        SwiftEventEndEvent endEvent = new SwiftEventEndEvent(event, "completed");
        Bukkit.getPluginManager().callEvent(endEvent);

        // Announce the event end
        plugin.getChatManager().announceEvent(event, ChatManager.EventAnnouncement.ENDED);
        
        // Distribute rewards and notify participants
        distributeRewards(event);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event_name", event.getName());
        notifyParticipants(event, "event_ended", placeholders);
        
        // Save the updated event
        plugin.getDatabaseManager().saveEvent(event);
        
        // Call hooks after ending
        plugin.getHookManager().callEventEnded(event, "completed");
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();
        
        return true;
    }
    
    public boolean cancelEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null || event.isCompleted() || event.isCancelled()) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.CANCELLED);
        activeEvents.remove(eventId);
        
        // Clean up caches for this event
        hudMessageCache.remove(eventId);
        hudCacheTimestamps.remove(eventId);
        
        // Fire Bukkit event
        SwiftEventEndEvent endEvent = new SwiftEventEndEvent(event, "cancelled");
        Bukkit.getPluginManager().callEvent(endEvent);
        
        // Notify participants
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event_name", event.getName());
        notifyParticipants(event, "event_cancelled", placeholders);
        
        // Save the updated event
        plugin.getDatabaseManager().saveEvent(event);
        
        // Call hooks after cancellation
        plugin.getHookManager().callEventEnded(event, "cancelled");
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();
        
        return true;
    }
    
    public boolean pauseEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null || !event.isActive()) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.PAUSED);
        
        // Notify participants
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event_name", event.getName());
        notifyParticipants(event, "event_paused", placeholders);
        
        // Save the updated event
        plugin.getDatabaseManager().saveEvent(event);
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();
        
        return true;
    }
    
    public boolean resumeEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null || event.getStatus() != Event.EventStatus.PAUSED) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.ACTIVE);
        
        // Notify participants
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event_name", event.getName());
        notifyParticipants(event, "event_resumed", placeholders);
        
        // Save the updated event
        plugin.getDatabaseManager().saveEvent(event);
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();
        
        return true;
    }
    
    public boolean joinEvent(String eventId, UUID playerId) {
        Event event = allEvents.get(eventId);
        if (event == null || !event.canJoin()) {
            return false;
        }
        
        // Check if player is already in too many events
        long playerEventCount = allEvents.values().stream()
                .filter(e -> e.isActive() && e.isParticipant(playerId))
                .count();
        
        if (playerEventCount >= plugin.getConfigManager().getMaxEventsPerPlayer()) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        
        // Call hooks before joining
        if (player != null && !plugin.getHookManager().callPlayerPreJoin(player, event)) {
            return false; // Hook cancelled join
        }
        
        // Store the participant count before adding the new player for notifications
        int participantsBeforeJoin = event.getCurrentParticipants();
        
        if (!event.addParticipant(playerId)) {
            return false; // Event is full or player already in event
        }
        
        // Teleport player to lobby if the event is scheduled
        if (player != null && event.isScheduled() && event.hasLocation()) {
            teleportToEvent(player, eventId);
        }
        
        // Fire Bukkit event
        SwiftEventPlayerJoinEvent joinEvent = new SwiftEventPlayerJoinEvent(event, playerId, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            event.removeParticipant(playerId);
            return false;
        }
        
        // Notify player
        if (player != null) {
            String message = plugin.getConfigManager().replacePlaceholder(
                plugin.getConfigManager().getMessage("event_joined"), 
                "event_name", 
                event.getName()
            );
            player.sendMessage(plugin.getConfigManager().getPrefix() + message);
        }
        
        // Notify all participants in staging events when someone joins
        if (event.getStatus() == Event.EventStatus.SCHEDULED && player != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("current_participants", String.valueOf(participantsBeforeJoin + 1));
            placeholders.put("max_participants", event.hasUnlimitedSlots() ? "∞" : String.valueOf(event.getMaxParticipants()));
            
            String notificationMessage = plugin.getConfigManager().getMessage("player_joined_staging", placeholders);
            
            // Send to all current participants (excluding the one who just joined to avoid duplicate messages)
            for (UUID participantId : event.getParticipants()) {
                if (!participantId.equals(playerId)) { // Exclude the new player
                    Player participant = Bukkit.getPlayer(participantId);
                    if (participant != null && participant.isOnline()) {
                        participant.sendMessage(plugin.getConfigManager().getPrefix() + notificationMessage);
                    }
                }
            }
        }
        
        // Save the updated event
        plugin.getDatabaseManager().saveEvent(event);
        
        // Call hooks after joining
        if (player != null) {
            plugin.getHookManager().callPlayerJoined(player, event);
        }
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();
        
        return true;
    }
    
    public boolean leaveEvent(String eventId, UUID playerId) {
        Event event = allEvents.get(eventId);
        if (event == null || !event.isParticipant(playerId)) {
            return false;
        }

        Player player = Bukkit.getPlayer(playerId);

        // Fire Bukkit event
        SwiftEventPlayerLeaveEvent leaveEvent = new SwiftEventPlayerLeaveEvent(event, playerId, player, "manual");
        Bukkit.getPluginManager().callEvent(leaveEvent);

        // Notify player
        if (player != null) {
            String message = plugin.getConfigManager().replacePlaceholder(
                plugin.getConfigManager().getMessage("event_left"), 
                "event_name", 
                event.getName()
            );
            player.sendMessage(plugin.getConfigManager().getPrefix() + message);
        }

        // Notify remaining participants in staging events when someone leaves
        if (event.getStatus() == Event.EventStatus.SCHEDULED && player != null) {
            // Calculate the participant count after the player leaves
            int participantsAfterLeave = event.getCurrentParticipants() - 1;
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("current_participants", String.valueOf(participantsAfterLeave));
            placeholders.put("max_participants", event.hasUnlimitedSlots() ? "∞" : String.valueOf(event.getMaxParticipants()));
            
            String notificationMessage = plugin.getConfigManager().getMessage("player_left_staging", placeholders);
            
            // Send to all remaining participants
            for (UUID participantId : event.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null && participant.isOnline()) {
                    participant.sendMessage(plugin.getConfigManager().getPrefix() + notificationMessage);
                }
            }
        }

        event.removeParticipant(playerId);

        // Save the updated event
        saveEvent(event);

        // Call hooks after leaving
        plugin.getHookManager().callPlayerLeft(player, playerId, event, "manual");
        
        // Update HUD for all players
        plugin.getHUDManager().updateActiveEvents();

        return true;
    }
    
    public boolean teleportToEvent(Player player, String eventId) {
        Event event = getEvent(eventId);
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent not found.");
            return false;
        }

        if (!event.isActive() && !event.isScheduled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou can only teleport to an active or scheduled event.");
            return false;
        }

        if (!event.hasLocation()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cThis event does not have a location set.");
            return false;
        }

        if (!event.isParticipant(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou must be in the event to teleport to it.");
            return false;
        }
        
        Location teleportLocation = new Location(
                Bukkit.getWorld(event.getWorld()),
                event.getX(),
                event.getY(),
                event.getZ()
        );

        player.teleport(teleportLocation);
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§aTeleported to " + event.getName() + ".");
        return true;
    }
    
    private void notifyParticipants(Event event, String message) {
        // Optimization: Use enhanced for loop on keySet
        for (UUID participantId : event.getParticipants()) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + message);
            }
        }
    }
    
    private void notifyParticipants(Event event, String messageKey, Map<String, String> placeholders) {
        String message = plugin.getConfigManager().getMessage(messageKey, placeholders);
        notifyParticipants(event, message);
    }
    
    private void distributeRewards(Event event) {
        List<String> rewards = event.getRewards();
        if (rewards.isEmpty()) {
            return;
        }
        
        // Clear and reuse set
        playersToNotify.clear();
        playersToNotify.addAll(event.getParticipants());
        
        for (UUID participantId : playersToNotify) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                for (String reward : rewards) {
                    // Execute reward command
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                            reward.replace("{player}", player.getName()));
                }
            }
        }
    }
    
    // Getter methods
    public Event getEvent(String eventId) {
        return allEvents.get(eventId);
    }
    
    public List<Event> getAllEvents() {
        return new ArrayList<>(allEvents.values());
    }
    
    public List<Event> getActiveEvents() {
        return new ArrayList<>(activeEvents.values());
    }
    
    public List<Event> getEventsByType(Event.EventType type) {
        return allEvents.values().stream()
                .filter(event -> event.getType() == type)
                .collect(Collectors.toList());
    }
    
    public List<Event> getEventsByStatus(Event.EventStatus status) {
        return allEvents.values().stream()
                .filter(event -> event.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    public List<Event> getPlayerEvents(UUID playerId) {
        return allEvents.values().stream()
                .filter(event -> event.isParticipant(playerId))
                .collect(Collectors.toList());
    }
    
    public boolean isPlayerInEvent(UUID playerId) {
        return allEvents.values().stream()
                .anyMatch(event -> event.isActive() && event.isParticipant(playerId));
    }
    
    public boolean eventsConflict(Event event1, Event event2) {
        // Check for time overlap
        if (event1.getStartTime() < event2.getEndTime() && event2.getStartTime() < event1.getEndTime()) {
            // Check for location overlap if both events have locations
            if (event1.hasLocation() && event2.hasLocation()) {
                if (event1.getLocation().equals(event2.getLocation())) {
                    return true; // Same location and time overlap
                }
            }

            // Check for participant overlap
            Set<UUID> participants1 = new HashSet<>(event1.getParticipants());
            participants1.retainAll(event2.getParticipants());
            if (!participants1.isEmpty()) {
                return true; // Overlapping participants and time
            }
        }
        return false;
    }
    
    public void saveAllEvents() {
        // Clear and reuse collection
        eventsToSave.clear();
        eventsToSave.addAll(allEvents.values());
        
        // Save in batches for better performance
        plugin.getDatabaseManager().saveEvents(eventsToSave);
    }
    
    public void saveEvent(Event event) {
        plugin.getDatabaseManager().saveEvent(event);
    }
    
    public void shutdown() {
        // Cancel tasks
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        if (eventUpdateTask != null) {
            eventUpdateTask.cancel();
        }
        
        // Save all events before shutdown
        saveAllEvents();
        
        // Clear caches
        hudMessageCache.clear();
        hudCacheTimestamps.clear();
        
        plugin.getLogger().info("EventManager shutdown complete");
    }
} 