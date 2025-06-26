package com.swiftevents.tasker;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.chat.ChatManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class EventTasker {
    
    private final SwiftEventsPlugin plugin;
    private BukkitTask taskerTask;
    private BukkitTask announceTask;
    private long lastEventTime;
    private long nextEventTime;
    private boolean running;
    private final Map<String, EventPreset> presets;
    private final Map<String, Integer> eventCounters;
    private final List<String> upcomingAnnouncements;
    
    public EventTasker(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.presets = new HashMap<>();
        this.eventCounters = new HashMap<>();
        this.upcomingAnnouncements = new ArrayList<>();
        this.lastEventTime = System.currentTimeMillis();
        this.running = false;
        
        loadPresets();
        scheduleNextEvent();
    }
    
    public void start() {
        if (!plugin.getConfigManager().isEventTaskerEnabled() || running) {
            return;
        }
        
        running = true;
        long checkInterval = plugin.getConfigManager().getTaskerCheckInterval() * 20L; // Convert to ticks
        
        // Main tasker task - checks if it's time to start events
        taskerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkEventSchedule, 0L, checkInterval);
        
        // Announcement task - runs every minute to check for announcements
        announceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAnnouncements, 0L, 1200L); // 60 seconds
        
        plugin.getLogger().info("Event Tasker has been started!");
    }
    
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        if (taskerTask != null) {
            taskerTask.cancel();
            taskerTask = null;
        }
        
        if (announceTask != null) {
            announceTask.cancel();
            announceTask = null;
        }
        
        plugin.getLogger().info("Event Tasker has been stopped!");
    }
    
    public void restart() {
        stop();
        loadPresets();
        scheduleNextEvent();
        start();
    }
    
    private void loadPresets() {
        presets.clear();
        eventCounters.clear();
        
        ConfigurationSection presetsSection = plugin.getConfig().getConfigurationSection("event_tasker.presets");
        if (presetsSection == null) {
            plugin.getLogger().warning("No event presets found in configuration!");
            return;
        }
        
        for (String key : presetsSection.getKeys(false)) {
            ConfigurationSection presetSection = presetsSection.getConfigurationSection(key);
            if (presetSection == null) continue;
            
            try {
                EventPreset preset = new EventPreset(key, presetSection);
                if (preset.isEnabled()) {
                    presets.put(key, preset);
                    eventCounters.put(key, 1);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load preset '" + key + "': " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + presets.size() + " event presets");
    }
    
    private void scheduleNextEvent() {
        if (presets.isEmpty()) {
            return;
        }
        
        int minInterval = plugin.getConfigManager().getMinEventInterval() * 60 * 1000; // Convert to milliseconds
        int maxInterval = plugin.getConfigManager().getMaxEventInterval() * 60 * 1000;
        
        long randomInterval = ThreadLocalRandom.current().nextLong(minInterval, maxInterval + 1);
        nextEventTime = System.currentTimeMillis() + randomInterval;
        
        plugin.getLogger().info("Next automatic event scheduled in " + (randomInterval / 60000) + " minutes");
    }
    
    private void checkEventSchedule() {
        if (!running || presets.isEmpty()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Check if it's time for the next event
        if (currentTime >= nextEventTime) {
            createAndStartAutomaticEvent();
            scheduleNextEvent();
        }
    }
    
    private void checkAnnouncements() {
        if (!plugin.getConfigManager().isAnnounceUpcoming() || !running) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long announceTime = plugin.getConfigManager().getAnnounceTime() * 60 * 1000; // Convert to milliseconds
        
        // Check if we should announce the upcoming event
        if (nextEventTime - currentTime <= announceTime && nextEventTime - currentTime > 0) {
            long minutesUntil = (nextEventTime - currentTime) / 60000;
            if (minutesUntil > 0) {
                announceUpcomingEvent(minutesUntil);
            }
        }
    }
    
    private void createAndStartAutomaticEvent() {
        EventPreset selectedPreset = selectRandomPreset();
        if (selectedPreset == null) {
            plugin.getLogger().warning("No valid presets available for automatic event creation!");
            return;
        }
        
        // Create the event
        String eventName = selectedPreset.getName().replace("{number}", 
                String.valueOf(eventCounters.get(selectedPreset.getId())));
        
        Event event = plugin.getEventManager().createEvent(
                eventName,
                selectedPreset.getDescription(),
                selectedPreset.getType(),
                null // System-created event
        );
        
        if (event == null) {
            plugin.getLogger().warning("Failed to create automatic event: " + eventName);
            return;
        }
        
        // Configure the event
        event.setMaxParticipants(selectedPreset.getMaxParticipants());
        event.setRewards(new ArrayList<>(selectedPreset.getRewards()));
        event.setStartTime(System.currentTimeMillis());
        event.setEndTime(System.currentTimeMillis() + (selectedPreset.getDuration() * 1000L));
        event.setStatus(Event.EventStatus.SCHEDULED);
        
        // Add tasker metadata
        event.addMetadata("auto_created", true);
        event.addMetadata("preset_id", selectedPreset.getId());
        event.addMetadata("min_participants", selectedPreset.getMinParticipants());
        
        // Save the event
        plugin.getEventManager().saveEvent(event);
        
        // Increment counter for this preset
        eventCounters.put(selectedPreset.getId(), eventCounters.get(selectedPreset.getId()) + 1);
        
        // Announce the event
        announceNewEvent(event);
        
        // Start the event immediately
        plugin.getEventManager().startEvent(event.getId());
        
        plugin.getLogger().info("Automatic event created and started: " + eventName);
        lastEventTime = System.currentTimeMillis();
    }
    
    private EventPreset selectRandomPreset() {
        List<EventPreset> availablePresets = new ArrayList<>();
        
        // Build weighted list
        for (EventPreset preset : presets.values()) {
            if (!preset.isEnabled()) continue;
            
            // Add preset multiple times based on weight
            for (int i = 0; i < preset.getWeight(); i++) {
                availablePresets.add(preset);
            }
        }
        
        if (availablePresets.isEmpty()) {
            return null;
        }
        
        return availablePresets.get(ThreadLocalRandom.current().nextInt(availablePresets.size()));
    }
    
    private void announceUpcomingEvent(long minutesUntil) {
        String announcement = "§6[SwiftEvents] §eAn automatic event will start in " + minutesUntil + " minute" + 
                (minutesUntil == 1 ? "" : "s") + "! §a/eventgui §eto join when it begins!";
        
        // Only announce once per time slot
        String timeKey = String.valueOf(minutesUntil);
        if (upcomingAnnouncements.contains(timeKey)) {
            return;
        }
        upcomingAnnouncements.add(timeKey);
        
        // Clean old announcements
        if (upcomingAnnouncements.size() > 10) {
            upcomingAnnouncements.clear();
        }
        
        Bukkit.broadcastMessage(announcement);
    }
    
    private void announceNewEvent(Event event) {
        // Use the new ChatManager for beautiful announcements
        if (plugin.getConfigManager().getConfig().getBoolean("chat.enabled", true)) {
            plugin.getChatManager().announceEvent(event, ChatManager.EventAnnouncement.CREATED);
        } else {
            // Fallback to basic announcement if chat is disabled
            String announcement = "§6[SwiftEvents] §aA new automatic event has started: §e" + event.getName() + 
                    "§a! §bUse §a/event join " + event.getName().replace(" ", "_") + " §bto participate!";
            Bukkit.broadcastMessage(announcement);
        }
        
        // Send HUD notification to all players
        if (plugin.getConfigManager().isHUDEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getHUDManager().sendHUDMessage(player, "§6New Event: §e" + event.getName());
            }
        }
    }
    
    // Getters and utility methods
    public boolean isRunning() {
        return running;
    }
    
    public long getNextEventTime() {
        return nextEventTime;
    }
    
    public long getTimeUntilNextEvent() {
        return Math.max(0, nextEventTime - System.currentTimeMillis());
    }
    
    public Map<String, EventPreset> getPresets() {
        return new HashMap<>(presets);
    }
    
    public EventPreset getPreset(String id) {
        return presets.get(id);
    }
    
    public void forceNextEvent() {
        nextEventTime = System.currentTimeMillis();
        plugin.getLogger().info("Next automatic event has been forced to start immediately");
    }
    
    public void setPresetEnabled(String presetId, boolean enabled) {
        EventPreset preset = presets.get(presetId);
        if (preset != null) {
            preset.setEnabled(enabled);
            plugin.getConfig().set("event_tasker.presets." + presetId + ".enabled", enabled);
            plugin.saveConfig();
        }
    }
} 