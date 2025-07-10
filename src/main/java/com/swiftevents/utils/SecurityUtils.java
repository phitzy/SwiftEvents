package com.swiftevents.utils;

import com.swiftevents.SwiftEventsPlugin;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Security utility class for SwiftEvents plugin
 * Provides input validation, sanitization, and security checks
 */
public class SecurityUtils {
    
    private final SwiftEventsPlugin plugin;
    private final LoggingUtils loggingUtils;
    
    // Security patterns
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)(script|javascript|vbscript|data:)");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)");
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile("(?i)(cmd|command|system|runtime|process)");
    private static final Pattern XSS_PATTERN = Pattern.compile("(?i)(onload|onerror|onclick|onmouseover|onfocus|onblur)");
    
    // Input length limits
    private static final int MAX_EVENT_NAME_LENGTH = 255;
    private static final int MAX_EVENT_DESCRIPTION_LENGTH = 10000;
    private static final int MAX_PLAYER_NAME_LENGTH = 16;
    private static final int MAX_COMMAND_LENGTH = 1000;
    
    public SecurityUtils(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.loggingUtils = new LoggingUtils(plugin);
    }
    
    /**
     * Validate and sanitize event name
     */
    public String validateEventName(String name) throws SecurityException {
        if (name == null || name.trim().isEmpty()) {
            throw new SecurityException("Event name cannot be null or empty");
        }
        
        if (name.length() > MAX_EVENT_NAME_LENGTH) {
            throw new SecurityException("Event name cannot exceed " + MAX_EVENT_NAME_LENGTH + " characters");
        }
        
        String sanitized = sanitizeString(name);
        if (sanitized == null || sanitized.trim().isEmpty()) {
            throw new SecurityException("Event name contains invalid characters");
        }
        
        if (containsInjectionPattern(sanitized)) {
            loggingUtils.securityViolation("event_name_injection", "Name: " + name);
            throw new SecurityException("Event name contains potentially dangerous content");
        }
        
        return sanitized.trim();
    }
    
    /**
     * Validate and sanitize event description
     */
    public String validateEventDescription(String description) throws SecurityException {
        if (description == null) {
            return null;
        }
        
        if (description.length() > MAX_EVENT_DESCRIPTION_LENGTH) {
            throw new SecurityException("Event description cannot exceed " + MAX_EVENT_DESCRIPTION_LENGTH + " characters");
        }
        
        String sanitized = sanitizeString(description);
        if (sanitized != null && containsInjectionPattern(sanitized)) {
            loggingUtils.securityViolation("event_description_injection", "Description: " + description);
            throw new SecurityException("Event description contains potentially dangerous content");
        }
        
        return sanitized;
    }
    
    /**
     * Validate player UUID
     */
    public UUID validatePlayerUUID(String uuidString) throws SecurityException {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            throw new SecurityException("Player UUID cannot be null or empty");
        }
        
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            loggingUtils.securityViolation("invalid_uuid", "UUID: " + uuidString);
            throw new SecurityException("Invalid UUID format");
        }
    }
    
    /**
     * Validate player name
     */
    public String validatePlayerName(String playerName) throws SecurityException {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new SecurityException("Player name cannot be null or empty");
        }
        
        if (playerName.length() > MAX_PLAYER_NAME_LENGTH) {
            throw new SecurityException("Player name cannot exceed " + MAX_PLAYER_NAME_LENGTH + " characters");
        }
        
        String sanitized = sanitizeString(playerName);
        if (sanitized == null || sanitized.trim().isEmpty()) {
            throw new SecurityException("Player name contains invalid characters");
        }
        
        if (containsInjectionPattern(sanitized)) {
            loggingUtils.securityViolation("player_name_injection", "Name: " + playerName);
            throw new SecurityException("Player name contains potentially dangerous content");
        }
        
        return sanitized.trim();
    }
    
    /**
     * Validate command input
     */
    public String validateCommand(String command) throws SecurityException {
        if (command == null || command.trim().isEmpty()) {
            throw new SecurityException("Command cannot be null or empty");
        }
        
        if (command.length() > MAX_COMMAND_LENGTH) {
            throw new SecurityException("Command cannot exceed " + MAX_COMMAND_LENGTH + " characters");
        }
        
        String sanitized = sanitizeString(command);
        if (sanitized == null || sanitized.trim().isEmpty()) {
            throw new SecurityException("Command contains invalid characters");
        }
        
        if (containsCommandInjectionPattern(sanitized)) {
            loggingUtils.securityViolation("command_injection", "Command: " + command);
            throw new SecurityException("Command contains potentially dangerous content");
        }
        
        return sanitized.trim();
    }
    
    /**
     * Validate event ID
     */
    public String validateEventId(String eventId) throws SecurityException {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new SecurityException("Event ID cannot be null or empty");
        }
        
        // Event IDs should be UUIDs
        try {
            UUID.fromString(eventId);
        } catch (IllegalArgumentException e) {
            loggingUtils.securityViolation("invalid_event_id", "Event ID: " + eventId);
            throw new SecurityException("Invalid event ID format");
        }
        
        return eventId.trim();
    }
    
    /**
     * Validate location coordinates
     */
    public void validateLocation(double x, double y, double z) throws SecurityException {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new SecurityException("Invalid X coordinate");
        }
        
        if (Double.isNaN(y) || Double.isInfinite(y)) {
            throw new SecurityException("Invalid Y coordinate");
        }
        
        if (Double.isNaN(z) || Double.isInfinite(z)) {
            throw new SecurityException("Invalid Z coordinate");
        }
        
        // Check for reasonable coordinate ranges
        if (Math.abs(x) > 30000000 || Math.abs(y) > 30000000 || Math.abs(z) > 30000000) {
            loggingUtils.securityViolation("suspicious_coordinates", "X: " + x + ", Y: " + y + ", Z: " + z);
            throw new SecurityException("Coordinates are outside reasonable range");
        }
    }
    
    /**
     * Validate world name
     */
    public String validateWorldName(String worldName) throws SecurityException {
        if (worldName == null || worldName.trim().isEmpty()) {
            throw new SecurityException("World name cannot be null or empty");
        }
        
        String sanitized = sanitizeString(worldName);
        if (sanitized == null || sanitized.trim().isEmpty()) {
            throw new SecurityException("World name contains invalid characters");
        }
        
        if (containsInjectionPattern(sanitized)) {
            loggingUtils.securityViolation("world_name_injection", "World: " + worldName);
            throw new SecurityException("World name contains potentially dangerous content");
        }
        
        return sanitized.trim();
    }
    
    /**
     * Validate integer parameter
     */
    public int validateInteger(String paramName, String value, int min, int max) throws SecurityException {
        if (value == null || value.trim().isEmpty()) {
            throw new SecurityException(paramName + " cannot be null or empty");
        }
        
        try {
            int intValue = Integer.parseInt(value.trim());
            if (intValue < min || intValue > max) {
                throw new SecurityException(paramName + " must be between " + min + " and " + max);
            }
            return intValue;
        } catch (NumberFormatException e) {
            loggingUtils.securityViolation("invalid_integer", paramName + ": " + value);
            throw new SecurityException(paramName + " must be a valid integer");
        }
    }
    
    /**
     * Validate long parameter
     */
    public long validateLong(String paramName, String value, long min, long max) throws SecurityException {
        if (value == null || value.trim().isEmpty()) {
            throw new SecurityException(paramName + " cannot be null or empty");
        }
        
        try {
            long longValue = Long.parseLong(value.trim());
            if (longValue < min || longValue > max) {
                throw new SecurityException(paramName + " must be between " + min + " and " + max);
            }
            return longValue;
        } catch (NumberFormatException e) {
            loggingUtils.securityViolation("invalid_long", paramName + ": " + value);
            throw new SecurityException(paramName + " must be a valid number");
        }
    }
    
    /**
     * Validate double parameter
     */
    public double validateDouble(String paramName, String value, double min, double max) throws SecurityException {
        if (value == null || value.trim().isEmpty()) {
            throw new SecurityException(paramName + " cannot be null or empty");
        }
        
        try {
            double doubleValue = Double.parseDouble(value.trim());
            if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                throw new SecurityException(paramName + " must be a valid number");
            }
            if (doubleValue < min || doubleValue > max) {
                throw new SecurityException(paramName + " must be between " + min + " and " + max);
            }
            return doubleValue;
        } catch (NumberFormatException e) {
            loggingUtils.securityViolation("invalid_double", paramName + ": " + value);
            throw new SecurityException(paramName + " must be a valid number");
        }
    }
    
    /**
     * Check if player has permission for action
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null) {
            return false;
        }
        
        boolean hasPermission = player.hasPermission(permission);
        loggingUtils.permissionCheck(player.getName(), permission, hasPermission);
        return hasPermission;
    }
    
    /**
     * Check if player can perform action on event
     */
    public boolean canModifyEvent(Player player, String eventId) {
        if (player == null || eventId == null) {
            return false;
        }
        
        // Check if player is event creator
        // This would need to be implemented based on your event ownership logic
        return hasPermission(player, "swiftevents.admin") || hasPermission(player, "swiftevents.modify");
    }
    
    /**
     * Sanitize string input
     */
    private String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove HTML tags
        String sanitized = HTML_TAG_PATTERN.matcher(input).replaceAll("");
        
        // Remove potentially dangerous patterns
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = XSS_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove control characters
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        return sanitized.trim();
    }
    
    /**
     * Check for injection patterns
     */
    private boolean containsInjectionPattern(String input) {
        if (input == null) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        return SCRIPT_PATTERN.matcher(lowerInput).find() ||
               XSS_PATTERN.matcher(lowerInput).find() ||
               lowerInput.contains("eval(") ||
               lowerInput.contains("document.") ||
               lowerInput.contains("window.") ||
               lowerInput.contains("alert(") ||
               lowerInput.contains("confirm(") ||
               lowerInput.contains("prompt(") ||
               lowerInput.contains("localStorage.") ||
               lowerInput.contains("sessionStorage.");
    }
    
    /**
     * Check for command injection patterns
     */
    private boolean containsCommandInjectionPattern(String input) {
        if (input == null) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        return COMMAND_INJECTION_PATTERN.matcher(lowerInput).find() ||
               SQL_INJECTION_PATTERN.matcher(lowerInput).find() ||
               lowerInput.contains(";") ||
               lowerInput.contains("&&") ||
               lowerInput.contains("||") ||
               lowerInput.contains("|") ||
               lowerInput.contains("`") ||
               lowerInput.contains("$(");
    }
    
    /**
     * Validate file path for security
     */
    public String validateFilePath(String path) throws SecurityException {
        if (path == null || path.trim().isEmpty()) {
            throw new SecurityException("File path cannot be null or empty");
        }
        
        // Check for path traversal attempts
        if (path.contains("..") || path.contains("//") || path.startsWith("/")) {
            loggingUtils.securityViolation("path_traversal", "Path: " + path);
            throw new SecurityException("Invalid file path");
        }
        
        // Check for dangerous file extensions
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".exe") || lowerPath.endsWith(".bat") || lowerPath.endsWith(".cmd") ||
            lowerPath.endsWith(".sh") || lowerPath.endsWith(".jar") || lowerPath.endsWith(".class")) {
            loggingUtils.securityViolation("dangerous_file_extension", "Path: " + path);
            throw new SecurityException("File type not allowed");
        }
        
        return path.trim();
    }
    
    /**
     * Validate URL for security
     */
    public String validateUrl(String url) throws SecurityException {
        if (url == null || url.trim().isEmpty()) {
            throw new SecurityException("URL cannot be null or empty");
        }
        
        String lowerUrl = url.toLowerCase();
        
        // Check for dangerous protocols
        if (lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("data:") || 
            lowerUrl.startsWith("vbscript:") || lowerUrl.startsWith("file:")) {
            loggingUtils.securityViolation("dangerous_url_protocol", "URL: " + url);
            throw new SecurityException("URL protocol not allowed");
        }
        
        // Only allow HTTP and HTTPS
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            throw new SecurityException("URL must use HTTP or HTTPS protocol");
        }
        
        return url.trim();
    }
    
    /**
     * Rate limiting check
     */
    public boolean checkRateLimit(String playerId, String action, int maxAttempts, long timeWindowMs) {
        // This would need to be implemented with a proper rate limiting mechanism
        // For now, we'll just return true
        return true;
    }
    
    /**
     * Log security event
     */
    public void logSecurityEvent(String event, String details) {
        loggingUtils.securityViolation(event, details);
    }
    
    /**
     * Validate JSON string for security
     */
    public String validateJsonString(String json) throws SecurityException {
        if (json == null) {
            return null;
        }
        
        if (json.length() > 1000000) { // 1MB limit
            throw new SecurityException("JSON string too large");
        }
        
        // Check for potential JSON injection
        if (json.contains("__proto__") || json.contains("constructor") || json.contains("prototype")) {
            loggingUtils.securityViolation("json_injection", "JSON: " + json);
            throw new SecurityException("JSON contains potentially dangerous content");
        }
        
        return json;
    }
    
    /**
     * Validate map for security
     */
    public void validateMap(java.util.Map<String, Object> map) throws SecurityException {
        if (map == null) {
            return;
        }
        
        if (map.size() > 1000) {
            throw new SecurityException("Map too large");
        }
        
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key != null && containsInjectionPattern(key)) {
                loggingUtils.securityViolation("map_key_injection", "Key: " + key);
                throw new SecurityException("Map key contains potentially dangerous content");
            }
            
            if (value instanceof String) {
                String stringValue = (String) value;
                if (containsInjectionPattern(stringValue)) {
                    loggingUtils.securityViolation("map_value_injection", "Value: " + stringValue);
                    throw new SecurityException("Map value contains potentially dangerous content");
                }
            }
        }
    }
} 