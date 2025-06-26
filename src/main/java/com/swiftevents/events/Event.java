package com.swiftevents.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Event {
    
    public enum EventType {
        PVP, PVE, BUILDING, RACING, TREASURE_HUNT, MINI_GAME, CUSTOM
    }
    
    public enum EventStatus {
        CREATED, SCHEDULED, ACTIVE, PAUSED, COMPLETED, CANCELLED
    }
    
    private String id;
    private String name;
    private String description;
    private EventType type;
    private EventStatus status;
    private int maxParticipants;
    private int currentParticipants;
    private Long startTime;
    private Long endTime;
    private UUID createdBy;
    private long createdAt;
    
    // Location data
    private String world;
    private double x, y, z;
    
    // Participants - Using ConcurrentHashMap.newKeySet() for thread safety
    private Set<UUID> participants;
    
    // Rewards and requirements
    private List<String> rewards;
    private Map<String, Object> requirements;
    
    // Additional metadata
    private Map<String, Object> metadata;
    
    // Optimization: Cache frequently computed values
    private transient String formattedRemainingTime = null;
    private transient long lastTimeCalculation = 0;
    private static final long TIME_CACHE_DURATION = 1000; // 1 second cache
    
    public Event(String id, String name, String description, EventType type) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = EventStatus.CREATED;
        this.maxParticipants = -1; // -1 means unlimited
        this.currentParticipants = 0;
        this.createdAt = System.currentTimeMillis();
        
        // Optimization: Use concurrent collections for thread safety
        this.participants = ConcurrentHashMap.newKeySet();
        this.rewards = new ArrayList<>();
        this.requirements = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
    }
    
    // Constructor for creating new events
    public Event(String name, String description, EventType type) {
        this(UUID.randomUUID().toString(), name, description, type);
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public Long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
        clearTimeCache();
    }
    
    public Long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Long endTime) {
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
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
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
    
    public void setLocation(String world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Set<UUID> getParticipants() {
        return new HashSet<>(participants);
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
    
    // Optimization: Direct participant check without creating new collection
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
        return hasUnlimitedSlots() ? Integer.MAX_VALUE : Math.max(0, maxParticipants - participants.size());
    }

    public List<String> getRewards() {
        return new ArrayList<>(rewards);
    }

    public void setRewards(List<String> rewards) {
        this.rewards = rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
    }

    public void addReward(String reward) {
        this.rewards.add(reward);
    }

    public void removeReward(String reward) {
        this.rewards.remove(reward);
    }

    public Map<String, Object> getRequirements() {
        return new HashMap<>(requirements);
    }

    public void setRequirements(Map<String, Object> requirements) {
        this.requirements.clear();
        if (requirements != null) {
            this.requirements.putAll(requirements);
        }
    }

    public void addRequirement(String key, Object value) {
        this.requirements.put(key, value);
    }

    public void removeRequirement(String key) {
        this.requirements.remove(key);
    }

    public Object getRequirement(String key) {
        return this.requirements.get(key);
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata.clear();
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void removeMetadata(String key) {
        this.metadata.remove(key);
    }

    public Object getMetadata(String key) {
        return this.metadata.get(key);
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
        return (status == EventStatus.CREATED || status == EventStatus.SCHEDULED) && !isFull();
    }

    public boolean canStart() {
        return status == EventStatus.CREATED || status == EventStatus.SCHEDULED;
    }

    public boolean hasStarted() {
        return startTime != null && System.currentTimeMillis() >= startTime;
    }

    public boolean hasEnded() {
        return endTime != null && System.currentTimeMillis() >= endTime;
    }

    public long getDuration() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return endTime - startTime;
    }

    public long getRemainingTime() {
        if (endTime == null) {
            return 0;
        }
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    // Optimized formatted remaining time with caching
    public String getFormattedRemainingTime() {
        long currentTime = System.currentTimeMillis();
        
        // Use cached value if it's recent enough
        if (formattedRemainingTime != null && (currentTime - lastTimeCalculation) < TIME_CACHE_DURATION) {
            return formattedRemainingTime;
        }
        
        long remaining = getRemainingTime();
        formattedRemainingTime = formatTimeString(remaining);
        lastTimeCalculation = currentTime;
        
        return formattedRemainingTime;
    }
    
    // Optimized time formatting
    private String formatTimeString(long milliseconds) {
        if (milliseconds <= 0) {
            return "0s";
        }
        
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder(16);
        
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(secs).append("s");
        
        return sb.toString();
    }
    
    private void clearTimeCache() {
        formattedRemainingTime = null;
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
        return String.format("Event{id='%s', name='%s', type=%s, status=%s, participants=%d/%s}", 
                id, name, type, status, currentParticipants, 
                maxParticipants > 0 ? String.valueOf(maxParticipants) : "∞");
    }
} 