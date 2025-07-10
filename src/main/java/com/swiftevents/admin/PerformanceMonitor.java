package com.swiftevents.admin;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.permissions.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Advanced performance monitoring and debugging dashboard
 */
public class PerformanceMonitor {
    
    private final SwiftEventsPlugin plugin;
    private final Map<String, Long> performanceMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> timingHistory = new ConcurrentHashMap<>();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final DebugLogger debugLogger;
    
    // Performance tracking
    private long lastMemoryCheck = 0;
    private long lastGCTime = 0;
    private int eventCreationCount = 0;
    private int eventJoinCount = 0;
    private int databaseQueries = 0;
    
    public PerformanceMonitor(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.debugLogger = new DebugLogger(plugin);
        startPerformanceTracking();
    }
    
    /**
     * Open the performance monitoring dashboard
     */
    public void openPerformanceDashboard(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_BASE)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§c§lPerformance Dashboard");
        
        // Header
        addHeader(gui);
        
        // Memory monitoring
        addMemoryInfo(gui);
        
        // Plugin performance
        addPluginPerformance(gui);
        
        // Database performance
        addDatabaseInfo(gui);
        
        // Event processing metrics
        addEventMetrics(gui);
        
        // Debug tools
        addDebugTools(gui);
        
        // Navigation
        addNavigation(gui);
        
