package com.swiftevents.api.events;

import com.swiftevents.events.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a SwiftEvents event ends
 * This event is not cancellable as the event has already concluded
 */
public class SwiftEventEndEvent extends org.bukkit.event.Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Event swiftEvent;
    private final String endReason;
    
    public SwiftEventEndEvent(Event swiftEvent, String endReason) {
        this.swiftEvent = swiftEvent;
        this.endReason = endReason;
    }
    
    /**
     * Get the SwiftEvents event that ended
     * @return The event
     */
    public Event getSwiftEvent() {
        return swiftEvent;
    }
    
    /**
     * Get the reason why the event ended
     * @return The end reason (e.g., "completed", "cancelled", "timeout")
     */
    public String getEndReason() {
        return endReason;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
} 