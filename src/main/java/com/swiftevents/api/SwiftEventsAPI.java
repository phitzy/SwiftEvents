package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.api.hooks.SwiftEventsHook;
import com.swiftevents.events.Event;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Static utility class for easy access to SwiftEvents API
 * This provides a simpler way for other plugins to integrate with SwiftEvents
 * without needing to manage plugin instances
 */
public final class SwiftEventsAPI {
    
    private static SwiftEventsPlugin plugin;
    private static EventAPI api;
    
    private SwiftEventsAPI() {
        // Utility class, no instantiation
    }
    
    /**
     * Initialize the API (called internally by SwiftEvents)
     * @param pluginInstance The SwiftEvents plugin instance
     */
    public static void initialize(SwiftEventsPlugin pluginInstance) {
        plugin = pluginInstance;
        api = pluginInstance.getEventAPI();
    }
    
    /**
     * Check if SwiftEvents is available and loaded
     * @return True if SwiftEvents is available
     */
    public static boolean isAvailable() {
        return plugin != null && plugin.isEnabled() && api != null;
    }
    
    /**
     * Get the SwiftEvents plugin instance
     * @return The plugin instance or null if not available
     */
    public static SwiftEventsPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * Get the EventAPI instance
     * @return The API instance or null if not available
     */
    public static EventAPI getAPI() {
        return api;
    }
    
    /**
     * Get the GuiAPI instance
     * @return The GuiAPI instance or null if not available
     */
    public static GuiAPI getGuiAPI() {
        return isAvailable() ? api.getGuiAPI() : null;
    }
    
    /**
     * Get the HudAPI instance
     * @return The HudAPI instance or null if not available
     */
    public static HudAPI getHudAPI() {
        return isAvailable() ? api.getHudAPI() : null;
    }
    
    /**
     * Get the ChatAPI instance
     * @return The ChatAPI instance or null if not available
     */
    public static ChatAPI getChatAPI() {
        return isAvailable() ? api.getChatAPI() : null;
    }
    
    /**
     * Get the TaskerAPI instance
     * @return The TaskerAPI instance or null if not available
     */
    public static TaskerAPI getTaskerAPI() {
        return isAvailable() ? api.getTaskerAPI() : null;
    }
    
    /**
     * Get the LocationAPI instance
     * @return The LocationAPI instance or null if not available
     */
    public static LocationAPI getLocationAPI() {
        return isAvailable() ? api.getLocationAPI() : null;
    }
    
    // ===== CONVENIENCE METHODS =====
    
    /**
     * Check if a player is currently in any active event
     * @param player The player to check
     * @return True if player is in an active event
     */
    public static boolean isPlayerInEvent(Player player) {
        return isAvailable() && api.isPlayerInEvent(player.getUniqueId());
    }
    
    /**
     * Get all events a player is participating in
     * @param player The player
     * @return List of events the player is in
     */
    public static List<Event> getPlayerEvents(Player player) {
        return isAvailable() ? api.getPlayerEvents(player.getUniqueId()) : List.of();
    }
    
    /**
     * Get all currently active events
     * @return List of active events
     */
    public static List<Event> getActiveEvents() {
        return isAvailable() ? api.getActiveEvents() : List.of();
    }
    
    /**
     * Get all events that are accepting new participants
     * @return List of joinable events
     */
    public static List<Event> getJoinableEvents() {
        return isAvailable() ? api.getJoinableEvents() : List.of();
    }
    
    /**
     * Create a new event
     * @param name Event name
     * @param description Event description
     * @param type Event type
     * @param creator The creating player (can be null)
     * @return The created event or null if failed
     */
    public static Event createEvent(String name, String description, Event.EventType type, Player creator) {
        if (!isAvailable()) return null;
        UUID creatorId = creator != null ? creator.getUniqueId() : null;
        return api.createEvent(name, description, type, creatorId);
    }
    
    /**
     * Make a player join an event
     * @param player The player
     * @param event The event to join
     * @return True if successful
     */
    public static boolean joinEvent(Player player, Event event) {
        return isAvailable() && api.joinEvent(event.getId(), player.getUniqueId());
    }
    