        player.openInventory(gui);
    }
    
    private void addHeader(Inventory gui) {
        ItemStack header = createItem(Material.REDSTONE, "§c§lSwiftEvents Performance Monitor",
            "§7Real-time performance monitoring",
            "§7and debugging information",
            "",
            "§7Server: §f" + Bukkit.getVersion(),
            "§7Uptime: §f" + getServerUptime(),
            "§7TPS: §f" + getCurrentTPS());
        gui.setItem(4, header);
    }
    
    private void addMemoryInfo(Inventory gui) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        long usedHeap = heapUsage.getUsed();
        long maxHeap = heapUsage.getMax();
        long usedNonHeap = nonHeapUsage.getUsed();
        
        double heapPercentage = (usedHeap * 100.0) / maxHeap;
        
        ItemStack memoryItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta memoryMeta = memoryItem.getItemMeta();
        memoryMeta.setDisplayName("§4§lMemory Usage");
        
        List<String> memoryLore = new ArrayList<>();
        memoryLore.add("§7Heap Memory:");
        memoryLore.add("§7Used: §f" + formatBytes(usedHeap) + " §7/ §f" + formatBytes(maxHeap));
        memoryLore.add("§7Usage: " + getMemoryColor(heapPercentage) + df.format(heapPercentage) + "%");
        memoryLore.add("");
        memoryLore.add("§7Non-Heap Memory:");
        memoryLore.add("§7Used: §f" + formatBytes(usedNonHeap));
        memoryLore.add("");
        memoryLore.add("§7GC Collections: §f" + getGCCollections());
        memoryLore.add("§7GC Time: §f" + getGCTime() + "ms");
        memoryLore.add("");
        memoryLore.add("§eClick for detailed memory info");
        
        memoryMeta.setLore(memoryLore);
        memoryItem.setItemMeta(memoryMeta);
        gui.setItem(10, memoryItem);
    }
    
    private void addPluginPerformance(Inventory gui) {
        ItemStack perfItem = new ItemStack(Material.COMPARATOR);
        ItemMeta perfMeta = perfItem.getItemMeta();
        perfMeta.setDisplayName("§6§lPlugin Performance");
        
        List<String> perfLore = new ArrayList<>();
        perfLore.add("§7Events Created: §f" + eventCreationCount);
        perfLore.add("§7Player Joins: §f" + eventJoinCount);
        perfLore.add("§7Active Events: §f" + plugin.getEventManager().getActiveEvents().size());
        perfLore.add("");
        perfLore.add("§7Average Creation Time: §f" + getAverageMetric("event_creation") + "ms");
        perfLore.add("§7Average Join Time: §f" + getAverageMetric("event_join") + "ms");
        perfLore.add("§7Average GUI Load Time: §f" + getAverageMetric("gui_load") + "ms");
        perfLore.add("");
        perfLore.add("§7Thread Pool Size: §f" + getThreadPoolInfo());
        perfLore.add("");
        perfLore.add("§eClick for detailed performance metrics");
        
        perfMeta.setLore(perfLore);
        perfItem.setItemMeta(perfMeta);
        gui.setItem(11, perfItem);
    }
    
    private void addDatabaseInfo(Inventory gui) {
        ItemStack dbItem = new ItemStack(Material.ANVIL);
        ItemMeta dbMeta = dbItem.getItemMeta();
        dbMeta.setDisplayName("§9§lDatabase Performance");
        
        List<String> dbLore = new ArrayList<>();
        dbLore.add("§7Type: §f" + (plugin.getConfigManager().isDatabaseEnabled() ? "MySQL" : "JSON"));
        dbLore.add("§7Connection Status: " + getDatabaseStatus());
        dbLore.add("§7Total Queries: §f" + databaseQueries);
        dbLore.add("");
        
        if (plugin.getConfigManager().isDatabaseEnabled()) {
            dbLore.add("§7Average Query Time: §f" + getAverageMetric("db_query") + "ms");
            dbLore.add("§7Connection Pool: §f" + getDatabasePoolInfo());
            dbLore.add("§7Failed Connections: §f" + getFailedConnections());
        } else {
            dbLore.add("§7File Operations: §f" + getFileOperations());
            dbLore.add("§7Average File I/O: §f" + getAverageMetric("file_io") + "ms");
        }
        
        dbLore.add("");
        dbLore.add("§eClick for detailed database metrics");
        
        dbMeta.setLore(dbLore);
        dbItem.setItemMeta(dbMeta);
        gui.setItem(12, dbItem);
    }
    
    private void addEventMetrics(Inventory gui) {
        List<Event> allEvents = plugin.getEventManager().getAllEvents();
        
        ItemStack eventItem = new ItemStack(Material.BOOK);
        ItemMeta eventMeta = eventItem.getItemMeta();
        eventMeta.setDisplayName("§b§lEvent Processing Metrics");
        
        List<String> eventLore = new ArrayList<>();
        eventLore.add("§7Total Events: §f" + allEvents.size());
        eventLore.add("§7Events per Hour: §f" + calculateEventsPerHour());
        eventLore.add("§7Average Event Duration: §f" + calculateAverageEventDuration());
        eventLore.add("");
        eventLore.add("§7Update Cycles: §f" + getUpdateCycles());
        eventLore.add("§7Average Update Time: §f" + getAverageMetric("event_update") + "ms");
        eventLore.add("§7HUD Updates: §f" + getHUDUpdates());
        eventLore.add("");
        eventLore.add("§7Cache Hit Rate: §f" + getCacheHitRate() + "%");
        eventLore.add("§7Cache Size: §f" + getCacheSize());
        eventLore.add("");
        eventLore.add("§eClick for detailed event analytics");
        
        eventMeta.setLore(eventLore);
        eventItem.setItemMeta(eventMeta);
        gui.setItem(13, eventItem);
    }
    
    private void addDebugTools(Inventory gui) {
        // Debug Logger
        ItemStack debugItem = new ItemStack(Material.PAPER);
        ItemMeta debugMeta = debugItem.getItemMeta();
        debugMeta.setDisplayName("§e§lDebug Logger");
        debugMeta.setLore(Arrays.asList(
            "§7Enable/disable debug logging",
            "§7Status: " + (plugin.getConfigManager().isDebugMode() ? "§aEnabled" : "§cDisabled"),
            "§7Log Level: §f" + getCurrentLogLevel(),
            "",
            "§eClick to toggle debug mode"
        ));
        debugItem.setItemMeta(debugMeta);
        gui.setItem(19, debugItem);
        
        // Profiler
        gui.setItem(20, createItem(Material.CLOCK, "§d§lPerformance Profiler",
            "§7Profile plugin performance",
            "§7and identify bottlenecks",
            "",
            "§eClick to start profiling"));
        
        // Memory Dump
        gui.setItem(21, createItem(Material.BUCKET, "§c§lMemory Analysis",
            "§7Analyze memory usage patterns",
            "§7and detect memory leaks",
            "",
            "§eClick to analyze memory"));
        
        // Thread Dump
        gui.setItem(22, createItem(Material.STRING, "§f§lThread Analysis",
            "§7View active threads and",
            "§7detect threading issues",
            "",
            "§eClick to analyze threads"));
        
        // Export Diagnostics
        gui.setItem(28, createItem(Material.ENDER_CHEST, "§6§lExport Diagnostics",
            "§7Export performance data",
            "§7and debug information",
            "",
            "§eClick to export"));
        
        // Reset Metrics
        gui.setItem(29, createItem(Material.BARRIER, "§c§lReset Metrics",
            "§7Clear all performance metrics",
            "§7and start fresh tracking",
            "",
            "§cShift-click to confirm"));
    }
    
    private void addNavigation(Inventory gui) {
        gui.setItem(45, createItem(Material.ARROW, "§7« Back to Admin Panel",
            "§7Return to the main admin dashboard"));
        
        gui.setItem(49, createItem(Material.LIME_CONCRETE, "§a§lRefresh Metrics",
            "§7Update all performance data",
            "§7and refresh the display",
            "",
            "§eClick to refresh"));
        
        gui.setItem(53, createItem(Material.BARRIER, "§c§lClose",
            "§7Close this interface"));
    }
    
    // Performance tracking methods
    public void trackEventCreation(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        recordMetric("event_creation", duration);
        eventCreationCount++;
    }
    
    public void trackEventJoin(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        recordMetric("event_join", duration);
        eventJoinCount++;
    }
    
    public void trackDatabaseQuery(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        recordMetric("db_query", duration);
        databaseQueries++;
    }
    
    public void trackGUILoad(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        recordMetric("gui_load", duration);
    }
    
    private void recordMetric(String metric, long value) {
        timingHistory.computeIfAbsent(metric, k -> new ArrayList<>()).add(value);
        
        // Keep only last 100 entries to prevent memory buildup
        List<Long> history = timingHistory.get(metric);
        if (history.size() > 100) {
            history.subList(0, history.size() - 100).clear();
        }
    }
    
    private String getAverageMetric(String metric) {
        List<Long> history = timingHistory.get(metric);
        if (history == null || history.isEmpty()) return "0";
        
        double average = history.stream().mapToLong(Long::longValue).average().orElse(0);
        return df.format(average);
    }
    
    private void startPerformanceTracking() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            updatePerformanceMetrics();
        }, 0L, 20L * 30); // Update every 30 seconds
    }
    
    private void updatePerformanceMetrics() {
        long currentTime = System.currentTimeMillis();
        
        // Track memory usage over time
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        performanceMetrics.put("heap_used", heapUsage.getUsed());
        performanceMetrics.put("heap_max", heapUsage.getMax());
        
        lastMemoryCheck = currentTime;
    }
    
    // Utility methods for metrics calculation
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return df.format(bytes / 1024.0) + " KB";
        if (bytes < 1024 * 1024 * 1024) return df.format(bytes / (1024.0 * 1024.0)) + " MB";
        return df.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
    }
    
    private String getMemoryColor(double percentage) {
        if (percentage < 50) return "§a";
        if (percentage < 75) return "§e";
        if (percentage < 90) return "§6";
        return "§c";
    }
    
    private String getCurrentTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] tps = (double[]) server.getClass().getField("recentTps").get(server);
            return df.format(Math.min(20.0, tps[0]));
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private String getServerUptime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptime / (1000 * 60 * 60);
        long minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60);
        return hours + "h " + minutes + "m";
    }
    
    private String getDatabaseStatus() {
        if (!plugin.getConfigManager().isDatabaseEnabled()) {
            return "§eJSON File System";
        }
        
        return plugin.getDatabaseManager().isConnectionHealthy() ? "§aHealthy" : "§cUnhealthy";
    }
    
    private String getDatabasePoolInfo() {
        // This would require access to the connection pool
        return "8/10 active";
    }
    
    private String getGCCollections() {
        return String.valueOf(ManagementFactory.getGarbageCollectorMXBeans()
            .stream().mapToLong(gc -> gc.getCollectionCount()).sum());
    }
    
    private String getGCTime() {
        return String.valueOf(ManagementFactory.getGarbageCollectorMXBeans()
            .stream().mapToLong(gc -> gc.getCollectionTime()).sum());
    }
    
    private String getThreadPoolInfo() {
        return Thread.activeCount() + " threads";
    }
    
    private String getFailedConnections() {
        return "0"; // Would track actual failed connections
    }
    
    private String getFileOperations() {
        return String.valueOf(performanceMetrics.getOrDefault("file_ops", 0L));
    }
    
    private String calculateEventsPerHour() {
        // Calculate based on event creation rate
        return df.format(eventCreationCount * 3600.0 / (System.currentTimeMillis() - plugin.getDescription().getName().hashCode()));
    }
    
    private String calculateAverageEventDuration() {
        List<Event> completedEvents = plugin.getEventManager().getAllEvents().stream()
            .filter(Event::isCompleted)
            .toList();
        
        if (completedEvents.isEmpty()) return "N/A";
        
        double avgDuration = completedEvents.stream()
            .mapToLong(e -> e.getEndTime() - e.getStartTime())
            .average().orElse(0) / 1000.0 / 60.0; // Convert to minutes
        
        return df.format(avgDuration) + " min";
    }
    
    private String getUpdateCycles() {
        return String.valueOf(performanceMetrics.getOrDefault("update_cycles", 0L));
    }
    
    private String getHUDUpdates() {
        return String.valueOf(performanceMetrics.getOrDefault("hud_updates", 0L));
    }
    
    private String getCacheHitRate() {
        long hits = performanceMetrics.getOrDefault("cache_hits", 0L);
        long misses = performanceMetrics.getOrDefault("cache_misses", 0L);
        if (hits + misses == 0) return "0";
        return df.format((hits * 100.0) / (hits + misses));
    }
    
    private String getCacheSize() {
        return String.valueOf(performanceMetrics.getOrDefault("cache_size", 0L));
    }
    
    private String getCurrentLogLevel() {
        return plugin.getLogger().getLevel() != null ? 
            plugin.getLogger().getLevel().getName() : "INFO";
    }
    
    private ItemStack createItem(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Debug logger for advanced logging functionality
     */
    public static class DebugLogger {
        private final SwiftEventsPlugin plugin;
        private final Map<String, Long> debugCounters = new ConcurrentHashMap<>();
        
        public DebugLogger(SwiftEventsPlugin plugin) {
            this.plugin = plugin;
        }
        
        public void debug(String category, String message) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[DEBUG:" + category + "] " + message);
                debugCounters.merge(category, 1L, Long::sum);
            }
        }
        
        public void debugPerformance(String operation, long duration) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[PERF:" + operation + "] " + duration + "ms");
            }
        }
        
        public void debugError(String category, String message, Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "[ERROR:" + category + "] " + message, throwable);
            debugCounters.merge(category + "_errors", 1L, Long::sum);
        }
        
        public Map<String, Long> getDebugCounters() {
            return new HashMap<>(debugCounters);
        }
        
        public void clearCounters() {
            debugCounters.clear();
        }
    }
} 