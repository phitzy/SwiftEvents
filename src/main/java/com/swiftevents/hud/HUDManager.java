package com.swiftevents.hud;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;

public class HUDManager {

    private final SwiftEventsPlugin plugin;
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private final Map<UUID, HUDPreference> playerPreferences = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> bossBarTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerEventDisplayIndices = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerLastRotationTimes = new ConcurrentHashMap<>();

    public enum HUDPreference {
        SIDEBAR, BOSS_BAR, NONE
    }

    public HUDManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }

    public void setPlayerHUDPreference(Player player, HUDPreference preference) {
        if (player == null) return;
        playerPreferences.put(player.getUniqueId(), preference);
        updatePlayerHUD(player);
    }

    public void toggleSidebar(Player player) {
        handleToggle(player, HUDPreference.SIDEBAR, "Sidebar");
    }

    public void toggleBossBar(Player player) {
        handleToggle(player, HUDPreference.BOSS_BAR, "Boss bar");
    }

    private void handleToggle(Player player, HUDPreference targetPreference, String hudName) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        HUDPreference current = playerPreferences.getOrDefault(playerId, HUDPreference.NONE);

        if (current == targetPreference) {
            playerPreferences.put(playerId, HUDPreference.NONE);
            player.sendMessage(Component.text(hudName + " HUD disabled.", NamedTextColor.GREEN));
        } else {
            playerPreferences.put(playerId, targetPreference);
            if (targetPreference == HUDPreference.BOSS_BAR) {
                player.sendMessage(Component.text("Boss bar HUD enabled. Shows active event information.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text(hudName + " HUD enabled.", NamedTextColor.GREEN));
            }
        }
        updatePlayerHUD(player);
    }

    private void updatePlayerHUD(Player player) {
        if (player == null || !player.isOnline()) return;

        HUDPreference preference = playerPreferences.getOrDefault(player.getUniqueId(), HUDPreference.NONE);

        // Always clean up other HUDs
        if (preference != HUDPreference.SIDEBAR) hideSidebar(player);
        if (preference != HUDPreference.BOSS_BAR) hideBossBar(player);

        switch (preference) {
            case SIDEBAR -> showSidebar(player);
            case BOSS_BAR -> showBossBar(player);
            case NONE -> {} // Already hidden
        }
    }

    private void showSidebar(Player player) {
        Scoreboard scoreboard = playerScoreboards.computeIfAbsent(player.getUniqueId(),
                k -> Bukkit.getScoreboardManager().getNewScoreboard());

        Objective objective = scoreboard.getObjective("SwiftEvents");
        if (objective == null) {
            Component title = Component.text("Events", NamedTextColor.GOLD, TextDecoration.BOLD);
            objective = scoreboard.registerNewObjective("SwiftEvents", Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Clear previous entries
        objective.getScoreboard().getEntries().forEach(objective.getScoreboard()::resetScores);

        // Get events relevant to this player: events they're participating in + active events
        List<Event> playerEvents = plugin.getEventManager().getPlayerEvents(player.getUniqueId());
        List<Event> activeEvents = plugin.getEventManager().getActiveEvents();
        
        // Combine and deduplicate events
        Set<Event> relevantEvents = new LinkedHashSet<>();
        relevantEvents.addAll(playerEvents); // Add events player is participating in
        relevantEvents.addAll(activeEvents); // Add active events (might overlap)
        
        List<Event> eventsToShow = new ArrayList<>(relevantEvents);

        if (eventsToShow.isEmpty()) {
            objective.getScore(LegacyComponentSerializer.legacySection().serialize(Component.text("No events to display.", NamedTextColor.GRAY))).setScore(0);
        } else {
            for (int i = 0; i < eventsToShow.size() && i < 15; i++) {
                Event event = eventsToShow.get(i);
                Component eventLine = Component.text("▪ ", NamedTextColor.DARK_GREEN)
                        .append(Component.text(event.getName(), NamedTextColor.GREEN))
                        .append(Component.text(" (", NamedTextColor.GRAY))
                        .append(Component.text(event.getStatus().name(), getStatusColor(event.getStatus())))
                        .append(Component.text(")", NamedTextColor.GRAY));
                objective.getScore(LegacyComponentSerializer.legacySection().serialize(eventLine)).setScore(15 - i);
            }
        }
        player.setScoreboard(scoreboard);
    }

    private void hideSidebar(Player player) {
        Scoreboard scoreboard = playerScoreboards.remove(player.getUniqueId());
        if (scoreboard != null && player.getScoreboard().equals(scoreboard)) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void showBossBar(Player player) {
        hideBossBar(player); // Clean up previous state

        // Get events relevant to this player: events they're participating in + active events
        List<Event> playerEvents = plugin.getEventManager().getPlayerEvents(player.getUniqueId());
        List<Event> activeEvents = plugin.getEventManager().getActiveEvents();
        
        // Combine and deduplicate events
        Set<Event> relevantEvents = new LinkedHashSet<>();
        relevantEvents.addAll(playerEvents); // Add events player is participating in
        relevantEvents.addAll(activeEvents); // Add active events (might overlap)
        
        List<Event> eventsToShow = new ArrayList<>(relevantEvents);
        
        // If no relevant events, show a placeholder bossbar to indicate it's enabled
        if (eventsToShow.isEmpty()) {
            BossBar bossBar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);
            Component title = Component.text("No events to display", NamedTextColor.GRAY, TextDecoration.ITALIC)
                    .append(Component.text(" - Bossbar HUD enabled", NamedTextColor.DARK_GRAY));
            bossBar.setTitle(LegacyComponentSerializer.legacySection().serialize(title));
            bossBar.setProgress(0.0);
            bossBar.addPlayer(player);
            playerBossBars.put(player.getUniqueId(), bossBar);
            
            // Schedule a task to check for events periodically
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline() || playerPreferences.get(player.getUniqueId()) != HUDPreference.BOSS_BAR) {
                    hideBossBar(player);
                    return;
                }
                
                // Recheck for relevant events
                List<Event> currentPlayerEvents = plugin.getEventManager().getPlayerEvents(player.getUniqueId());
                List<Event> currentActiveEvents = plugin.getEventManager().getActiveEvents();
                Set<Event> currentRelevantEvents = new LinkedHashSet<>();
                currentRelevantEvents.addAll(currentPlayerEvents);
                currentRelevantEvents.addAll(currentActiveEvents);
                
                if (!currentRelevantEvents.isEmpty()) {
                    // Events are now available, restart the bossbar with proper content
                    showBossBar(player);
                }
            }, 20L, 20L); // Check every second
            
            bossBarTasks.put(player.getUniqueId(), task);
            return;
        }

        // Update more frequently for better accuracy (every 2 seconds instead of rotation interval)
        long updateInterval = 40L; // 2 seconds
        long rotationInterval = plugin.getConfigManager().getHUDBossBarRotationInterval() * 20L;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || playerPreferences.get(player.getUniqueId()) != HUDPreference.BOSS_BAR) {
                hideBossBar(player);
                return;
            }

            // Recheck for relevant events
            List<Event> currentPlayerEvents = plugin.getEventManager().getPlayerEvents(player.getUniqueId());
            List<Event> currentActiveEvents = plugin.getEventManager().getActiveEvents();
            Set<Event> currentRelevantEvents = new LinkedHashSet<>();
            currentRelevantEvents.addAll(currentPlayerEvents);
            currentRelevantEvents.addAll(currentActiveEvents);
            
            if (currentRelevantEvents.isEmpty()) {
                hideBossBar(player);
                return;
            }

            List<Event> currentEventsToShow = new ArrayList<>(currentRelevantEvents);
            int currentIndex = playerEventDisplayIndices.getOrDefault(player.getUniqueId(), 0);
            if (currentIndex >= currentEventsToShow.size()) {
                currentIndex = 0;
            }

            Event eventToShow = currentEventsToShow.get(currentIndex);
            BossBar bossBar = playerBossBars.computeIfAbsent(player.getUniqueId(), k -> {
                BossBar newBar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
                newBar.addPlayer(player);
                return newBar;
            });

            // Create title with status indicator and better formatting
            Component title = createBossBarTitle(eventToShow);
            bossBar.setTitle(LegacyComponentSerializer.legacySection().serialize(title));

            // Calculate progress more accurately
            double progress = calculateBossBarProgress(eventToShow);
            bossBar.setProgress(progress);

            // Update color based on event status
            bossBar.setColor(getBossBarColor(eventToShow.getStatus()));

            // Rotate to next event based on rotation interval
            long currentTime = System.currentTimeMillis();
            Long lastRotation = playerLastRotationTimes.get(player.getUniqueId());
            if (lastRotation == null || (currentTime - lastRotation) >= rotationInterval) {
                playerEventDisplayIndices.put(player.getUniqueId(), (currentIndex + 1) % currentEventsToShow.size());
                playerLastRotationTimes.put(player.getUniqueId(), currentTime);
            }

        }, 0L, updateInterval);

        bossBarTasks.put(player.getUniqueId(), task);
    }

    private Component createBossBarTitle(Event event) {
        Component title = Component.text(event.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text(event.getStatus().name(), getStatusColor(event.getStatus())));
        
        // Add time information based on event status
        if (event.isActive() && event.getEndTime() > 0) {
            title = title.append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(event.getFormattedRemainingTime(), NamedTextColor.YELLOW));
        } else if (event.isScheduled() && event.getStartTime() > 0) {
            long timeUntilStart = event.getStartTime() - System.currentTimeMillis();
            if (timeUntilStart > 0) {
                title = title.append(Component.text(" - Starts in ", NamedTextColor.GRAY))
                        .append(Component.text(formatTime(timeUntilStart), NamedTextColor.GREEN));
            } else {
                title = title.append(Component.text(" - Starting now!", NamedTextColor.GREEN));
            }
        } else if (event.getStatus() == Event.EventStatus.CREATED) {
            title = title.append(Component.text(" - Waiting to start", NamedTextColor.GRAY));
        } else if (event.getStatus() == Event.EventStatus.PAUSED) {
            title = title.append(Component.text(" - Paused", NamedTextColor.GOLD));
        }
        
        // Add participant count for active/scheduled events
        if (event.isActive() || event.isScheduled()) {
            title = title.append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(event.getCurrentParticipants() + "/" + 
                            (event.hasUnlimitedSlots() ? "∞" : event.getMaxParticipants()) + " players", 
                            NamedTextColor.AQUA));
        }
        
        return title;
    }

    private double calculateBossBarProgress(Event event) {
        if (event.getStatus() == Event.EventStatus.COMPLETED || 
            event.getStatus() == Event.EventStatus.CANCELLED) {
            return 0.0;
        }
        
        if (event.getStatus() == Event.EventStatus.CREATED) {
            return 0.1; // Small progress to show it exists
        }
        
        if (event.getStatus() == Event.EventStatus.PAUSED) {
            return 0.5; // Half progress to indicate paused state
        }
        
        // For active events, calculate based on remaining time
        if (event.isActive() && event.getEndTime() > 0) {
            long duration = event.getDuration();
            if (duration > 0) {
                long remaining = event.getRemainingTime();
                return Math.max(0.0, Math.min(1.0, (double) remaining / duration));
            }
        }
        
        // For scheduled events, calculate based on time until start
        if (event.isScheduled() && event.getStartTime() > 0) {
            long timeUntilStart = event.getStartTime() - System.currentTimeMillis();
            if (timeUntilStart > 0) {
                // Show progress based on how close to start time (max 30 minutes)
                long maxWaitTime = Math.min(30 * 60 * 1000, timeUntilStart); // 30 minutes max
                return Math.max(0.1, Math.min(0.9, 1.0 - ((double) timeUntilStart / maxWaitTime)));
            }
        }
        
        return 1.0; // Default to full progress
    }

    private BarColor getBossBarColor(Event.EventStatus status) {
        return switch (status) {
            case CREATED -> BarColor.WHITE;
            case SCHEDULED -> BarColor.YELLOW;
            case ACTIVE -> BarColor.GREEN;
            case PAUSED -> BarColor.PURPLE;
            case COMPLETED -> BarColor.BLUE;
            case CANCELLED -> BarColor.RED;
        };
    }

    private String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "now";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    private void hideBossBar(Player player) {
        BukkitTask existingTask = bossBarTasks.remove(player.getUniqueId());
        if (existingTask != null) existingTask.cancel();

        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) bossBar.removeAll();

        playerEventDisplayIndices.remove(player.getUniqueId());
        playerLastRotationTimes.remove(player.getUniqueId());
    }

    public void sendActionBarMessage(Player player, String message) {
        if (player == null || !player.isOnline() || message == null) return;
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    public void updateActiveEvents() {
        Bukkit.getOnlinePlayers().forEach(this::updatePlayerHUD);
    }

    public void forceUpdatePlayerHUD(Player player) {
        if (player == null || !player.isOnline()) return;
        updatePlayerHUD(player);
    }

    public void clearPlayerHUD(Player player) {
        if (player == null) return;
        playerPreferences.put(player.getUniqueId(), HUDPreference.NONE);
        updatePlayerHUD(player);
    }

    public void shutdown() {
        // Cancel all running tasks
        bossBarTasks.values().forEach(BukkitTask::cancel);
        bossBarTasks.clear();

        // Remove all boss bars
        playerBossBars.values().forEach(BossBar::removeAll);
        playerBossBars.clear();

        // Clear scoreboards for online players
        playerScoreboards.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(p -> p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()));
        playerScoreboards.clear();

        playerPreferences.clear();
        playerEventDisplayIndices.clear();
        playerLastRotationTimes.clear();

        plugin.getLogger().info("HUDManager shutdown and cleaned up resources.");
    }

    private NamedTextColor getStatusColor(Event.EventStatus status) {
        return switch (status) {
            case CREATED -> NamedTextColor.GRAY;
            case SCHEDULED -> NamedTextColor.YELLOW;
            case ACTIVE -> NamedTextColor.GREEN;
            case PAUSED -> NamedTextColor.GOLD;
            case COMPLETED -> NamedTextColor.DARK_GREEN;
            case CANCELLED -> NamedTextColor.RED;
        };
    }
} 