    /**
     * Make a player leave an event
     * @param player The player
     * @param event The event to leave
     * @return True if successful
     */
    public static boolean leaveEvent(Player player, Event event) {
        return isAvailable() && api.leaveEvent(event.getId(), player.getUniqueId());
    }
    
    /**
     * Start an event
     * @param event The event to start
     * @return True if successful
     */
    public static boolean startEvent(Event event) {
        return isAvailable() && api.startEvent(event.getId());
    }
    
    /**
     * End an event
     * @param event The event to end
     * @return True if successful
     */
    public static boolean endEvent(Event event) {
        return isAvailable() && api.endEvent(event.getId());
    }
    
    /**
     * Cancel an event
     * @param event The event to cancel
     * @return True if successful
     */
    public static boolean cancelEvent(Event event) {
        return isAvailable() && api.cancelEvent(event.getId());
    }
    
    /**
     * Register an integration hook
     * @param hook The hook to register
     * @return True if registered successfully
     */
    public static boolean registerHook(SwiftEventsHook hook) {
        return isAvailable() && api.registerHook(hook);
    }
    
    /**
     * Unregister an integration hook
     * @param hookName The name of the hook to unregister
     * @return True if unregistered successfully
     */
    public static boolean unregisterHook(String hookName) {
        return isAvailable() && api.unregisterHook(hookName);
    }
    
    /**
     * Send a message to all participants of an event
     * @param event The event
     * @param message The message to send
     * @return Number of players the message was sent to
     */
    public static int broadcastToEvent(Event event, String message) {
        return isAvailable() ? api.broadcastToEventParticipants(event.getId(), message) : 0;
    }
    
    /**
     * Create and immediately start an event
     * @param name Event name
     * @param description Event description
     * @param type Event type
     * @param creator The creating player (can be null)
     * @param durationSeconds Duration in seconds (0 for unlimited)
     * @return The created and started event or null if failed
     */
    public static Event createAndStartEvent(String name, String description, Event.EventType type, 
                                          Player creator, long durationSeconds) {
        if (!isAvailable()) return null;
        UUID creatorId = creator != null ? creator.getUniqueId() : null;
        return api.createAndStartEvent(name, description, type, creatorId, durationSeconds);
    }
    
    /**
     * Get events by type
     * @param type The event type
     * @return List of events of that type
     */
    public static List<Event> getEventsByType(Event.EventType type) {
        return isAvailable() ? api.getEventsByType(type) : List.of();
    }
    
    /**
     * Check if two events would conflict with each other
     * @param event1 First event
     * @param event2 Second event
     * @return True if they conflict
     */
    public static boolean eventsConflict(Event event1, Event event2) {
        return isAvailable() && api.eventsConflict(event1, event2);
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Attempt to find SwiftEvents plugin if not already initialized
     * @return True if SwiftEvents was found and initialized
     */
    public static boolean tryInitialize() {
        if (isAvailable()) {
            return true;
        }
        
        Plugin swiftEventsPlugin = Bukkit.getPluginManager().getPlugin("SwiftEvents");
        if (swiftEventsPlugin instanceof SwiftEventsPlugin swiftEvents && swiftEvents.isEnabled()) {
            initialize(swiftEvents);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get version information about SwiftEvents
     * @return Version string or "Unknown" if not available
     */
    public static String getVersion() {
        return isAvailable() ? plugin.getDescription().getVersion() : "Unknown";
    }
    
    /**
     * Check if a specific feature is enabled
     * @param feature The feature name ("gui", "hud", "database", "tasker")
     * @return True if the feature is enabled
     */
    public static boolean isFeatureEnabled(String feature) {
        if (!isAvailable()) return false;
        
        return switch (feature.toLowerCase()) {
            case "gui" -> api.isGUIEnabled();
            case "hud" -> api.isHUDEnabled();
            case "database" -> api.isDatabaseEnabled();
            case "tasker" -> plugin.getConfigManager().isEventTaskerEnabled();
            default -> false;
        };
    }
} 