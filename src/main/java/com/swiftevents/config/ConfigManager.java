package com.swiftevents.config;

import com.swiftevents.SwiftEventsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

public class ConfigManager {
    
    private final SwiftEventsPlugin plugin;
    private FileConfiguration config;
    
    // Optimization: Efficient cache with primitive values where possible
    private final Map<String, Object> configCache = new ConcurrentHashMap<>(128, 0.75f);
    
    // Pre-cached primitive values for performance (avoiding boxing/unboxing)
    private boolean databaseEnabled;
    private String databaseHost;
    private int databasePort;
    private String databaseName;
    private String databaseUsername;
    private String databasePassword;
    private int databaseConnectionTimeout;
    private int maxDatabaseConnections;
    private boolean hudEnabled;
    private String hudPosition;
    private boolean hudAnimationsEnabled;
    private int maxConcurrentEvents;
    private int autoSaveInterval;
    private int playerCooldown;
    private int maxEventsPerPlayer;
    private boolean eventTaskerEnabled;
    private String messagePrefix;
    private boolean debugMode;
    private boolean metricsEnabled;
    private boolean backupOnShutdown;
    private String defaultLanguage;
    private boolean perPlayerLanguage;
    private boolean guiEnabled;
    private String guiTitle;
    private boolean chatEnabled;
    
    // Optimization: Use ArrayList with known approximate size for validation errors
    private final List<String> validationErrors = new ArrayList<>(16);
    
    // Optimization: Pre-computed common strings to avoid repeated concatenation
    private static final String[] VALID_HUD_POSITIONS = {"ACTION_BAR", "BOSS_BAR", "TITLE"};
    private static final String DATABASE_PREFIX = "database.";
    private static final String EVENTS_PREFIX = "events.";
    private static final String HUD_PREFIX = "hud.";
    
    public ConfigManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Set default values if not present
        setDefaults();
        
