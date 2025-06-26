package com.swiftevents.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Event {
    
    public static class EventLocation {
        private final String worldName;
        private final double x, y, z;

        public EventLocation(String worldName, double x, double y, double z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getWorldName() {
            return worldName;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }
    
    public enum EventType {
        PVP, PVE, BUILDING, RACING, TREASURE_HUNT, MINI_GAME, CUSTOM
    }
    
    public enum EventStatus {
        CREATED, SCHEDULED, ACTIVE, PAUSED, COMPLETED, CANCELLED
    }
    
    private final String id;
    private String name;
    private String description;
    private EventType type;
    private EventStatus status;
    private int maxParticipants;
    private int currentParticipants;
    private long startTime; // Use primitive long instead of Long
    private long endTime; // Use primitive long instead of Long
    private UUID createdBy;
    private final long createdAt; // Make immutable
    
    // Location data - optimize by using primitives and nullability flag
    private String world;
    private double x, y, z;
    private boolean hasLocation = false; // Flag to check if location is set
    
    // Participants - Using smaller initial capacity and load factor optimization
    private final Set<UUID> participants;
    
    // Rewards and requirements - Use ArrayList with smaller initial capacity
    private List<String> rewards;
    private Map<String, Object> requirements;
    
    // Additional metadata - Lazy initialization
    private Map<String, Object> metadata;
    
    // Optimization: Cache frequently computed values with invalidation
    private transient String cachedFormattedTime = null;
    private transient long lastTimeCalculation = 0;
    private static final long TIME_CACHE_DURATION = 1000; // 1 second cache
    
    // Optimization: String builder pool for thread-safe formatting
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = 
        ThreadLocal.withInitial(() -> new StringBuilder(64));
    
    public Event(String id, String name, String description, EventType type) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = EventStatus.CREATED;
        this.maxParticipants = -1; // -1 means unlimited
        this.currentParticipants = 0;
        this.createdAt = System.currentTimeMillis();
        
        // Optimization: Start with smaller collections and proper load factors
        this.participants = ConcurrentHashMap.newKeySet(4); // Start small
        this.rewards = new ArrayList<>(2); // Most events have few rewards
        this.requirements = new HashMap<>(4, 0.75f); // Standard load factor
        // metadata will be lazily initialized
    }
    
    // Constructor for creating new events
    public Event(String name, String description, EventType type) {
        this(UUID.randomUUID().toString(), name, description, type);
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public EventType getType() {
        return type;
    }
    
    public void setType(EventType type) {
        this.type = type;
    }
    
    public EventStatus getStatus() {
        return status;
    }
    
    public void setStatus(EventStatus status) {
        this.status = status;
        // Clear time cache when status changes
        clearTimeCache();
    }
    
    public int getMaxParticipants() {
        return maxParticipants;
    }
    
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public int getCurrentParticipants() {
        return currentParticipants;
    }
    
    public void setCurrentParticipants(int currentParticipants) {
        this.currentParticipants = currentParticipants;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
        clearTimeCache();
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
        clearTimeCache();
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public String getWorld() {
        return world;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getZ() {
        return z;
    }
    
    public boolean hasLocation() {
        return hasLocation;
    }
    
    public void setLocation(String world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasLocation = true;
    }
    
    public void clearLocation() {
        this.world = null;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.hasLocation = false;
    }
    
    // Optimization: Return view instead of copy for participants
    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }
    
    public void setParticipants(Set<UUID> participants) {
        this.participants.clear();
        if (participants != null) {
            this.participants.addAll(participants);
        }
        this.currentParticipants = this.participants.size();
    }
    
    public boolean addParticipant(UUID playerId) {
        if (maxParticipants > 0 && participants.size() >= maxParticipants) {
            return false;
        }
        
        boolean added = participants.add(playerId);
        if (added) {
            currentParticipants = participants.size();
        }
        return added;
    }
    
    public boolean removeParticipant(UUID playerId) {
        boolean removed = participants.remove(playerId);
        if (removed) {
            currentParticipants = participants.size();
        }
        return removed;
    }
    
    public boolean isParticipant(UUID playerId) {
        return participants.contains(playerId);
    }
    
    public boolean isFull() {
        return maxParticipants > 0 && participants.size() >= maxParticipants;
    }
    
    public boolean hasUnlimitedSlots() {
        return maxParticipants <= 0;
    }
    
    public int getAvailableSlots() {
        return maxParticipants <= 0 ? Integer.MAX_VALUE : Math.max(0, maxParticipants - participants.size());
    }
    
    // Rewards management with lazy initialization
    public List<String> getRewards() {
        return rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
    }
    
    public void setRewards(List<String> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            this.rewards = null; // Save memory when empty
        } else {
            this.rewards = new ArrayList<>(rewards);
        }
    }
    
    public void addReward(String reward) {
        if (rewards == null) {
            rewards = new ArrayList<>(2);
        }
        rewards.add(reward);
    }
    
    public void removeReward(String reward) {
        if (rewards != null) {
            rewards.remove(reward);
            if (rewards.isEmpty()) {
                rewards = null; // Save memory when empty
            }
        }
    }
    
    // Requirements management
    public Map<String, Object> getRequirements() {
        return requirements != null ? new HashMap<>(requirements) : new HashMap<>();
    }
    
    public void setRequirements(Map<String, Object> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            this.requirements = null;
        } else {
            this.requirements = new HashMap<>(requirements);
        }
    }
    
    public void addRequirement(String key, Object value) {
        if (requirements == null) {
            requirements = new HashMap<>(4, 0.75f);
        }
        requirements.put(key, value);
    }
    
    public void removeRequirement(String key) {
        if (requirements != null) {
            requirements.remove(key);
            if (requirements.isEmpty()) {
                requirements = null;
            }
        }
    }
    
    public Object getRequirement(String key) {
        return requirements != null ? requirements.get(key) : null;
    }
    
    // Metadata management with lazy initialization
    public Map<String, Object> getMetadata() {
        return metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            this.metadata = null;
        } else {
            this.metadata = new HashMap<>(metadata);
        }
    }
    
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>(4, 0.75f);
        }
        metadata.put(key, value);
    }
    
    public void removeMetadata(String key) {
        if (metadata != null) {
            metadata.remove(key);
            if (metadata.isEmpty()) {
                metadata = null;
            }
        }
    }
    
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    // Status check methods
    public boolean isActive() {
        return status == EventStatus.ACTIVE;
    }
    
    public boolean isCompleted() {
        return status == EventStatus.COMPLETED;
    }
    
    public boolean isCancelled() {
        return status == EventStatus.CANCELLED;
    }
    
    public boolean isScheduled() {
        return status == EventStatus.SCHEDULED;
    }
    
    public boolean canJoin() {
        return (status == EventStatus.CREATED || status == EventStatus.SCHEDULED || status == EventStatus.ACTIVE) 
               && !isFull();
    }
    
    public boolean canStart() {
        return status == EventStatus.CREATED || status == EventStatus.SCHEDULED;
    }
    
    public boolean hasStarted() {
        return startTime > 0 && System.currentTimeMillis() >= startTime;
    }
    
    public boolean hasEnded() {
        return endTime > 0 && System.currentTimeMillis() >= endTime;
    }
    
    public long getDuration() {
        if (startTime <= 0 || endTime <= 0) {
            return 0;
        }
        return Math.max(0, endTime - startTime);
    }
    
    public long getRemainingTime() {
        if (endTime <= 0) {
            return Long.MAX_VALUE; // Unlimited time
        }
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    // Optimized time formatting with caching and string builder
    public String getFormattedRemainingTime() {
        long currentTime = System.currentTimeMillis();
        
        // Check if cached value is still valid
        if (cachedFormattedTime != null && (currentTime - lastTimeCalculation) < TIME_CACHE_DURATION) {
            return cachedFormattedTime;
        }
        
        long remaining = getRemainingTime();
        cachedFormattedTime = formatTimeString(remaining);
        lastTimeCalculation = currentTime;
        
        return cachedFormattedTime;
    }
    
    // Optimized time formatting using ThreadLocal StringBuilder
    private String formatTimeString(long milliseconds) {
        if (milliseconds == Long.MAX_VALUE) {
            return "Unlimited";
        }
        
        if (milliseconds <= 0) {
            return "Ended";
        }
        
        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0); // Clear the builder
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            sb.append(days).append("d ");
            hours %= 24;
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
            minutes %= 60;
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (days == 0 && hours == 0) { // Only show seconds for shorter durations
            seconds %= 60;
            if (minutes == 0 || seconds > 0) {
                sb.append(seconds).append("s");
            }
        }
        
        String result = sb.toString().trim();
        return result.isEmpty() ? "0s" : result;
    }
    
    private void clearTimeCache() {
        cachedFormattedTime = null;
        lastTimeCalculation = 0;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Event event = (Event) obj;
        return Objects.equals(id, event.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0);
        sb.append("Event{id='").append(id)
          .append("', name='").append(name)
          .append("', type=").append(type)
          .append("', status=").append(status)
          .append("', participants=").append(participants.size())
          .append('}');
        return sb.toString();
    }

    public EventLocation getLocation() {
        if (!hasLocation) {
            return null;
        }
        return new EventLocation(world, x, y, z);
    }
} 