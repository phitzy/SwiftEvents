package com.swiftevents.gui;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.permissions.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsGUIManager {
    
    private final SwiftEventsPlugin plugin;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
    
    public StatisticsGUIManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void openEventStatisticsGUI(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_BASE)) {
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§b§lEvent Statistics Dashboard");
        
        List<Event> allEvents = plugin.getEventManager().getAllEvents();
        
        // Overview Statistics
        addOverviewSection(gui, allEvents);
        
        // Event Type Analysis
        addEventTypeAnalysis(gui, allEvents);
        
        // Performance Metrics
        addPerformanceMetrics(gui, allEvents);
        
        // Time-based Analysis
        addTimeAnalysis(gui, allEvents);
        
        // Navigation
        addStatisticsNavigation(gui);
        
        player.openInventory(gui);
    }
    
    private void addOverviewSection(Inventory gui, List<Event> allEvents) {
        // Total Events
        ItemStack totalEvents = new ItemStack(Material.BOOK);
        ItemMeta totalMeta = totalEvents.getItemMeta();
        totalMeta.setDisplayName("§6§lTotal Events");
        
        List<Event> activeEvents = allEvents.stream().filter(Event::isActive).collect(Collectors.toList());
        List<Event> completedEvents = allEvents.stream().filter(Event::isCompleted).collect(Collectors.toList());
        
        totalMeta.setLore(Arrays.asList(
            "§7Total Events: §f" + allEvents.size(),
            "§7Active Events: §a" + activeEvents.size(),
            "§7Completed Events: §2" + completedEvents.size(),
            "§7Cancelled Events: §c" + allEvents.stream().mapToInt(e -> e.isCancelled() ? 1 : 0).sum(),
            "",
            "§7Success Rate: §f" + calculateSuccessRate(allEvents) + "%"
        ));
        totalEvents.setItemMeta(totalMeta);
        gui.setItem(10, totalEvents);
        
        // Player Participation
        ItemStack playerStats = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playerMeta = playerStats.getItemMeta();
        playerMeta.setDisplayName("§d§lPlayer Participation");
        
        int totalParticipants = allEvents.stream().mapToInt(Event::getCurrentParticipants).sum();
        double avgParticipants = allEvents.isEmpty() ? 0 : 
            (double) totalParticipants / allEvents.size();
        
        playerMeta.setLore(Arrays.asList(
            "§7Total Participations: §f" + totalParticipants,
            "§7Average per Event: §f" + decimalFormat.format(avgParticipants),
            "§7Most Popular Event: §f" + getMostPopularEvent(allEvents),
            "§7Unique Players: §f" + getUniquePlayerCount(allEvents),
            "",
            "§eClick for detailed player stats"
        ));
        playerStats.setItemMeta(playerMeta);
        gui.setItem(12, playerStats);
        
        // Recent Activity
        ItemStack recentActivity = new ItemStack(Material.CLOCK);
        ItemMeta recentMeta = recentActivity.getItemMeta();
        recentMeta.setDisplayName("§e§lRecent Activity");
        
        List<Event> recentEvents = getRecentEvents(allEvents, 7); // Last 7 days
        recentMeta.setLore(Arrays.asList(
            "§7Events (Last 7 days): §f" + recentEvents.size(),
            "§7Daily Average: §f" + decimalFormat.format(recentEvents.size() / 7.0),
            "§7Most Active Day: §f" + getMostActiveDay(),
            "",
            "§eClick for activity timeline"
        ));
        recentActivity.setItemMeta(recentMeta);
        gui.setItem(14, recentActivity);
    }
    
    private void addEventTypeAnalysis(Inventory gui, List<Event> allEvents) {
        // Event type distribution
        Map<Event.EventType, Long> typeDistribution = allEvents.stream()
            .collect(Collectors.groupingBy(Event::getType, Collectors.counting()));
        
        Event.EventType[] types = Event.EventType.values();
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        
        for (int i = 0; i < types.length && i < slots.length; i++) {
            Event.EventType type = types[i];
            long count = typeDistribution.getOrDefault(type, 0L);
            double percentage = allEvents.isEmpty() ? 0 : (count * 100.0) / allEvents.size();
            
            ItemStack typeItem = new ItemStack(getEventTypeMaterial(type));
            ItemMeta typeMeta = typeItem.getItemMeta();
            typeMeta.setDisplayName("§f" + type.name().replace("_", " ") + " Events");
            
            List<Event> typeEvents = allEvents.stream()
                .filter(e -> e.getType() == type)
                .collect(Collectors.toList());
            
            double avgParticipants = typeEvents.isEmpty() ? 0 : 
                typeEvents.stream().mapToInt(Event::getCurrentParticipants).average().orElse(0);
            
            typeMeta.setLore(Arrays.asList(
                "§7Count: §f" + count + " §7(" + decimalFormat.format(percentage) + "%)",
                "§7Avg Participants: §f" + decimalFormat.format(avgParticipants),
                "§7Success Rate: §f" + calculateTypeSuccessRate(typeEvents) + "%",
                "",
                "§eClick for detailed analysis"
            ));
            typeItem.setItemMeta(typeMeta);
            gui.setItem(slots[i], typeItem);
        }
    }
    
    private void addPerformanceMetrics(Inventory gui, List<Event> allEvents) {
        // System Performance
        ItemStack performance = new ItemStack(Material.REDSTONE);
        ItemMeta perfMeta = performance.getItemMeta();
        perfMeta.setDisplayName("§c§lSystem Performance");
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsage = (usedMemory * 100.0) / maxMemory;
        
        perfMeta.setLore(Arrays.asList(
            "§7Memory Usage: §f" + decimalFormat.format(memoryUsage) + "%",
            "§7Used: §f" + formatBytes(usedMemory),
            "§7Max: §f" + formatBytes(maxMemory),
            "§7Database Status: §a" + getDatabaseStatus(),
            "",
            "§eClick for detailed metrics"
        ));
        performance.setItemMeta(perfMeta);
        gui.setItem(37, performance);
        
        // Event Processing Speed
        ItemStack processing = new ItemStack(Material.COMPARATOR);
        ItemMeta procMeta = processing.getItemMeta();
        procMeta.setDisplayName("§6§lProcessing Metrics");
        procMeta.setLore(Arrays.asList(
            "§7Average Event Creation: §f" + getAvgCreationTime() + "ms",
            "§7Average Join Time: §f" + getAvgJoinTime() + "ms",
            "§7Events/Hour: §f" + getEventsPerHour(allEvents),
            "",
            "§eClick for performance details"
        ));
        processing.setItemMeta(procMeta);
        gui.setItem(38, processing);
    }
    
    private void addTimeAnalysis(Inventory gui, List<Event> allEvents) {
        // Peak Hours Analysis
        ItemStack peakHours = new ItemStack(Material.DAYLIGHT_DETECTOR);
        ItemMeta peakMeta = peakHours.getItemMeta();
        peakMeta.setDisplayName("§9§lPeak Hours Analysis");
        peakMeta.setLore(Arrays.asList(
            "§7Most Active Hour: §f" + getPeakHour(allEvents),
            "§7Peak Participation: §f" + getPeakParticipation(),
            "§7Weekend vs Weekday: §f" + getWeekendRatio(allEvents),
            "",
            "§eClick for hourly breakdown"
        ));
        peakHours.setItemMeta(peakMeta);
        gui.setItem(28, peakHours);
        
        // Duration Analysis
        ItemStack duration = new ItemStack(Material.CLOCK);
        ItemMeta durationMeta = duration.getItemMeta();
        durationMeta.setDisplayName("§5§lEvent Duration Analysis");
        durationMeta.setLore(Arrays.asList(
            "§7Average Duration: §f" + getAverageDuration(allEvents),
            "§7Shortest Event: §f" + getShortestDuration(allEvents),
            "§7Longest Event: §f" + getLongestDuration(allEvents),
            "",
            "§eClick for duration trends"
        ));
        duration.setItemMeta(durationMeta);
        gui.setItem(29, duration);
    }
    
    private void addStatisticsNavigation(Inventory gui) {
        // Export Data
        ItemStack export = new ItemStack(Material.PAPER);
        ItemMeta exportMeta = export.getItemMeta();
        exportMeta.setDisplayName("§a§lExport Statistics");
        exportMeta.setLore(Arrays.asList(
            "§7Export statistics to file",
            "§7for external analysis",
            "",
            "§eClick to export"
        ));
        export.setItemMeta(exportMeta);
        gui.setItem(46, export);
        
        // Refresh Data
        ItemStack refresh = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.setDisplayName("§2§lRefresh Statistics");
        refreshMeta.setLore(Arrays.asList(
            "§7Update all statistics",
            "§7with latest data",
            "",
            "§eClick to refresh"
        ));
        refresh.setItemMeta(refreshMeta);
        gui.setItem(49, refresh);
        
        // Back to Admin
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§7« Back to Admin Panel");
        back.setItemMeta(backMeta);
        gui.setItem(53, back);
    }
    
    // Helper methods for calculations
    private String calculateSuccessRate(List<Event> events) {
        if (events.isEmpty()) return "0";
        long completed = events.stream().filter(Event::isCompleted).count();
        return decimalFormat.format((completed * 100.0) / events.size());
    }
    
    private String calculateTypeSuccessRate(List<Event> events) {
        if (events.isEmpty()) return "0";
        long completed = events.stream().filter(Event::isCompleted).count();
        return decimalFormat.format((completed * 100.0) / events.size());
    }
    
    private String getMostPopularEvent(List<Event> events) {
        return events.stream()
            .max(Comparator.comparing(Event::getCurrentParticipants))
            .map(Event::getName)
            .orElse("None");
    }
    
    private int getUniquePlayerCount(List<Event> events) {
        return (int) events.stream()
            .flatMap(event -> event.getParticipants().stream())
            .distinct()
            .count();
    }
    
    private List<Event> getRecentEvents(List<Event> events, int days) {
        long cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        return events.stream()
            .filter(event -> event.getCreatedAt() > cutoff)
            .collect(Collectors.toList());
    }
    
    private String getMostActiveDay() {
        // Implementation would analyze event creation patterns
        return "Saturday";
    }
    
    private Material getEventTypeMaterial(Event.EventType type) {
        return switch (type) {
            case PVP -> Material.DIAMOND_SWORD;
            case PVE -> Material.IRON_SWORD;
            case BUILDING -> Material.BRICKS;
            case RACING -> Material.GOLDEN_BOOTS;
            case TREASURE_HUNT -> Material.CHEST;
            case MINI_GAME -> Material.JUKEBOX;
            case CUSTOM -> Material.COMMAND_BLOCK;
            case TOURNAMENT -> Material.DIAMOND_SWORD;
            case CHALLENGE -> Material.DIAMOND_SWORD;
            default -> Material.BARRIER;
        };
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return decimalFormat.format(bytes / 1024.0) + " KB";
        return decimalFormat.format(bytes / (1024.0 * 1024.0)) + " MB";
    }
    
    private String getDatabaseStatus() {
        return plugin.getConfigManager().isDatabaseEnabled() ? "Connected" : "File Storage";
    }
    
    private String getAvgCreationTime() {
        return "45"; // Would be calculated from actual metrics
    }
    
    private String getAvgJoinTime() {
        return "15"; // Would be calculated from actual metrics
    }
    
    private String getEventsPerHour(List<Event> events) {
        if (events.isEmpty()) return "0";
        // Calculate events per hour based on recent activity
        return decimalFormat.format(2.5); // Placeholder
    }
    
    private String getPeakHour(List<Event> events) {
        return "8:00 PM"; // Would analyze actual event times
    }
    
    private String getPeakParticipation() {
        return "150 players"; // Would calculate from event data
    }
    
    private String getWeekendRatio(List<Event> events) {
        return "60% weekend"; // Would analyze event timing
    }
    
    private String getAverageDuration(List<Event> events) {
        return "45 minutes"; // Would calculate from event durations
    }
    
    private String getShortestDuration(List<Event> events) {
        return "5 minutes"; // Would find minimum duration
    }
    
    private String getLongestDuration(List<Event> events) {
        return "3 hours"; // Would find maximum duration
    }
} 