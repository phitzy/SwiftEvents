package com.swiftevents.api.events;

import com.swiftevents.events.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Called when a player leaves a SwiftEvents event
 * This event is not cancellable as the leave action has already occurred
 */
public class SwiftEventPlayerLeaveEvent extends org.bukkit.event.Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Event swiftEvent;
    private final UUID playerId;
    private final Player player;
    private final String leaveReason;
    
    public SwiftEventPlayerLeaveEvent(Event swiftEvent, UUID playerId, Player player, String leaveReason) {
        this.swiftEvent = swiftEvent;
        this.playerId = playerId;
        this.player = player;
        this.leaveReason = leaveReason;
    }
    
    /**
     * Get the SwiftEvents event being left
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
     * Get the player leaving the event
     * @return The player (may be null if offline)
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Get the reason why the player left
     * @return The leave reason (e.g., "manual", "disconnect", "forced")
     */
    public String getLeaveReason() {
        return leaveReason;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
} 