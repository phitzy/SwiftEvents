package com.swiftevents.hud;

import com.swiftevents.SwiftEventsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class HUDManager {
    
    private final SwiftEventsPlugin plugin;
    private final Map<UUID, BossBar> playerBossBars;
    private final Map<UUID, BukkitTask> actionBarTasks;
    
    // Optimization: Pre-created components to reduce allocations
    private static final Component EMPTY_COMPONENT = Component.empty();
    private static final TextColor GOLD_COLOR = TextColor.color(255, 215, 0);
    private static final TextColor WHITE_COLOR = TextColor.color(255, 255, 255);
    
    // Optimization: String builders for different thread contexts
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = ThreadLocal.withInitial(() -> new StringBuilder(128));
    
    public HUDManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.playerBossBars = new ConcurrentHashMap<>();
        this.actionBarTasks = new ConcurrentHashMap<>();
    }
    
    public void sendHUDMessage(Player player, String message) {
        if (!plugin.getConfigManager().isHUDEnabled() || player == null || !player.isOnline()) {
            return;
        }
        
        String position = plugin.getConfigManager().getHUDPosition().toUpperCase();
        
        switch (position) {
            case "ACTION_BAR":
                sendActionBarMessage(player, message);
                break;
            case "BOSS_BAR":
                sendBossBarMessage(player, message);
                break;
            case "TITLE":
                sendTitleMessage(player, message);
                break;
            default:
                sendActionBarMessage(player, message);
                break;
        }
    }
    
    private void sendActionBarMessage(Player player, String message) {
        // Optimization: Reuse component creation
        Component component = Component.text(message).color(GOLD_COLOR);
        player.sendActionBar(component);
    }
    
    private void sendBossBarMessage(Player player, String message) {
        UUID playerId = player.getUniqueId();
        
        // Remove existing boss bar if present
        BossBar existingBar = playerBossBars.remove(playerId);
        if (existingBar != null) {
            existingBar.removePlayer(player);
        }
        
        // Create new boss bar
        BossBar bossBar = Bukkit.createBossBar(message, BarColor.YELLOW, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
        
        playerBossBars.put(playerId, bossBar);
        
        // Auto-remove after duration
        int duration = plugin.getConfigManager().getHUDNotificationDuration();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BossBar bar = playerBossBars.remove(playerId);
            if (bar != null) {
                bar.removePlayer(player);
            }
        }, duration * 20L);
    }
    
    private void sendTitleMessage(Player player, String message) {
        // Optimization: Reuse component creation
        Component titleComponent = Component.text("Event Update").color(GOLD_COLOR);
        Component subtitleComponent = Component.text(message).color(WHITE_COLOR);
        
        player.showTitle(net.kyori.adventure.title.Title.title(titleComponent, subtitleComponent));
    }
    
    public void sendEventNotification(Player player, String eventName, String notificationType, String details) {
        if (!plugin.getConfigManager().isHUDEnabled() || player == null || !player.isOnline()) {
            return;
        }
        
        // Null safety for parameters
        if (eventName == null || notificationType == null || details == null) {
            plugin.getLogger().warning("Attempted to send HUD notification with null parameters");
            return;
        }
        
        // Optimization: Use StringBuilder for message construction
        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0); // Clear the builder
        
        switch (notificationType.toLowerCase()) {
            case "start":
                sb.append("§aEvent Started: §6").append(eventName).append(" §7- ").append(details);
                break;
            case "end":
                sb.append("§cEvent Ended: §6").append(eventName).append(" §7- ").append(details);
                break;
            case "join":
                sb.append("§bJoined Event: §6").append(eventName).append(" §7- ").append(details);
                break;
            case "leave":
                sb.append("§eLeft Event: §6").append(eventName).append(" §7- ").append(details);
                break;
            case "update":
                sb.append("§6").append(eventName).append(" §7- ").append(details);
                break;
            default:
                sb.append("§6").append(eventName).append(" §7- ").append(details);
                break;
        }
        
        sendHUDMessage(player, sb.toString());
    }
    
    public void sendEventCountdown(Player player, String eventName, long remainingSeconds) {
        if (!plugin.getConfigManager().isHUDEnabled() || player == null || !player.isOnline()) {
            return;
        }
        
        // Optimization: Use StringBuilder for message construction
        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0); // Clear the builder
        
        String timeFormatted = formatTime(remainingSeconds);
        sb.append("§6").append(eventName).append(" §7- §c").append(timeFormatted).append(" remaining");
        
        sendHUDMessage(player, sb.toString());
    }
    
    // Optimized time formatting with StringBuilder
    private String formatTime(long seconds) {
        if (seconds <= 0) {
            return "Starting now!";
        }
        
        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0); // Clear the builder
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            sb.append(String.format("%02d:%02d:%02d", hours, minutes, secs));
        } else if (minutes > 0) {
            sb.append(String.format("%02d:%02d", minutes, secs));
        } else {
            sb.append(secs).append("s");
        }
        
        return sb.toString();
    }
    
    public void broadcastEventNotification(String message) {
        if (!plugin.getConfigManager().isHUDEnabled()) {
            return;
        }
        
        // Optimization: Get online players once and iterate
        var onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            return;
        }
        
        for (Player player : onlinePlayers) {
            sendHUDMessage(player, message);
        }
    }
    
    public void clearPlayerHUD(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Clear boss bar
        BossBar bossBar = playerBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
        
        // Clear action bar
        BukkitTask task = actionBarTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Clear action bar with empty message
        if (player.isOnline()) {
            player.sendActionBar(EMPTY_COMPONENT);
        }
    }
    
    public void clearAllHUDs() {
        // Clear all boss bars
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        playerBossBars.clear();
        
        // Clear all action bar tasks
        for (BukkitTask task : actionBarTasks.values()) {
            task.cancel();
        }
        actionBarTasks.clear();
        
        // Clear action bars for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(EMPTY_COMPONENT);
        }
    }
    
    public void updateEventProgress(Player player, String eventName, double progress, String additionalInfo) {
        if (!plugin.getConfigManager().isHUDEnabled() || player == null || !player.isOnline()) {
            return;
        }
        
        String position = plugin.getConfigManager().getHUDPosition().toUpperCase();
        
        if ("BOSS_BAR".equals(position)) {
            UUID playerId = player.getUniqueId();
            BossBar bossBar = playerBossBars.get(playerId);
            
            if (bossBar != null) {
                // Update existing boss bar
                StringBuilder sb = STRING_BUILDER.get();
                sb.setLength(0);
                sb.append("§6").append(eventName);
                if (additionalInfo != null && !additionalInfo.isEmpty()) {
                    sb.append(" §7- ").append(additionalInfo);
                }
                
                bossBar.setTitle(sb.toString());
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress))); // Clamp between 0 and 1
            } else {
                // Create new boss bar for progress
                StringBuilder sb = STRING_BUILDER.get();
                sb.setLength(0);
                sb.append("§6").append(eventName);
                if (additionalInfo != null && !additionalInfo.isEmpty()) {
                    sb.append(" §7- ").append(additionalInfo);
                }
                
                BossBar newBossBar = Bukkit.createBossBar(sb.toString(), BarColor.YELLOW, BarStyle.SOLID);
                newBossBar.addPlayer(player);
                newBossBar.setProgress(Math.max(0.0, Math.min(1.0, progress))); // Clamp between 0 and 1
                
                playerBossBars.put(playerId, newBossBar);
            }
        } else {
            // For other HUD types, send a regular message
            StringBuilder sb = STRING_BUILDER.get();
            sb.setLength(0);
            sb.append("§6").append(eventName);
            if (additionalInfo != null && !additionalInfo.isEmpty()) {
                sb.append(" §7- ").append(additionalInfo);
            }
            sb.append(" §7(").append(String.format("%.1f", progress * 100)).append("%)");
            
            sendHUDMessage(player, sb.toString());
        }
    }

    public void shutdown() {
        clearAllHUDs();
        plugin.getLogger().info("HUDManager shutdown complete");
    }
} 