package com.swiftevents.events;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.api.events.*;
import com.swiftevents.chat.ChatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EventManager {
    
    private final SwiftEventsPlugin plugin;
    private final Map<String, Event> activeEvents;
    private final Map<String, Event> allEvents;
    private BukkitTask autoSaveTask;
    private BukkitTask eventUpdateTask;
    
    // Optimization: Cache frequently accessed values
    private long lastHookUpdate = 0;
    private static final long HOOK_UPDATE_INTERVAL = 60000; // 60 seconds
    private static final long EVENT_UPDATE_INTERVAL = 1000; // 1 second
    
    // Optimization: Pre-allocated collections to reduce GC pressure
    private final List<Event> eventUpdateBuffer = new ArrayList<>();
    private final Set<UUID> playersToNotify = new HashSet<>();
    
    public EventManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.activeEvents = new ConcurrentHashMap<>();
        this.allEvents = new ConcurrentHashMap<>();
        
        // Load existing events
        loadAllEvents();
        
        // Start auto-save task
        startAutoSave();
        
        // Start event update task
        startEventUpdater();
    }
    
    private void loadAllEvents() {
        plugin.getDatabaseManager().loadAllEvents().thenAccept(events -> {
            for (Event event : events) {
                allEvents.put(event.getId(), event);
                if (event.isActive()) {
                    activeEvents.put(event.getId(), event);
                }
            }
            plugin.getLogger().info("Loaded " + events.size() + " events from storage");
        });
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
    
    // Optimized update method
    private void updateEvents() {
        long currentTime = System.currentTimeMillis();
        
        // Clear buffer and reuse
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
        
        // Call hooks periodically instead of every update
        if (currentTime - lastHookUpdate > HOOK_UPDATE_INTERVAL) {
            lastHookUpdate = currentTime;
            callHookUpdates();
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
            updateEventHUD(event);
        }
    }
    
    private void callHookUpdates() {
        // Batch hook calls to reduce overhead
        if (!activeEvents.isEmpty()) {
            List<Event> eventsToUpdate = new ArrayList<>(activeEvents.values());
            for (Event event : eventsToUpdate) {
                try {
                    plugin.getHookManager().callEventUpdate(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in hook update for event " + event.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    private void updateEventHUD(Event event) {
        // Clear and reuse set
        playersToNotify.clear();
        playersToNotify.addAll(event.getParticipants());
        
        if (playersToNotify.isEmpty()) return;
        
        String message = formatEventHUD(event);
        
        // Batch player updates
        for (UUID participantId : playersToNotify) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                plugin.getHUDManager().sendHUDMessage(player, message);
            }
        }
    }
    
    private String formatEventHUD(Event event) {
        // Optimization: Use StringBuilder for string concatenation
        StringBuilder sb = new StringBuilder(64); // Pre-allocate reasonable capacity
        sb.append("§6").append(event.getName())
          .append(" §7| §a").append(event.getStatus().name())
          .append(" §7| §e").append(event.getFormattedRemainingTime())
          .append(" remaining");
        return sb.toString();
    }
    
    // Event creation and management methods
    public Event createEvent(String name, String description, Event.EventType type, UUID creatorId) {
        if (activeEvents.size() >= plugin.getConfigManager().getMaxConcurrentEvents()) {
            return null; // Too many active events
        }
        
        Event event = new Event(name, description, type);
        event.setCreatedBy(creatorId);
        
        // Call hooks before creation
        if (!plugin.getHookManager().callEventPreCreate(event)) {
            return null; // Hook cancelled creation
        }
        
        // Fire Bukkit event
        SwiftEventCreateEvent createEvent = new SwiftEventCreateEvent(event);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) {
            return null;
        }
        
        allEvents.put(event.getId(), event);
        
        // Save to database
        plugin.getDatabaseManager().saveEvent(event);
        
        // Call hooks after creation
        plugin.getHookManager().callEventCreated(event);
        
        // Send chat announcement
        plugin.getChatManager().announceEvent(event, ChatManager.EventAnnouncement.CREATED);
        
        plugin.getLogger().info("Event created: " + event.getName() + " by " + creatorId);
        return event;
    }
    
    public boolean deleteEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null) {
            return false;
        }
        
        // Cancel if active
        if (event.isActive()) {
            cancelEvent(eventId);
        }
        
        allEvents.remove(eventId);
        activeEvents.remove(eventId);
        
        // Delete from database
        plugin.getDatabaseManager().deleteEvent(eventId);
        
        plugin.getLogger().info("Event deleted: " + event.getName());
        return true;
    }
    
    public boolean startEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null || !event.canStart()) {
            return false;
        }
        
        // Call hooks before start
        if (!plugin.getHookManager().callEventPreStart(event)) {
            return false; // Hook cancelled start
        }
        
        // Fire Bukkit event
        SwiftEventStartEvent startEvent = new SwiftEventStartEvent(event);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.ACTIVE);
        event.setStartTime(System.currentTimeMillis());
        activeEvents.put(eventId, event);
        
        // Notify participants
        notifyParticipants(event, "§aEvent '" + event.getName() + "' has started!");
        
        // Save changes
        plugin.getDatabaseManager().saveEvent(event);
        
        // Call hooks after start
        plugin.getHookManager().callEventStarted(event);
        
        // Send chat announcement
        plugin.getChatManager().announceEvent(event, ChatManager.EventAnnouncement.STARTING);
        
        plugin.getLogger().info("Event started: " + event.getName());
        return true;
    }
    
    public boolean endEvent(String eventId) {
        Event event = activeEvents.get(eventId);
        if (event == null) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.COMPLETED);
        event.setEndTime(System.currentTimeMillis());
        activeEvents.remove(eventId);
        
        // Notify participants
        notifyParticipants(event, "§eEvent '" + event.getName() + "' has ended!");
        
        // Distribute rewards if any
        distributeRewards(event);
        
        // Save changes
        plugin.getDatabaseManager().saveEvent(event);
        
        // Fire Bukkit event and call hooks
        SwiftEventEndEvent endEvent = new SwiftEventEndEvent(event, "completed");
        Bukkit.getPluginManager().callEvent(endEvent);
        plugin.getHookManager().callEventEnded(event, "completed");
        
        // Send chat announcement
        plugin.getChatManager().announceEvent(event, ChatManager.EventAnnouncement.ENDED);
        
        plugin.getLogger().info("Event ended: " + event.getName());
        return true;
    }
    
    public boolean cancelEvent(String eventId) {
        Event event = allEvents.get(eventId);
        if (event == null) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.CANCELLED);
        activeEvents.remove(eventId);
        
        // Notify participants
        notifyParticipants(event, "§cEvent '" + event.getName() + "' has been cancelled!");
        
        // Save changes
        plugin.getDatabaseManager().saveEvent(event);
        
        // Fire Bukkit event and call hooks
        SwiftEventEndEvent endEvent = new SwiftEventEndEvent(event, "cancelled");
        Bukkit.getPluginManager().callEvent(endEvent);
        plugin.getHookManager().callEventEnded(event, "cancelled");
        
        // Send chat announcement
        plugin.getChatManager().announceEvent(event, ChatManager.EventAnnouncement.ENDED);
        
        plugin.getLogger().info("Event cancelled: " + event.getName());
        return true;
    }
    
    public boolean pauseEvent(String eventId) {
        Event event = activeEvents.get(eventId);
        if (event == null || !event.isActive()) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.PAUSED);
        
        // Notify participants
        notifyParticipants(event, "§6Event '" + event.getName() + "' has been paused!");
        
        // Save changes
        plugin.getDatabaseManager().saveEvent(event);
        
        return true;
    }
    
    public boolean resumeEvent(String eventId) {
        Event event = activeEvents.get(eventId);
        if (event == null || event.getStatus() != Event.EventStatus.PAUSED) {
            return false;
        }
        
        event.setStatus(Event.EventStatus.ACTIVE);
        
        // Notify participants
        notifyParticipants(event, "§aEvent '" + event.getName() + "' has been resumed!");
        
        // Save changes
        plugin.getDatabaseManager().saveEvent(event);
        
        return true;
    }
    
    // Participant management
    public boolean joinEvent(String eventId, UUID playerId) {
        Event event = allEvents.get(eventId);
        if (event == null || !event.canJoin()) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        
        // Call hooks before join
        if (player != null && !plugin.getHookManager().callPlayerPreJoin(player, event)) {
            return false; // Hook cancelled join
        }
        
        // Fire Bukkit event
        SwiftEventPlayerJoinEvent joinEvent = new SwiftEventPlayerJoinEvent(event, playerId, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            return false;
        }
        
        boolean success = event.addParticipant(playerId);
        if (success) {
            if (player != null) {
                String message = plugin.getConfigManager().getMessage("event_joined")
                        .replace("{event_name}", event.getName());
                player.sendMessage(plugin.getConfigManager().getPrefix() + message);
            }
            
            // Save changes
            plugin.getDatabaseManager().saveEvent(event);
            
            // Call hooks after join
            if (player != null) {
                plugin.getHookManager().callPlayerJoined(player, event);
            }
        }
        
        return success;
    }
    
    public boolean leaveEvent(String eventId, UUID playerId) {
        Event event = allEvents.get(eventId);
        if (event == null) {
            return false;
        }
        
        boolean success = event.removeParticipant(playerId);
        if (success) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                String message = plugin.getConfigManager().getMessage("event_left")
                        .replace("{event_name}", event.getName());
                player.sendMessage(plugin.getConfigManager().getPrefix() + message);
            }
            
            // Save changes
            plugin.getDatabaseManager().saveEvent(event);
            
            // Fire Bukkit event and call hooks
            SwiftEventPlayerLeaveEvent leaveEvent = new SwiftEventPlayerLeaveEvent(event, playerId, player, "manual");
            Bukkit.getPluginManager().callEvent(leaveEvent);
            plugin.getHookManager().callPlayerLeft(player, playerId, event, "manual");
        }
        
        return success;
    }
    
    private void notifyParticipants(Event event, String message) {
        for (UUID participantId : event.getParticipants()) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + message);
            }
        }
    }
    
    private void distributeRewards(Event event) {
        if (event.getRewards().isEmpty()) {
            return;
        }
        
        for (UUID participantId : event.getParticipants()) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                for (String reward : event.getRewards()) {
                    // Execute reward command (could be items, money, etc.)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                            reward.replace("{player}", player.getName()));
                }
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§aYou have received rewards for participating in " + event.getName() + "!");
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
                .filter(event -> event.getParticipants().contains(playerId))
                .collect(Collectors.toList());
    }
    
    public boolean isPlayerInEvent(UUID playerId) {
        return allEvents.values().stream()
                .anyMatch(event -> event.getParticipants().contains(playerId));
    }
    
    // Save methods
    public void saveAllEvents() {
        // Check database health before saving
        if (!plugin.getDatabaseManager().isConnectionHealthy()) {
            plugin.getLogger().warning("Database connection unhealthy, attempting reconnection...");
            plugin.getDatabaseManager().attemptReconnection();
        }
        
        // Optimization: Only save events that have been modified
        List<Event> eventsToSave = allEvents.values().stream()
                .filter(this::hasEventChanged)
                .collect(Collectors.toList());
        
        if (!eventsToSave.isEmpty()) {
            plugin.getLogger().info("Auto-saving " + eventsToSave.size() + " modified events");
            eventsToSave.forEach(event -> plugin.getDatabaseManager().saveEvent(event));
        }
    }
    
    public void saveEvent(Event event) {
        if (event != null) {
            plugin.getDatabaseManager().saveEvent(event);
        }
    }
    
    // Simple change detection - could be enhanced with more sophisticated tracking
    private boolean hasEventChanged(Event event) {
        // For now, save all events, but this method provides the framework
        // for more sophisticated change tracking in the future
        return true;
    }
    
    // Cleanup
    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        if (eventUpdateTask != null) {
            eventUpdateTask.cancel();
        }
        
        // Final save of all events
        saveAllEvents();
        
        plugin.getLogger().info("EventManager shutdown complete");
    }
} 