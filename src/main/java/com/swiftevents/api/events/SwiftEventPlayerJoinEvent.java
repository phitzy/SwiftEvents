package com.swiftevents.api.events;

import com.swiftevents.events.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Called when a player joins a SwiftEvents event
 * Other plugins can listen to this event and cancel the join if needed
 */
public class SwiftEventPlayerJoinEvent extends org.bukkit.event.Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private final Event swiftEvent;
    private final UUID playerId;
    private final Player player;
    private boolean cancelled = false;
    
    public SwiftEventPlayerJoinEvent(Event swiftEvent, UUID playerId, Player player) {
        this.swiftEvent = swiftEvent;
        this.playerId = playerId;
        this.player = player;
    }
    
    /**
     * Get the SwiftEvents event being joined
     * @return The event
     */
    public Event getSwiftEvent() {
        return swiftEvent;
    }
    
    /**
     * Get the player's UUID
     * @return The player's UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Get the player joining the event
     * @return The player (may be null if offline)
     */
    public Player getPlayer() {
        return player;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
} 