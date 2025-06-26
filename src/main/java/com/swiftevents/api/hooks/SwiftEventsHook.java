package com.swiftevents.api.hooks;

import com.swiftevents.events.Event;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Interface for creating integration hooks with SwiftEvents
 * Other plugins can implement this interface to receive callbacks
 * when important events happen in SwiftEvents
 */
public interface SwiftEventsHook {
    
    /**
     * Get the name of this hook (should be unique)
     * @return The hook name
     */
    String getHookName();
    
    /**
     * Get the priority of this hook (lower number = higher priority)
     * @return The priority value
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Called when an event is about to be created
     * @param event The event being created
     * @return True to allow creation, false to prevent it
     */
    default boolean onEventPreCreate(Event event) {
        return true;
    }
    
    /**
     * Called after an event has been created
     * @param event The created event
     */
    default void onEventCreated(Event event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when an event is about to start
     * @param event The event being started
     * @return True to allow start, false to prevent it
     */
    default boolean onEventPreStart(Event event) {
        return true;
    }
    
    /**
     * Called after an event has started
     * @param event The started event
     */
    default void onEventStarted(Event event) {
        // Default implementation does nothing
    }
    
    /**
     * Called after an event has ended
     * @param event The ended event
     * @param reason The reason it ended
     */
    default void onEventEnded(Event event, String reason) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a player is about to join an event
     * @param player The player joining
     * @param event The event being joined
     * @return True to allow join, false to prevent it
     */
    default boolean onPlayerPreJoin(Player player, Event event) {
        return true;
    }
    
    /**
     * Called after a player has joined an event
     * @param player The player who joined
     * @param event The event that was joined
     */
    default void onPlayerJoined(Player player, Event event) {
        // Default implementation does nothing
    }
    
    /**
     * Called after a player has left an event
     * @param player The player who left (may be null if offline)
     * @param playerId The UUID of the player who left
     * @param event The event that was left
     * @param reason The reason they left
     */
    default void onPlayerLeft(Player player, UUID playerId, Event event, String reason) {
        // Default implementation does nothing
    }
    
    /**
     * Called periodically while an event is running (every minute)
     * @param event The running event
     */
    default void onEventUpdate(Event event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when the plugin is being disabled
     * Use this to clean up any resources
     */
    default void onPluginDisable() {
        // Default implementation does nothing
    }
} 