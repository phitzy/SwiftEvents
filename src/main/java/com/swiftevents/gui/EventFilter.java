package com.swiftevents.gui;

import com.swiftevents.events.Event;
import org.bukkit.entity.Player;

public enum EventFilter {
    ALL("All Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return hasPermissionForEvent(event, player);
        }
    },
    ACTIVE("Active Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.isActive() && hasPermissionForEvent(event, player);
        }
    },
    JOINABLE("Joinable Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.canJoin() && hasPermissionForEvent(event, player);
        }
    },
    PARTICIPATING("My Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.isParticipant(player.getUniqueId()) && hasPermissionForEvent(event, player);
        }
    },
    PVP("PvP Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.getType() == Event.EventType.PVP && hasPermissionForEvent(event, player);
        }
    },
    PVE("PvE Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.getType() == Event.EventType.PVE && hasPermissionForEvent(event, player);
        }
    },
    BUILDING("Building Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.getType() == Event.EventType.BUILDING && hasPermissionForEvent(event, player);
        }
    },
    RACING("Racing Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.getType() == Event.EventType.RACING && hasPermissionForEvent(event, player);
        }
    },
    TREASURE_HUNT("Treasure Hunt Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.getType() == Event.EventType.TREASURE_HUNT && hasPermissionForEvent(event, player);
        }
    },
    MINI_GAME("Mini-Game Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.getType() == Event.EventType.MINI_GAME && hasPermissionForEvent(event, player);
        }
    },
    CUSTOM("Custom Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.getType() == Event.EventType.CUSTOM && hasPermissionForEvent(event, player);
        }
    },
    SCHEDULED("Scheduled Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.isScheduled() && hasPermissionForEvent(event, player);
        }
    },
    COMPLETED("Completed Events") {
        @Override
        public boolean matches(Event event, Player player) {
            return event.isCompleted() && hasPermissionForEvent(event, player);
        }
    };
    
    private final String displayName;
    
    EventFilter(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public abstract boolean matches(Event event, Player player);
    
    protected boolean hasPermissionForEvent(Event event, Player player) {
        // Check if player has permission for this event type
        String eventTypePermission = "swiftevents.event." + event.getType().name().toLowerCase();
        return player.hasPermission("swiftevents.user") && 
               player.hasPermission(eventTypePermission);
    }
    
    public static EventFilter getNext(EventFilter current) {
        EventFilter[] values = values();
        int currentIndex = current.ordinal();
        return values[(currentIndex + 1) % values.length];
    }
} 