        // Validate configuration
        if (validateConfig()) {
            plugin.saveConfig();
            // Cache frequently accessed values
            cacheCommonValues();
            plugin.getLogger().info("Configuration loaded and validated successfully!");
        } else {
            plugin.getLogger().severe("Configuration validation failed with errors:");
            for (String error : validationErrors) {
                plugin.getLogger().severe("  - " + error);
            }
            plugin.getLogger().info("Using default values for invalid configurations...");
            cacheCommonValues(); // Still cache what we can
        }
    }
    
    public boolean validateConfig() {
        validationErrors.clear();
        
        // Validate database settings
        if (config.getBoolean("database.enabled", false)) {
            validateDatabaseConfig();
        }
        
        // Validate event settings
        validateEventConfig();
        
        // Validate HUD settings
        validateHUDConfig();
        
        // Validate permission groups
        validatePermissionGroups();
        
        // Validate localization settings
        validateLocalizationConfig();
        
        // Validate advanced settings
        validateAdvancedConfig();
        
        return validationErrors.isEmpty();
    }
    
    private void validateDatabaseConfig() {
        int timeout = config.getInt(DATABASE_PREFIX + "connection_timeout", 30);
        if (timeout < 5 || timeout > 300) {
            validationErrors.add("database.connection_timeout must be between 5 and 300 seconds");
        }
        
        int maxConnections = config.getInt(DATABASE_PREFIX + "max_connections", 10);
        if (maxConnections < 1 || maxConnections > 50) {
            validationErrors.add("database.max_connections must be between 1 and 50");
        }
        
        String host = config.getString(DATABASE_PREFIX + "host", "localhost");
        if (host == null || host.trim().isEmpty()) {
            validationErrors.add("database.host cannot be empty");
        }
    }
    
    private void validateEventConfig() {
        int maxConcurrent = config.getInt(EVENTS_PREFIX + "max_concurrent", 5);
        if (maxConcurrent < 1 || maxConcurrent > 100) {
            validationErrors.add("events.max_concurrent must be between 1 and 100");
        }
        
        int playerCooldown = config.getInt(EVENTS_PREFIX + "player_cooldown", 300);
        if (playerCooldown < 0) {
            validationErrors.add("events.player_cooldown cannot be negative");
        }
        
        int maxEventsPerPlayer = config.getInt(EVENTS_PREFIX + "max_events_per_player", 3);
        if (maxEventsPerPlayer < 1 || maxEventsPerPlayer > 50) {
            validationErrors.add("events.max_events_per_player must be between 1 and 50");
        }
    }
    
    private void validateHUDConfig() {
        String hudPosition = config.getString(HUD_PREFIX + "position", "ACTION_BAR");
        boolean validPosition = false;
        for (String validPos : VALID_HUD_POSITIONS) {
            if (validPos.equals(hudPosition)) {
                validPosition = true;
                break;
            }
        }
        if (!validPosition) {
            validationErrors.add("hud.position must be one of: " + String.join(", ", VALID_HUD_POSITIONS));
        }
        
        int duration = config.getInt(HUD_PREFIX + "notification_duration", 5);
        if (duration < 1 || duration > 60) {
            validationErrors.add("hud.notification_duration must be between 1 and 60 seconds");
        }
    }
    
    private void validatePermissionGroups() {
        ConfigurationSection limitsSection = config.getConfigurationSection("permission_groups.limits");
        if (limitsSection != null) {
            for (String group : limitsSection.getKeys(false)) {
                ConfigurationSection groupSection = limitsSection.getConfigurationSection(group);
                if (groupSection != null) {
                    int maxEvents = groupSection.getInt("max_events_per_player", 2);
                    if (maxEvents < -1) {
                        validationErrors.add("permission_groups.limits." + group + ".max_events_per_player cannot be less than -1");
                    }
                    
                    int cooldown = groupSection.getInt("event_cooldown", 300);
                    if (cooldown < 0) {
                        validationErrors.add("permission_groups.limits." + group + ".event_cooldown cannot be negative");
                    }
                }
            }
        }
    }
    
    private void validateLocalizationConfig() {
        String defaultLang = config.getString("localization.default_language", "en");
        if (defaultLang == null || defaultLang.trim().isEmpty()) {
            validationErrors.add("localization.default_language cannot be empty");
        }
        
        ConfigurationSection languagesSection = config.getConfigurationSection("localization.languages");
        if (languagesSection != null && !languagesSection.contains(defaultLang)) {
            validationErrors.add("localization.default_language '" + defaultLang + "' is not defined in available languages");
        }
    }
    
    private void validateAdvancedConfig() {
        ConfigurationSection perfSection = config.getConfigurationSection("advanced.performance");
        if (perfSection != null) {
            int cacheDuration = perfSection.getInt("cache_duration", 300);
            if (cacheDuration < 60 || cacheDuration > 3600) {
                validationErrors.add("advanced.performance.cache_duration must be between 60 and 3600 seconds");
            }
            
            int batchSize = perfSection.getInt("batch_size", 50);
            if (batchSize < 1 || batchSize > 1000) {
                validationErrors.add("advanced.performance.batch_size must be between 1 and 1000");
            }
        }
    }
    
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    private void setDefaults() {
        // Database defaults
        setDefaultIfMissing("database.enabled", false);
        setDefaultIfMissing("database.type", "mysql");
        setDefaultIfMissing("database.host", "localhost");
        setDefaultIfMissing("database.port", 3306);
        setDefaultIfMissing("database.name", "swiftevents");
        setDefaultIfMissing("database.username", "root");
        setDefaultIfMissing("database.password", "password");
        setDefaultIfMissing("database.connection_timeout", 30);
        setDefaultIfMissing("database.max_connections", 10);
        
        // JSON storage defaults
        setDefaultIfMissing("json.folder", "events");
        setDefaultIfMissing("json.auto_backup", true);
        setDefaultIfMissing("json.backup_interval", 3600);
        setDefaultIfMissing("json.max_backups", 5);
        
        // GUI defaults
        setDefaultIfMissing("gui.enabled", true);
        setDefaultIfMissing("gui.title", "SwiftEvents");
        setDefaultIfMissing("gui.size", 54);
        setDefaultIfMissing("gui.update_interval", 30);
        setDefaultIfMissing("gui.animations", true);
        setDefaultIfMissing("gui.auto_refresh", true);
        
        // HUD defaults
        setDefaultIfMissing("hud.enabled", true);
        setDefaultIfMissing("hud.position", "ACTION_BAR");
        setDefaultIfMissing("hud.notification_duration", 5);
        setDefaultIfMissing("hud.animations", true);
        
        // Chat defaults
        setDefaultIfMissing("chat.enabled", true);
        setDefaultIfMissing("chat.sound_effects", true);
        setDefaultIfMissing("chat.announce_to_all", true);
        setDefaultIfMissing("chat.reminder_time", 300);
        setDefaultIfMissing("chat.reminders_enabled", true);
        setDefaultIfMissing("chat.interactive_messages", true);
        setDefaultIfMissing("chat.hover_tooltips", true);
        
        // Event defaults
        setDefaultIfMissing("events.max_concurrent", 5);
        setDefaultIfMissing("events.auto_save_interval", 300);
        setDefaultIfMissing("events.player_cooldown", 300);
        setDefaultIfMissing("events.max_events_per_player", 3);
        setDefaultIfMissing("events.statistics_enabled", true);
        setDefaultIfMissing("events.auto_cancel_empty_after", 600);
        
        // Event Tasker defaults
        setDefaultIfMissing("event_tasker.enabled", false);
        setDefaultIfMissing("event_tasker.check_interval", 60);
        setDefaultIfMissing("event_tasker.min_event_interval", 1800);
        setDefaultIfMissing("event_tasker.max_event_interval", 7200);
        setDefaultIfMissing("event_tasker.announce_upcoming", true);
        setDefaultIfMissing("event_tasker.announce_time", 300);
        
        // Localization defaults
        setDefaultIfMissing("localization.default_language", "en");
        setDefaultIfMissing("localization.per_player_language", false);
        
        // Debug and metrics
        setDefaultIfMissing("debug_mode", false);
        setDefaultIfMissing("metrics.enabled", true);
        setDefaultIfMissing("backup_on_shutdown", true);
        
        // Advanced performance settings
        setDefaultIfMissing("advanced.performance.caching_enabled", true);
        setDefaultIfMissing("advanced.performance.cache_duration", 300);
        setDefaultIfMissing("advanced.performance.async_operations", true);
        setDefaultIfMissing("advanced.performance.batch_operations", true);
        setDefaultIfMissing("advanced.performance.batch_size", 50);
        
        // Integration settings
        setDefaultIfMissing("integrations.placeholder_api", true);
        setDefaultIfMissing("integrations.vault", true);
        setDefaultIfMissing("integrations.discord.enabled", false);
        setDefaultIfMissing("integrations.discord.announce_events", true);
        setDefaultIfMissing("integrations.worldguard.enabled", true);
        setDefaultIfMissing("integrations.worldguard.respect_regions", true);
        
        // Message prefix
        setDefaultIfMissing("messages.prefix", "§6[SwiftEvents] §r");
    }
    
    private void setDefaultIfMissing(String path, Object defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
        }
    }
    
    private void cacheCommonValues() {
        // Database settings
        databaseEnabled = config.getBoolean("database.enabled", false);
        databaseHost = config.getString("database.host", "localhost");
        databasePort = config.getInt("database.port", 3306);
        databaseName = config.getString("database.name", "swiftevents");
        databaseUsername = config.getString("database.username", "root");
        databasePassword = config.getString("database.password", "password");
        databaseConnectionTimeout = config.getInt("database.connection_timeout", 30);
        maxDatabaseConnections = config.getInt("database.max_connections", 10);
        
        // GUI settings
        guiEnabled = config.getBoolean("gui.enabled", true);
        guiTitle = config.getString("gui.title", "SwiftEvents");
        
        // HUD settings
        hudEnabled = config.getBoolean("hud.enabled", true);
        hudPosition = config.getString("hud.position", "ACTION_BAR");
        hudAnimationsEnabled = config.getBoolean("hud.animations", true);
        
        // Chat settings
        chatEnabled = config.getBoolean("chat.enabled", true);
        
        // Event settings
        maxConcurrentEvents = config.getInt("events.max_concurrent", 5);
        autoSaveInterval = config.getInt("events.auto_save_interval", 300);
        playerCooldown = config.getInt("events.player_cooldown", 300);
        maxEventsPerPlayer = config.getInt("events.max_events_per_player", 3);
        
        // Event tasker
        eventTaskerEnabled = config.getBoolean("event_tasker.enabled", false);
        
        // Localization
        defaultLanguage = config.getString("localization.default_language", "en");
        perPlayerLanguage = config.getBoolean("localization.per_player_language", false);
        
        // System settings
        debugMode = config.getBoolean("debug_mode", false);
        metricsEnabled = config.getBoolean("metrics.enabled", true);
        backupOnShutdown = config.getBoolean("backup_on_shutdown", true);
        
        // Message prefix
        messagePrefix = config.getString("messages.prefix", "§6[SwiftEvents] §r");
    }
    
    // Optimized getter methods using cached values
    public boolean isDatabaseEnabled() {
        return databaseEnabled;
    }
    
    public String getDatabaseType() {
        return getCachedString("database.type", "mysql");
    }
    
    public String getDatabaseHost() {
        return databaseHost;
    }
    
    public int getDatabasePort() {
        return databasePort;
    }
    
    public String getDatabaseName() {
        return databaseName;
    }
    
    public String getDatabaseUsername() {
        return databaseUsername;
    }
    
    public String getDatabasePassword() {
        return databasePassword;
    }
    
    public int getDatabaseConnectionTimeout() {
        return databaseConnectionTimeout;
    }
    
    public int getMaxDatabaseConnections() {
        return maxDatabaseConnections;
    }
    
    public int getDatabaseValidationTimeout() {
        return getCachedInt("database.validation_timeout", 5);
    }
    
    public String getJsonFolder() {
        return getCachedString("json.folder", "events");
    }
    
    public boolean isJsonAutoBackupEnabled() {
        return getCachedBoolean("json.auto_backup", true);
    }
    
    public int getJsonBackupInterval() {
        return getCachedInt("json.backup_interval", 3600);
    }
    
    public int getMaxJsonBackups() {
        return getCachedInt("json.max_backups", 5);
    }
    
    public boolean isGUIEnabled() {
        return guiEnabled;
    }
    
    public String getGUITitle() {
        return guiTitle;
    }
    
    public int getGUISize() {
        return getCachedInt("gui.size", 54);
    }
    
    public int getGUIUpdateInterval() {
        return getCachedInt("gui.update_interval", 30);
    }
    
    public boolean isGUIAnimationsEnabled() {
        return getCachedBoolean("gui.animations", true);
    }
    
    public boolean isGUIAutoRefreshEnabled() {
        return getCachedBoolean("gui.auto_refresh", true);
    }
    
    public boolean isHUDEnabled() {
        return hudEnabled;
    }
    
    public String getHUDPosition() {
        return hudPosition;
    }
    
    public int getHUDNotificationDuration() {
        return getCachedInt("hud.notification_duration", 5);
    }
    
    public boolean isHUDAnimationsEnabled() {
        return hudAnimationsEnabled;
    }
    
    public String getHUDColor(String eventType) {
        return getCachedString("hud.colors." + eventType.toLowerCase(), "§6");
    }
    
    public boolean isChatEnabled() {
        return chatEnabled;
    }
    
    public boolean isChatSoundEffectsEnabled() {
        return getCachedBoolean("chat.sound_effects", true);
    }
    
    public boolean isChatAnnounceToAll() {
        return getCachedBoolean("chat.announce_to_all", true);
    }
    
    public int getChatReminderTime() {
        return getCachedInt("chat.reminder_time", 300);
    }
    
    public boolean isChatRemindersEnabled() {
        return getCachedBoolean("chat.reminders_enabled", true);
    }
    
    public boolean isChatInteractiveMessagesEnabled() {
        return getCachedBoolean("chat.interactive_messages", true);
    }
    
    public boolean isChatHoverTooltipsEnabled() {
        return getCachedBoolean("chat.hover_tooltips", true);
    }
    
    public int getMaxConcurrentEvents() {
        return maxConcurrentEvents;
    }
    
    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }
    
    public int getPlayerCooldown() {
        return playerCooldown;
    }
    
    public int getMaxEventsPerPlayer() {
        return maxEventsPerPlayer;
    }
    
    public boolean isEventStatisticsEnabled() {
        return getCachedBoolean("events.statistics_enabled", true);
    }
    
    public int getAutoCancelEmptyAfter() {
        return getCachedInt("events.auto_cancel_empty_after", 600);
    }
    
    public boolean isEventTaskerEnabled() {
        return eventTaskerEnabled;
    }
    
    public int getTaskerCheckInterval() {
        return getCachedInt("event_tasker.check_interval", 60);
    }
    
    public int getMinEventInterval() {
        return getCachedInt("event_tasker.min_event_interval", 1800);
    }
    
    public int getMaxEventInterval() {
        return getCachedInt("event_tasker.max_event_interval", 7200);
    }
    
    public boolean isAnnounceUpcoming() {
        return getCachedBoolean("event_tasker.announce_upcoming", true);
    }
    
    public int getAnnounceTime() {
        return getCachedInt("event_tasker.announce_time", 300);
    }
    
    // Optimization: Return cached collection or empty list if not present
    @SuppressWarnings("unchecked")
    public List<String> getAutoJoinGroups() {
        return (List<String>) configCache.computeIfAbsent("permission_groups.auto_join", 
            k -> config.getStringList("permission_groups.auto_join"));
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getEventCreatorGroups() {
        return (List<String>) configCache.computeIfAbsent("permission_groups.event_creators", 
            k -> config.getStringList("permission_groups.event_creators"));
    }
    
    public int getGroupMaxEvents(String group) {
        return getCachedInt("permission_groups.limits." + group + ".max_events_per_player", 2);
    }
    
    public int getGroupCooldown(String group) {
        return getCachedInt("permission_groups.limits." + group + ".event_cooldown", 300);
    }
    
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    public boolean isPerPlayerLanguageEnabled() {
        return perPlayerLanguage;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, String> getAvailableLanguages() {
        return (Map<String, String>) configCache.computeIfAbsent("localization.languages", k -> {
            ConfigurationSection section = config.getConfigurationSection("localization.languages");
            if (section == null) {
                return Map.of("en", "English");
            }
            
            Map<String, String> languages = new HashMap<>(section.getKeys(false).size());
            for (String key : section.getKeys(false)) {
                languages.put(key, section.getString(key, key));
            }
            return languages;
        });
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }
    
    public boolean isBackupOnShutdown() {
        return backupOnShutdown;
    }
    
    public boolean isCachingEnabled() {
        return getCachedBoolean("advanced.performance.caching_enabled", true);
    }
    
    public int getCacheDuration() {
        return getCachedInt("advanced.performance.cache_duration", 300);
    }
    
    public boolean isAsyncOperationsEnabled() {
        return getCachedBoolean("advanced.performance.async_operations", true);
    }
    
    public boolean isBatchOperationsEnabled() {
        return getCachedBoolean("advanced.performance.batch_operations", true);
    }
    
    public int getBatchSize() {
        return getCachedInt("advanced.performance.batch_size", 50);
    }
    
    // Integration settings
    public boolean isPlaceholderAPIEnabled() {
        return getCachedBoolean("integrations.placeholder_api", true);
    }
    
    public boolean isVaultEnabled() {
        return getCachedBoolean("integrations.vault", true);
    }
    
    public boolean isDiscordEnabled() {
        return getCachedBoolean("integrations.discord.enabled", false);
    }
    
    public String getDiscordWebhookUrl() {
        return getCachedString("integrations.discord.webhook_url", "");
    }
    
    public boolean isDiscordAnnounceEventsEnabled() {
        return getCachedBoolean("integrations.discord.announce_events", true);
    }
    
    public boolean isWorldGuardEnabled() {
        return getCachedBoolean("integrations.worldguard.enabled", true);
    }
    
    public boolean isWorldGuardRespectRegions() {
        return getCachedBoolean("integrations.worldguard.respect_regions", true);
    }
    
    // Message handling with caching
    public String getMessage(String key) {
        String cacheKey = "messages." + key;
        return getCachedString(cacheKey, "Message not found: " + key);
    }
    
    public String getPrefix() {
        return messagePrefix;
    }
    
    // Runtime configuration modification
    public boolean setConfigValue(String path, Object value) {
        if (path == null || value == null) {
            return false;
        }
        
        try {
            config.set(path, value);
            
            // Update cache
            configCache.put(path, value);
            
            // Update cached primitives if needed
            updatePrimitiveCacheIfNeeded(path, value);
            
            plugin.saveConfig();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set config value " + path + ": " + e.getMessage());
            return false;
        }
    }
    
    private void updatePrimitiveCacheIfNeeded(String path, Object value) {
        // Update cached primitives when their config values change
        switch (path) {
            case "database.enabled" -> {
                if (value instanceof Boolean bool) databaseEnabled = bool;
            }
            case "gui.enabled" -> {
                if (value instanceof Boolean bool) guiEnabled = bool;
            }
            case "hud.enabled" -> {
                if (value instanceof Boolean bool) hudEnabled = bool;
            }
            case "events.max_concurrent" -> {
                if (value instanceof Integer i) maxConcurrentEvents = i;
            }
            case "events.auto_save_interval" -> {
                if (value instanceof Integer i) autoSaveInterval = i;
            }
            // Add more cases as needed
        }
    }
    
    public Object getConfigValue(String path) {
        return configCache.computeIfAbsent(path, k -> config.get(path));
    }
    
    public boolean hasConfigValue(String path) {
        return config.contains(path);
    }
    
    // Optimized cache access methods
    private String getCachedString(String key, String defaultValue) {
        return (String) configCache.computeIfAbsent(key, k -> config.getString(k, defaultValue));
    }
    
    private int getCachedInt(String key, int defaultValue) {
        return (Integer) configCache.computeIfAbsent(key, k -> config.getInt(k, defaultValue));
    }
    
    private boolean getCachedBoolean(String key, boolean defaultValue) {
        return (Boolean) configCache.computeIfAbsent(key, k -> config.getBoolean(k, defaultValue));
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public void reloadConfig() {
        try {
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            // Clear cache to force re-reading
            clearCache();
            
            // Re-validate and cache
            if (validateConfig()) {
                cacheCommonValues();
                plugin.getLogger().info("Configuration reloaded successfully!");
            } else {
                plugin.getLogger().warning("Configuration reloaded with validation errors:");
                for (String error : validationErrors) {
                    plugin.getLogger().warning("  - " + error);
                }
                cacheCommonValues(); // Still cache what we can
            }
            
            // Reload messages
            loadMessages();
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean loadMessages() {
        // Implementation would load message files here
        // For now, just return true as messages are loaded from config.yml
        return true;
    }
    
    public void clearCache() {
        configCache.clear();
    }
} 