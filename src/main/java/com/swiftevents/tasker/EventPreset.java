package com.swiftevents.tasker;

import com.swiftevents.events.Event;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a configurable event preset/template that can be used by the EventTasker
 * to automatically create and schedule events based on predefined settings.
 */
public class EventPreset {
    
    private final String id;
    private String name;
    private String description;
    private Event.EventType type;
    private int duration; // in seconds
    private int maxParticipants;
    private int minParticipants;
    private List<String> rewards;
    private boolean enabled;
    private int weight; // Higher weight = more likely to be selected
    
    /**
     * Creates an EventPreset from a configuration section
     */
    public EventPreset(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name", "Unnamed Event");
        this.description = config.getString("description", "No description provided");
        
        // Parse event type
        String typeString = config.getString("type", "CUSTOM");
        try {
            this.type = Event.EventType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.type = Event.EventType.CUSTOM;
        }
        
        this.duration = config.getInt("duration", 1800); // Default 30 minutes
        this.maxParticipants = config.getInt("max_participants", -1); // -1 = unlimited
        this.minParticipants = config.getInt("min_participants", 1);
        this.rewards = config.getStringList("rewards");
        if (this.rewards == null) {
            this.rewards = new ArrayList<>();
        }
        this.enabled = config.getBoolean("enabled", true);
        this.weight = config.getInt("weight", 1);
        
        // Validate settings
        if (this.weight < 1) {
            this.weight = 1;
        }
        if (this.minParticipants < 1) {
            this.minParticipants = 1;
        }
        if (this.maxParticipants > 0 && this.maxParticipants < this.minParticipants) {
            this.maxParticipants = this.minParticipants;
        }
    }
    
    /**
     * Creates an EventPreset with manual parameters
     */
    public EventPreset(String id, String name, String description, Event.EventType type, 
                      int duration, int maxParticipants, int minParticipants, 
                      List<String> rewards, boolean enabled, int weight) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.duration = duration;
        this.maxParticipants = maxParticipants;
        this.minParticipants = minParticipants;
        this.rewards = rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
        this.enabled = enabled;
        this.weight = Math.max(1, weight);
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Event.EventType getType() {
        return type;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public int getMaxParticipants() {
        return maxParticipants;
    }
    
    public int getMinParticipants() {
        return minParticipants;
    }
    
    public List<String> getRewards() {
        return new ArrayList<>(rewards);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getWeight() {
        return weight;
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setType(Event.EventType type) {
        this.type = type;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public void setMinParticipants(int minParticipants) {
        this.minParticipants = minParticipants;
    }
    
    public void setRewards(List<String> rewards) {
        this.rewards = rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setWeight(int weight) {
        this.weight = Math.max(1, weight);
    }
    
    // Utility methods
    public boolean hasRewards() {
        return rewards != null && !rewards.isEmpty();
    }
    
    public boolean hasUnlimitedParticipants() {
        return maxParticipants <= 0;
    }
    
    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
    
    public boolean isValid() {
        return id != null && !id.trim().isEmpty() && 
               name != null && !name.trim().isEmpty() &&
               type != null && duration > 0 && 
               minParticipants > 0 && weight > 0;
    }
    
    @Override
    public String toString() {
        return "EventPreset{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", duration=" + duration +
                ", maxParticipants=" + maxParticipants +
                ", minParticipants=" + minParticipants +
                ", enabled=" + enabled +
                ", weight=" + weight +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EventPreset that = (EventPreset) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
} 