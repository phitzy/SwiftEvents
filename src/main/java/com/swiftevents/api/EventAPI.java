package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.api.hooks.SwiftEventsHook;
import com.swiftevents.events.Event;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for SwiftEvents plugin
 * This class provides methods for other plugins to integrate with SwiftEvents
 */
public class EventAPI {
    
    private final SwiftEventsPlugin plugin;
    
    public EventAPI(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get an event by its ID
     * @param eventId The event ID
     * @return The event or null if not found
     */
    public Event getEvent(String eventId) {
        return plugin.getEventManager().getEvent(eventId);
    }
    
    /**
     * Get all events
     * @return List of all events
     */
    public List<Event> getAllEvents() {
        return plugin.getEventManager().getAllEvents();
    }
    
    /**
     * Get all active events
     * @return List of active events
     */
    public List<Event> getActiveEvents() {
        return plugin.getEventManager().getActiveEvents();
    }
    
    /**
     * Get events by type
     * @param type The event type
     * @return List of events of the specified type
     */
    public List<Event> getEventsByType(Event.EventType type) {
        return plugin.getEventManager().getEventsByType(type);
    }
    
    /**
     * Get events by status
     * @param status The event status
     * @return List of events with the specified status
     */
    public List<Event> getEventsByStatus(Event.EventStatus status) {
        return plugin.getEventManager().getEventsByStatus(status);
    }
    
    /**
     * Get events that a player is participating in
     * @param playerId The player's UUID
     * @return List of events the player is participating in
     */
    public List<Event> getPlayerEvents(UUID playerId) {
        return plugin.getEventManager().getPlayerEvents(playerId);
    }
    
    /**
     * Check if a player is currently in any active event
     * @param playerId The player's UUID
     * @return True if player is in an active event
     */
    public boolean isPlayerInEvent(UUID playerId) {
        return plugin.getEventManager().isPlayerInEvent(playerId);
    }
    
    /**
     * Create a new event
     * @param name The event name
     * @param description The event description
     * @param type The event type
     * @param creatorId The UUID of the creator (can be null)
     * @return The created event or null if creation failed
     */
    public Event createEvent(String name, String description, Event.EventType type, UUID creatorId) {
        return plugin.getEventManager().createEvent(name, description, type, creatorId);
    }
    
    /**
     * Delete an event
     * @param eventId The event ID
     * @return True if deletion was successful
     */
    public boolean deleteEvent(String eventId) {
        return plugin.getEventManager().deleteEvent(eventId);
    }
    
    /**
     * Start an event
     * @param eventId The event ID
     * @return True if the event was started successfully
     */
    public boolean startEvent(String eventId) {
        return plugin.getEventManager().startEvent(eventId);
    }
    
    /**
     * End an event
     * @param eventId The event ID
     * @return True if the event was ended successfully
     */
    public boolean endEvent(String eventId) {
        return plugin.getEventManager().endEvent(eventId);
    }
    
    /**
     * Cancel an event
     * @param eventId The event ID
     * @return True if the event was cancelled successfully
     */
    public boolean cancelEvent(String eventId) {
        return plugin.getEventManager().cancelEvent(eventId);
    }
    
    /**
     * Pause an event
     * @param eventId The event ID
     * @return True if the event was paused successfully
     */
    public boolean pauseEvent(String eventId) {
        return plugin.getEventManager().pauseEvent(eventId);
    }
    
    /**
     * Resume a paused event
     * @param eventId The event ID
     * @return True if the event was resumed successfully
     */
    public boolean resumeEvent(String eventId) {
        return plugin.getEventManager().resumeEvent(eventId);
    }
    
    /**
     * Add a player to an event
     * @param eventId The event ID
     * @param playerId The player's UUID
     * @return True if the player was added successfully
     */
    public boolean joinEvent(String eventId, UUID playerId) {
        return plugin.getEventManager().joinEvent(eventId, playerId);
    }
    
    /**
     * Remove a player from an event
     * @param eventId The event ID
     * @param playerId The player's UUID
     * @return True if the player was removed successfully
     */
    public boolean leaveEvent(String eventId, UUID playerId) {
        return plugin.getEventManager().leaveEvent(eventId, playerId);
    }
    
    /**
     * Save an event to storage
     * @param event The event to save
     */
    public void saveEvent(Event event) {
        plugin.getEventManager().saveEvent(event);
    }
    
    /**
     * Check if the database is enabled
     * @return True if database storage is enabled
     */
    public boolean isDatabaseEnabled() {
        return plugin.getConfigManager().isDatabaseEnabled();
    }
    
    /**
     * Check if GUI is enabled
     * @return True if GUI is enabled
     */
    public boolean isGUIEnabled() {
        return plugin.getConfigManager().isGUIEnabled();
    }
    
    /**
     * Check if HUD is enabled
     * @return True if HUD is enabled
     */
    public boolean isHUDEnabled() {
        return plugin.getConfigManager().isHUDEnabled();
    }
    
    /**
     * Get the maximum number of concurrent events allowed
     * @return The maximum concurrent events
     */
    public int getMaxConcurrentEvents() {
        return plugin.getConfigManager().getMaxConcurrentEvents();
    }
    
    /**
     * Get the plugin instance
     * @return The SwiftEvents plugin instance
     */
    public SwiftEventsPlugin getPlugin() {
        return plugin;
    }
    
    // ===== INTEGRATION METHODS =====
    
    /**
     * Register an integration hook
     * @param hook The hook to register
     * @return True if registered successfully
     */
    public boolean registerHook(SwiftEventsHook hook) {
        return plugin.getHookManager().registerHook(hook);
    }
    
    /**
     * Unregister an integration hook
     * @param hookName The name of the hook to unregister
     * @return True if unregistered successfully
     */
    public boolean unregisterHook(String hookName) {
        return plugin.getHookManager().unregisterHook(hookName);
    }
    
    /**
     * Get a specific integration hook
     * @param hookName The hook name
     * @return The hook or null if not found
     */
    public SwiftEventsHook getHook(String hookName) {
        return plugin.getHookManager().getHook(hookName);
    }
    
    /**
     * Get all registered integration hooks
     * @return Collection of all hooks
     */
    public Collection<SwiftEventsHook> getAllHooks() {
        return plugin.getHookManager().getAllHooks();
    }
    
    /**
     * Get names of all registered hooks
     * @return Set of hook names
     */
    public Set<String> getHookNames() {
        return plugin.getHookManager().getHookNames();
    }
    
    // ===== ENHANCED EVENT METHODS =====
    
    /**
     * Create and immediately start an event
     * @param name The event name
     * @param description The event description
     * @param type The event type
     * @param creatorId The UUID of the creator (can be null)
     * @param duration Duration in seconds (0 for no time limit)
     * @return The created and started event or null if creation/start failed
     */
    public Event createAndStartEvent(String name, String description, Event.EventType type, UUID creatorId, long duration) {
        Event event = createEvent(name, description, type, creatorId);
        if (event != null) {
            if (duration > 0) {
                event.setEndTime(System.currentTimeMillis() + (duration * 1000));
            }
            if (startEvent(event.getId())) {
                return event;
            } else {
                // If start failed, delete the created event
                deleteEvent(event.getId());
                return null;
            }
        }
        return null;
    }
    
    /**
     * Schedule an event to start at a specific time
     * @param eventId The event ID
     * @param startTime The time to start (milliseconds since epoch)
     * @return True if scheduled successfully
     */
    public boolean scheduleEvent(String eventId, long startTime) {
        Event event = getEvent(eventId);
        if (event != null && event.getStatus() == Event.EventStatus.CREATED) {
            event.setStatus(Event.EventStatus.SCHEDULED);
            event.setStartTime(startTime);
            plugin.getEventManager().saveEvent(event);
            return true;
        }
        return false;
    }
    
    /**
     * Add multiple players to an event at once
     * @param eventId The event ID
     * @param playerIds Collection of player UUIDs
     * @return Number of players successfully added
     */
    public int addPlayersToEvent(String eventId, Collection<UUID> playerIds) {
        int count = 0;
        for (UUID playerId : playerIds) {
            if (joinEvent(eventId, playerId)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Remove multiple players from an event at once
     * @param eventId The event ID
     * @param playerIds Collection of player UUIDs
     * @return Number of players successfully removed
     */
    public int removePlayersFromEvent(String eventId, Collection<UUID> playerIds) {
        int count = 0;
        for (UUID playerId : playerIds) {
            if (leaveEvent(eventId, playerId)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Get events that are accepting new participants
     * @return List of joinable events
     */
    public List<Event> getJoinableEvents() {
        return plugin.getEventManager().getAllEvents().stream()
                .filter(Event::canJoin)
                .toList();
    }
    
    /**
     * Get running events (active status)
     * @return List of currently running events
     */
    public List<Event> getRunningEvents() {
        return plugin.getEventManager().getEventsByStatus(Event.EventStatus.ACTIVE);
    }
    
    /**
     * Get events created by a specific player
     * @param creatorId The creator's UUID
     * @return List of events created by the player
     */
    public List<Event> getEventsByCreator(UUID creatorId) {
        return plugin.getEventManager().getAllEvents().stream()
                .filter(event -> creatorId.equals(event.getCreatedBy()))
                .toList();
    }
    
    /**
     * Send a custom message to all participants of an event
     * @param eventId The event ID
     * @param message The message to send
     * @return Number of players the message was sent to
     */
    public int broadcastToEventParticipants(String eventId, String message) {
        Event event = getEvent(eventId);
        if (event == null) return 0;
        
        int count = 0;
        for (UUID participantId : event.getParticipants()) {
            Player player = plugin.getServer().getPlayer(participantId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
                count++;
            }
        }
        return count;
    }
    
    /**
     * Check if two events conflict (same type, overlapping time)
     * @param event1 First event
     * @param event2 Second event
     * @return True if events conflict
     */
    public boolean eventsConflict(Event event1, Event event2) {
        if (event1.getType() != event2.getType()) {
            return false;
        }
        
        Long start1 = event1.getStartTime();
        Long end1 = event1.getEndTime();
        Long start2 = event2.getStartTime();
        Long end2 = event2.getEndTime();
        
        if (start1 == null || start2 == null) {
            return false;
        }
        
        // If either event has no end time, assume they conflict if they start at the same time
        if (end1 == null || end2 == null) {
            return start1.equals(start2);
        }
        
        // Check for time overlap
        return start1 < end2 && start2 < end1;
    }
} 