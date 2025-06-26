package com.swiftevents.api.events;

import com.swiftevents.events.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Called when a SwiftEvents event is created
 * Other plugins can listen to this event and cancel it if needed
 */
public class SwiftEventCreateEvent extends org.bukkit.event.Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private final Event swiftEvent;
    private boolean cancelled = false;
    
    public SwiftEventCreateEvent(Event swiftEvent) {
        this.swiftEvent = swiftEvent;
    }
    
    /**
     * Get the SwiftEvents event being created
     * @return The event
     */
    public Event getSwiftEvent() {
        return swiftEvent;
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