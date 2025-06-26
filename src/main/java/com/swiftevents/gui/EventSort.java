package com.swiftevents.gui;

import com.swiftevents.events.Event;
import java.util.Comparator;

public enum EventSort {
    NAME("Name (A-Z)") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing(Event::getName, String.CASE_INSENSITIVE_ORDER);
        }
    },
    NAME_DESC("Name (Z-A)") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing(Event::getName, String.CASE_INSENSITIVE_ORDER).reversed();
        }
    },
    STATUS("Status") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing(Event::getStatus);
        }
    },
    TYPE("Type") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing(Event::getType);
        }
    },
    PARTICIPANTS("Participants (Most)") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing(Event::getCurrentParticipants).reversed();
        }
    },
    PARTICIPANTS_LEAST("Participants (Least)") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing(Event::getCurrentParticipants);
        }
    },
    CREATED_TIME("Created (Newest)") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing(Event::getCreatedAt).reversed();
        }
    },
    CREATED_TIME_OLD("Created (Oldest)") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing(Event::getCreatedAt);
        }
    },
    START_TIME("Start Time") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing((Event event) -> event.getStartTime() > 0 ? event.getStartTime() : Long.MAX_VALUE);
        }
    },
    REMAINING_TIME("Time Remaining") {
        @Override
        public Comparator<Event> getComparator() {
            return Comparator.comparing((Event event) -> {
                if (!event.isActive() || event.getEndTime() <= 0) {
                    return Long.MAX_VALUE;
                }
                return event.getRemainingTime();
            });
        }
    };
    
    private final String displayName;
    
    EventSort(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public abstract Comparator<Event> getComparator();
    
    public static EventSort getNext(EventSort current) {
        EventSort[] values = values();
        int currentIndex = current.ordinal();
        return values[(currentIndex + 1) % values.length];
    }
} 