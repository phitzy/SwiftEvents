package com.swiftevents.chat;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {
    
    public enum EventAnnouncement {
        CREATED, STARTING, ENDED, REMINDER
    }
    
    private final SwiftEventsPlugin plugin;
    private final MiniMessage miniMessage;
    
    // Cache for formatted messages to improve performance
    private final Map<String, Component> messageCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 30000; // 30 seconds
    
    // Teleport requests - prevent spam
    private final Map<UUID, Long> lastTeleportRequest = new ConcurrentHashMap<>();
    private static final long TELEPORT_COOLDOWN = 5000; // 5 seconds
    
    // Event type colors for visual consistency
    private final Map<Event.EventType, TextColor> eventTypeColors = Map.of(
        Event.EventType.PVP, TextColor.color(255, 85, 85),        // Red
        Event.EventType.PVE, TextColor.color(255, 170, 0),        // Orange
        Event.EventType.BUILDING, TextColor.color(85, 255, 85),   // Green
        Event.EventType.RACING, TextColor.color(85, 85, 255),     // Blue
        Event.EventType.TREASURE_HUNT, TextColor.color(255, 255, 85), // Yellow
        Event.EventType.MINI_GAME, TextColor.color(255, 85, 255), // Magenta
        Event.EventType.CUSTOM, TextColor.color(170, 170, 170)    // Gray
    );
    
    // Background task for cache cleanup
    private BukkitTask cacheCleanupTask;
    
    public ChatManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        
        // Start cache cleanup task
        startCacheCleanup();
    }
    
    /**
     * Announces an event to all players or specific audience
     */
    public void announceEvent(Event event, EventAnnouncement type) {
        announceEvent(event, type, Bukkit.getServer());
    }
    
    public void announceEvent(Event event, EventAnnouncement type, Audience audience) {
        Component message = createEventAnnouncement(event, type);
        
        // Send different presentation based on announcement type
        switch (type) {
            case CREATED -> {
                audience.sendMessage(message);
                if (plugin.getConfigManager().getConfig().getBoolean("chat.sound_effects", true)) {
                    playAnnouncementSound(audience, "ui.toast.challenge_complete");
                }
            }
            case STARTING -> {
                audience.sendMessage(message);
                // Send title for starting events
                Component title = Component.text("Event Starting!")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);
                Component subtitle = Component.text(event.getName())
                    .color(getEventTypeColor(event.getType()));
                    
                audience.showTitle(Title.title(title, subtitle, 
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))));
                    
                if (plugin.getConfigManager().getConfig().getBoolean("chat.sound_effects", true)) {
                    playAnnouncementSound(audience, "block.note_block.chime");
                }
            }
            case ENDED -> {
                audience.sendMessage(message);
                if (plugin.getConfigManager().getConfig().getBoolean("chat.sound_effects", true)) {
                    playAnnouncementSound(audience, "entity.experience_orb.pickup");
                }
            }
            case REMINDER -> {
                audience.sendMessage(message);
            }
        }
    }
    
    /**
     * Creates a beautifully formatted event announcement with clickable elements
     */
    private Component createEventAnnouncement(Event event, EventAnnouncement type) {
        String cacheKey = event.getId() + "_" + type.name() + "_" + event.getStatus().name();
        
        // Check cache first
        if (messageCache.containsKey(cacheKey)) {
            Long timestamp = cacheTimestamps.get(cacheKey);
            if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                return messageCache.get(cacheKey);
            }
        }
        
        Component announcement = buildEventAnnouncement(event, type);
        
        // Cache the message
        messageCache.put(cacheKey, announcement);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        
        return announcement;
    }
    
    private Component buildEventAnnouncement(Event event, EventAnnouncement type) {
        TextColor eventColor = getEventTypeColor(event.getType());
        
        // Create announcement header
        Component separator = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            .color(NamedTextColor.DARK_GRAY);
        
        // Event status icon
        String statusIcon = getStatusIcon(type);
        String statusText = getStatusText(type);
        
        // Main announcement line
        Component mainLine = Component.text()
            .append(Component.text(statusIcon + " ").color(eventColor))
            .append(Component.text(statusText + " ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
            .append(Component.text(event.getName()).color(eventColor).decorate(TextDecoration.BOLD))
            .append(Component.text(" [" + event.getType().name() + "]").color(NamedTextColor.GRAY))
            .build();
        
        // Description line
        Component descLine = Component.text("  " + event.getDescription())
            .color(NamedTextColor.GRAY);
        
        // Action buttons
        Component actionLine = createActionButtons(event);
        
        // Build final announcement
        return Component.text()
            .append(separator)
            .append(Component.newline())
            .append(mainLine)
            .append(Component.newline())
            .append(descLine)
            .append(Component.newline())
            .append(actionLine)
            .append(Component.newline())
            .append(separator)
            .build();
    }
    
    private Component createActionButtons(Event event) {
        List<Component> buttons = new ArrayList<>();
        
        // Teleport button
        if (event.getWorld() != null) {
            Component teleportButton = Component.text("  [⚡ TELEPORT] ").color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/event teleport " + event.getId()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to the event location!")
                    .color(NamedTextColor.YELLOW)));
            buttons.add(teleportButton);
        }
        
        // Join/Leave button
        Component joinButton;
        if (event.canJoin()) {
            joinButton = Component.text("[📋 JOIN] ").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/event join " + event.getId()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to join this event!")
                    .color(NamedTextColor.GREEN)));
        } else {
            joinButton = Component.text("[📋 INFO] ").color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/event info " + event.getId()))
                .hoverEvent(HoverEvent.showText(Component.text("Click for event information")
                    .color(NamedTextColor.YELLOW)));
        }
        buttons.add(joinButton);
        
        // Share button
        Component shareButton = Component.text("[📤 SHARE] ").color(NamedTextColor.LIGHT_PURPLE)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.suggestCommand("/tell <player> Join the event: " + event.getName() + " - /event join " + event.getId()))
            .hoverEvent(HoverEvent.showText(Component.text("Click to share this event with others!")
                .color(NamedTextColor.LIGHT_PURPLE)));
        buttons.add(shareButton);
        
        return Component.join(Component.space(), buttons);
    }
    
    private String getStatusIcon(EventAnnouncement type) {
        return switch (type) {
            case CREATED -> "🎉";
            case STARTING -> "▶️";
            case ENDED -> "🏁";
            case REMINDER -> "⏰";
        };
    }
    
    private String getStatusText(EventAnnouncement type) {
        return switch (type) {
            case CREATED -> "NEW EVENT CREATED";
            case STARTING -> "EVENT STARTING";
            case ENDED -> "EVENT ENDED";
            case REMINDER -> "EVENT REMINDER";
        };
    }
    
    private TextColor getEventTypeColor(Event.EventType type) {
        return eventTypeColors.getOrDefault(type, NamedTextColor.WHITE);
    }
    
    private void playAnnouncementSound(Audience audience, String sound) {
        if (audience instanceof Player player) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }
    
    /**
     * Handles teleportation to event locations with cooldown
     */
    public boolean teleportToEvent(Player player, Event event) {
        UUID playerId = player.getUniqueId();
        
        // Check cooldown
        if (lastTeleportRequest.containsKey(playerId)) {
            long lastRequest = lastTeleportRequest.get(playerId);
            if (System.currentTimeMillis() - lastRequest < TELEPORT_COOLDOWN) {
                long remaining = (TELEPORT_COOLDOWN - (System.currentTimeMillis() - lastRequest)) / 1000;
                player.sendMessage(Component.text("Please wait " + remaining + " seconds before teleporting again!")
                    .color(NamedTextColor.RED));
                return false;
            }
        }
        
        // Check if event has location
        if (event.getWorld() == null) {
            player.sendMessage(Component.text("This event doesn't have a set location!")
                .color(NamedTextColor.RED));
            return false;
        }
        
        // Get location
        Location eventLocation = new Location(
            Bukkit.getWorld(event.getWorld()),
            event.getX(),
            event.getY(),
            event.getZ()
        );
        
        if (eventLocation.getWorld() == null) {
            player.sendMessage(Component.text("Event world not found!")
                .color(NamedTextColor.RED));
            return false;
        }
        
        // Update cooldown
        lastTeleportRequest.put(playerId, System.currentTimeMillis());
        
        // Teleport with style
        Component teleportMessage = Component.text()
            .append(Component.text("✨ Teleporting to ").color(NamedTextColor.AQUA))
            .append(Component.text(event.getName()).color(getEventTypeColor(event.getType())).decorate(TextDecoration.BOLD))
            .append(Component.text("...").color(NamedTextColor.AQUA))
            .build();
        
        player.sendMessage(teleportMessage);
        player.teleport(eventLocation);
        
        // Play teleport sound
        player.playSound(player.getLocation(), "entity.enderman.teleport", 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Sends an event reminder to specific players
     */
    public void sendEventReminder(Event event, Collection<Player> players) {
        Component reminder = createEventReminder(event);
        players.forEach(player -> player.sendMessage(reminder));
    }
    
    private Component createEventReminder(Event event) {
        TextColor eventColor = getEventTypeColor(event.getType());
        
        return Component.text()
            .append(Component.text("🔔 ").color(NamedTextColor.YELLOW))
            .append(Component.text("Reminder: ").color(NamedTextColor.WHITE))
            .append(Component.text(event.getName()).color(eventColor).decorate(TextDecoration.BOLD))
            .append(Component.text(" starts in ").color(NamedTextColor.WHITE))
            .append(Component.text(event.getFormattedRemainingTime()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
            .append(Component.text("!").color(NamedTextColor.WHITE))
            .build();
    }
    
    /**
     * Starts background tasks
     */
    private void startCacheCleanup() {
        // Clean cache every 5 minutes
        cacheCleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            cacheTimestamps.entrySet().removeIf(entry -> {
                if (currentTime - entry.getValue() > CACHE_DURATION) {
                    messageCache.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        }, 6000L, 6000L); // 5 minutes in ticks
    }
    
    /**
     * Cleanup method
     */
    public void shutdown() {
        if (cacheCleanupTask != null) {
            cacheCleanupTask.cancel();
        }
        messageCache.clear();
        cacheTimestamps.clear();
        lastTeleportRequest.clear();
    }
}
 