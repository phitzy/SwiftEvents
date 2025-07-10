package com.swiftevents.utils;

import com.swiftevents.SwiftEventsPlugin;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive logging utility for SwiftEvents plugin
 * Provides structured logging, performance tracking, and security monitoring
 */
public class LoggingUtils {
    
    private final SwiftEventsPlugin plugin;
    private final Logger logger;
    
    // Performance tracking
    private final ConcurrentHashMap<String, AtomicLong> operationCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> operationTimers = new ConcurrentHashMap<>();
    
    // Security monitoring
    private final ConcurrentHashMap<String, AtomicLong> securityViolations = new ConcurrentHashMap<>();
    
    // Log levels for different components
    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR, SECURITY
    }
    
    public LoggingUtils(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Log a debug message
     */
    public void debug(String message) {
        if (plugin.getConfigManager().isDebugMode()) {
            logger.log(Level.FINE, "[DEBUG] " + message);
        }
    }
    
    /**
     * Log an info message
     */
    public void info(String message) {
        logger.log(Level.INFO, "[INFO] " + message);
    }
    
    /**
     * Log a warning message
     */
    public void warning(String message) {
        logger.log(Level.WARNING, "[WARNING] " + message);
    }
    
    /**
     * Log an error message
     */
    public void error(String message) {
        logger.log(Level.SEVERE, "[ERROR] " + message);
    }
    
    /**
     * Log an error message with exception
     */
    public void error(String message, Throwable exception) {
        logger.log(Level.SEVERE, "[ERROR] " + message, exception);
    }
    
    /**
     * Log a security-related message
     */
    public void security(String message) {
        logger.log(Level.WARNING, "[SECURITY] " + message);
        incrementSecurityViolation("general");
    }
    
    /**
     * Log a security violation with specific type
     */
    public void securityViolation(String type, String details) {
        logger.log(Level.WARNING, "[SECURITY_VIOLATION] Type: " + type + " - Details: " + details);
        incrementSecurityViolation(type);
    }
    
    /**
     * Log player action for audit trail
     */
    public void playerAction(Player player, String action, String details) {
        String playerName = player != null ? player.getName() : "Unknown";
        String playerId = player != null ? player.getUniqueId().toString() : "Unknown";
        
        logger.log(Level.INFO, "[PLAYER_ACTION] Player: " + playerName + " (" + playerId + ") - Action: " + action + " - Details: " + details);
    }
    
    /**
     * Log event creation
     */
    public void eventCreated(String eventId, String eventName, String creatorName) {
        logger.log(Level.INFO, "[EVENT_CREATED] ID: " + eventId + " - Name: " + eventName + " - Creator: " + creatorName);
        incrementOperationCounter("event_created");
    }
    
    /**
     * Log event deletion
     */
    public void eventDeleted(String eventId, String eventName, String reason) {
        logger.log(Level.INFO, "[EVENT_DELETED] ID: " + eventId + " - Name: " + eventName + " - Reason: " + reason);
        incrementOperationCounter("event_deleted");
    }
    
    /**
     * Log player join event
     */
    public void playerJoinedEvent(String playerName, String playerId, String eventId, String eventName) {
        logger.log(Level.INFO, "[PLAYER_JOINED] Player: " + playerName + " (" + playerId + ") - Event: " + eventName + " (" + eventId + ")");
        incrementOperationCounter("player_joined");
    }
    
    /**
     * Log player leave event
     */
    public void playerLeftEvent(String playerName, String playerId, String eventId, String eventName, String reason) {
        logger.log(Level.INFO, "[PLAYER_LEFT] Player: " + playerName + " (" + playerId + ") - Event: " + eventName + " (" + eventId + ") - Reason: " + reason);
        incrementOperationCounter("player_left");
    }
    
    /**
     * Log database operation
     */
    public void databaseOperation(String operation, String details, long durationMs) {
        logger.log(Level.FINE, "[DATABASE] Operation: " + operation + " - Details: " + details + " - Duration: " + durationMs + "ms");
        incrementOperationCounter("database_" + operation);
        recordOperationTime("database_" + operation, durationMs);
    }
    
    /**
     * Log database error
     */
    public void databaseError(String operation, String details, Throwable exception) {
        logger.log(Level.SEVERE, "[DATABASE_ERROR] Operation: " + operation + " - Details: " + details, exception);
        incrementOperationCounter("database_error");
    }
    
    /**
     * Log performance metric
     */
    public void performanceMetric(String metric, long value, String unit) {
        logger.log(Level.FINE, "[PERFORMANCE] " + metric + ": " + value + " " + unit);
    }
    
    /**
     * Log memory usage
     */
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        performanceMetric("Memory Usage", usedMemory / 1024 / 1024, "MB");
        performanceMetric("Total Memory", totalMemory / 1024 / 1024, "MB");
        performanceMetric("Max Memory", maxMemory / 1024 / 1024, "MB");
        performanceMetric("Memory Usage %", (usedMemory * 100) / maxMemory, "%");
    }
    
    /**
     * Log thread count
     */
    public void logThreadCount() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        
        int threadCount = rootGroup.activeCount();
        performanceMetric("Active Threads", threadCount, "threads");
    }
    
    /**
     * Log operation statistics
     */
    public void logOperationStatistics() {
        logger.log(Level.INFO, "[STATISTICS] Operation Counters:");
        operationCounters.forEach((operation, counter) -> {
            logger.log(Level.INFO, "  " + operation + ": " + counter.get());
        });
        
        logger.log(Level.INFO, "[STATISTICS] Security Violations:");
        securityViolations.forEach((type, counter) -> {
            logger.log(Level.INFO, "  " + type + ": " + counter.get());
        });
    }
    
    /**
     * Track operation time
     */
    public void recordOperationTime(String operation, long durationMs) {
        operationTimers.computeIfAbsent(operation, k -> new AtomicLong(0))
                      .addAndGet(durationMs);
    }
    
    /**
     * Get average operation time
     */
    public long getAverageOperationTime(String operation) {
        AtomicLong totalTime = operationTimers.get(operation);
        AtomicLong count = operationCounters.get(operation);
        
        if (totalTime != null && count != null && count.get() > 0) {
            return totalTime.get() / count.get();
        }
        return 0;
    }
    
    /**
     * Increment operation counter
     */
    private void incrementOperationCounter(String operation) {
        operationCounters.computeIfAbsent(operation, k -> new AtomicLong(0))
                        .incrementAndGet();
    }
    
    /**
     * Increment security violation counter
     */
    private void incrementSecurityViolation(String type) {
        securityViolations.computeIfAbsent(type, k -> new AtomicLong(0))
                         .incrementAndGet();
    }
    
    /**
     * Log configuration validation errors
     */
    public void configValidationError(String key, String expected, String actual) {
        logger.log(Level.WARNING, "[CONFIG_VALIDATION] Key: " + key + " - Expected: " + expected + " - Actual: " + actual);
    }
    
    /**
     * Log plugin lifecycle events
     */
    public void pluginLifecycle(String event, String details) {
        logger.log(Level.INFO, "[LIFECYCLE] Event: " + event + " - Details: " + details);
    }
    
    /**
     * Log hook execution
     */
    public void hookExecution(String hookName, String event, boolean success, long durationMs) {
        Level level = success ? Level.FINE : Level.WARNING;
        String status = success ? "SUCCESS" : "FAILED";
        logger.log(level, "[HOOK] " + hookName + " - Event: " + event + " - Status: " + status + " - Duration: " + durationMs + "ms");
    }
    
    /**
     * Log API usage
     */
    public void apiUsage(String apiName, String method, String caller) {
        logger.log(Level.FINE, "[API] " + apiName + "." + method + " - Caller: " + caller);
        incrementOperationCounter("api_" + apiName + "_" + method);
    }
    
    /**
     * Log permission check
     */
    public void permissionCheck(String playerName, String permission, boolean granted) {
        Level level = granted ? Level.FINE : Level.WARNING;
        String status = granted ? "GRANTED" : "DENIED";
        logger.log(level, "[PERMISSION] Player: " + playerName + " - Permission: " + permission + " - Status: " + status);
        
        if (!granted) {
            incrementSecurityViolation("permission_denied");
        }
    }
    
    /**
     * Log command execution
     */
    public void commandExecution(String playerName, String command, String[] args) {
        String argsString = args != null ? String.join(" ", args) : "";
        logger.log(Level.INFO, "[COMMAND] Player: " + playerName + " - Command: " + command + " - Args: " + argsString);
        incrementOperationCounter("command_" + command);
    }
    
    /**
     * Log teleport event
     */
    public void teleportEvent(String playerName, String fromLocation, String toLocation) {
        logger.log(Level.FINE, "[TELEPORT] Player: " + playerName + " - From: " + fromLocation + " - To: " + toLocation);
        incrementOperationCounter("teleport");
    }
    
    /**
     * Log reward distribution
     */
    public void rewardDistribution(String playerName, String eventName, String reward) {
        logger.log(Level.INFO, "[REWARD] Player: " + playerName + " - Event: " + eventName + " - Reward: " + reward);
        incrementOperationCounter("reward_distributed");
    }
    
    /**
     * Log HUD update
     */
    public void hudUpdate(String eventId, int playerCount) {
        logger.log(Level.FINE, "[HUD] Event: " + eventId + " - Players: " + playerCount);
        incrementOperationCounter("hud_update");
    }
    
    /**
     * Log chat message
     */
    public void chatMessage(String playerName, String message, String channel) {
        logger.log(Level.FINE, "[CHAT] Player: " + playerName + " - Channel: " + channel + " - Message: " + message);
        incrementOperationCounter("chat_message");
    }
    
    /**
     * Log GUI interaction
     */
    public void guiInteraction(String playerName, String guiName, String action) {
        logger.log(Level.FINE, "[GUI] Player: " + playerName + " - GUI: " + guiName + " - Action: " + action);
        incrementOperationCounter("gui_" + action);
    }
    
    /**
     * Log backup operation
     */
    public void backupOperation(String type, String details, boolean success) {
        Level level = success ? Level.INFO : Level.SEVERE;
        String status = success ? "SUCCESS" : "FAILED";
        logger.log(level, "[BACKUP] Type: " + type + " - Details: " + details + " - Status: " + status);
        incrementOperationCounter("backup_" + type);
    }
    
    /**
     * Log data corruption detection
     */
    public void dataCorruption(String dataType, String details) {
        logger.log(Level.SEVERE, "[DATA_CORRUPTION] Type: " + dataType + " - Details: " + details);
        incrementSecurityViolation("data_corruption");
    }
    
    /**
     * Log resource usage
     */
    public void resourceUsage(String resource, long usage, String unit) {
        logger.log(Level.FINE, "[RESOURCE] " + resource + ": " + usage + " " + unit);
    }
    
    /**
     * Clear all counters (useful for testing)
     */
    public void clearCounters() {
        operationCounters.clear();
        operationTimers.clear();
        securityViolations.clear();
    }
    
    /**
     * Get operation counter value
     */
    public long getOperationCounter(String operation) {
        AtomicLong counter = operationCounters.get(operation);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Get security violation counter value
     */
    public long getSecurityViolationCounter(String type) {
        AtomicLong counter = securityViolations.get(type);
        return counter != null ? counter.get() : 0;
    }
} 