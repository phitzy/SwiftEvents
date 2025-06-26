package com.swiftevents.config;

import com.swiftevents.SwiftEventsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

public class ConfigManager {
    
    private final SwiftEventsPlugin plugin;
    private FileConfiguration config;
    
    // Cache for frequently accessed config values
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    
    // Pre-cached common values
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
    
    // Validation error storage
    private final List<String> validationErrors = new ArrayList<>();
    
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
        int timeout = config.getInt("database.connection_timeout", 30);
        if (timeout < 5 || timeout > 300) {
            validationErrors.add("database.connection_timeout must be between 5 and 300 seconds");
        }
        
        int maxConnections = config.getInt("database.max_connections", 10);
        if (maxConnections < 1 || maxConnections > 50) {
            validationErrors.add("database.max_connections must be between 1 and 50");
        }
        
        String host = config.getString("database.host", "localhost");
        if (host.isEmpty()) {
            validationErrors.add("database.host cannot be empty");
        }
    }
    
    private void validateEventConfig() {
        int maxConcurrent = config.getInt("events.max_concurrent", 5);
        if (maxConcurrent < 1 || maxConcurrent > 100) {
            validationErrors.add("events.max_concurrent must be between 1 and 100");
        }
        
        int playerCooldown = config.getInt("events.player_cooldown", 300);
        if (playerCooldown < 0) {
            validationErrors.add("events.player_cooldown cannot be negative");
        }
        
        int maxEventsPerPlayer = config.getInt("events.max_events_per_player", 3);
        if (maxEventsPerPlayer < 1 || maxEventsPerPlayer > 50) {
            validationErrors.add("events.max_events_per_player must be between 1 and 50");
        }
    }
    
    private void validateHUDConfig() {
        String hudPosition = config.getString("hud.position", "ACTION_BAR");
        List<String> validPositions = Arrays.asList("ACTION_BAR", "BOSS_BAR", "TITLE");
        if (!validPositions.contains(hudPosition)) {
            validationErrors.add("hud.position must be one of: " + String.join(", ", validPositions));
        }
        
        int duration = config.getInt("hud.notification_duration", 5);
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
        if (defaultLang.isEmpty()) {
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
        // Database settings
        setDefaultIfMissing("database.enabled", false);
        setDefaultIfMissing("database.type", "mysql");
        setDefaultIfMissing("database.host", "localhost");
        setDefaultIfMissing("database.port", 3306);
        setDefaultIfMissing("database.name", "swiftevents");
        setDefaultIfMissing("database.username", "root");
        setDefaultIfMissing("database.password", "password");
        setDefaultIfMissing("database.connection_timeout", 30);
        setDefaultIfMissing("database.max_connections", 10);
        setDefaultIfMissing("database.connection_validation_timeout", 5);
        
        // JSON storage settings
        setDefaultIfMissing("json.folder", "events");
        setDefaultIfMissing("json.auto_backup", true);
        setDefaultIfMissing("json.backup_interval", 3600);
        setDefaultIfMissing("json.max_backups", 5);
        
        // GUI settings
        setDefaultIfMissing("gui.enabled", true);
        setDefaultIfMissing("gui.title", "§6SwiftEvents");
        setDefaultIfMissing("gui.size", 54);
        setDefaultIfMissing("gui.update_interval", 5);
        setDefaultIfMissing("gui.animations_enabled", true);
        setDefaultIfMissing("gui.auto_refresh", true);
        
        // HUD settings
        setDefaultIfMissing("hud.enabled", true);
        setDefaultIfMissing("hud.position", "ACTION_BAR");
        setDefaultIfMissing("hud.notification_duration", 5);
        setDefaultIfMissing("hud.animations_enabled", true);
        
        // Chat settings
        setDefaultIfMissing("chat.enabled", true);
        setDefaultIfMissing("chat.sound_effects", true);
        setDefaultIfMissing("chat.announce_to_all", true);
        setDefaultIfMissing("chat.reminder_time", 5);
        setDefaultIfMissing("chat.reminders_enabled", true);
        setDefaultIfMissing("chat.interactive_messages", true);
        setDefaultIfMissing("chat.hover_tooltips", true);
        
        // Event settings
        setDefaultIfMissing("events.max_concurrent", 5);
        setDefaultIfMissing("events.auto_save_interval", 300);
        setDefaultIfMissing("events.player_cooldown", 300);
        setDefaultIfMissing("events.max_events_per_player", 3);
        setDefaultIfMissing("events.track_statistics", true);
        setDefaultIfMissing("events.auto_cancel_empty_after", 10);
        
        // Event Tasker settings
        setDefaultIfMissing("event_tasker.enabled", false);
        setDefaultIfMissing("event_tasker.check_interval", 60);
        setDefaultIfMissing("event_tasker.min_event_interval", 30);
        setDefaultIfMissing("event_tasker.max_event_interval", 120);
        setDefaultIfMissing("event_tasker.announce_upcoming", true);
        setDefaultIfMissing("event_tasker.announce_time", 5);
        
        // Permission groups
        setDefaultIfMissing("permission_groups.auto_join_groups", Arrays.asList("vip", "premium", "staff"));
        setDefaultIfMissing("permission_groups.event_creator_groups", Arrays.asList("admin", "moderator"));
        
        // Localization settings
        setDefaultIfMissing("localization.default_language", "en");
        setDefaultIfMissing("localization.per_player_language", false);
        
        // Advanced settings
        setDefaultIfMissing("advanced.debug_mode", false);
        setDefaultIfMissing("advanced.metrics_enabled", true);
        setDefaultIfMissing("advanced.backup_on_shutdown", true);
        setDefaultIfMissing("advanced.performance.enable_caching", true);
        setDefaultIfMissing("advanced.performance.cache_duration", 300);
        setDefaultIfMissing("advanced.performance.async_operations", true);
        setDefaultIfMissing("advanced.performance.batch_operations", true);
        setDefaultIfMissing("advanced.performance.batch_size", 50);
        
        // Integration settings
        setDefaultIfMissing("integrations.placeholderapi", true);
        setDefaultIfMissing("integrations.vault", true);
        setDefaultIfMissing("integrations.discord.enabled", false);
        setDefaultIfMissing("integrations.discord.webhook_url", "");
        setDefaultIfMissing("integrations.discord.announce_events", true);
        setDefaultIfMissing("integrations.worldguard.enabled", true);
        setDefaultIfMissing("integrations.worldguard.respect_regions", true);
        
        // Messages
        setDefaultIfMissing("messages.prefix", "§6[SwiftEvents]§r ");
        setDefaultIfMissing("messages.no_permission", "§cYou don't have permission to use this command!");
        setDefaultIfMissing("messages.cooldown_active", "§cYou must wait {time} before joining another event!");
        setDefaultIfMissing("messages.max_events_reached", "§cYou have reached the maximum number of events you can join!");
        setDefaultIfMissing("messages.config_reloaded", "§aConfiguration has been reloaded successfully!");
        setDefaultIfMissing("messages.config_invalid", "§cConfiguration validation failed: {errors}");
        setDefaultIfMissing("messages.backup_created", "§aEvent data backup created successfully!");
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
        
        // HUD settings
        hudEnabled = config.getBoolean("hud.enabled", true);
        hudPosition = config.getString("hud.position", "ACTION_BAR");
        hudAnimationsEnabled = config.getBoolean("hud.animations_enabled", true);
        
        // Event settings
        maxConcurrentEvents = config.getInt("events.max_concurrent", 5);
        autoSaveInterval = config.getInt("events.auto_save_interval", 300);
        playerCooldown = config.getInt("events.player_cooldown", 300);
        maxEventsPerPlayer = config.getInt("events.max_events_per_player", 3);
        
        // Tasker settings
        eventTaskerEnabled = config.getBoolean("event_tasker.enabled", false);
        
        // Advanced settings
        debugMode = config.getBoolean("advanced.debug_mode", false);
        metricsEnabled = config.getBoolean("advanced.metrics_enabled", true);
        backupOnShutdown = config.getBoolean("advanced.backup_on_shutdown", true);
        
        // Localization settings
        defaultLanguage = config.getString("localization.default_language", "en");
        perPlayerLanguage = config.getBoolean("localization.per_player_language", false);
        
        // Messages
        messagePrefix = config.getString("messages.prefix", "§6[SwiftEvents]§r ");
    }
    
    // Enhanced getter methods with better caching
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
        return getCachedInt("database.connection_validation_timeout", 5);
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
        return getCachedBoolean("gui.enabled", true);
    }
    
    public String getGUITitle() {
        return getCachedString("gui.title", "§6SwiftEvents");
    }
    
    public int getGUISize() {
        return getCachedInt("gui.size", 54);
    }
    
    public int getGUIUpdateInterval() {
        return getCachedInt("gui.update_interval", 5);
    }
    
    public boolean isGUIAnimationsEnabled() {
        return getCachedBoolean("gui.animations_enabled", true);
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
        return getCachedString("hud.colors." + eventType, "#FFAA00");
    }
    
    public boolean isChatEnabled() {
        return getCachedBoolean("chat.enabled", true);
    }
    
    public boolean isChatSoundEffectsEnabled() {
        return getCachedBoolean("chat.sound_effects", true);
    }
    
    public boolean isChatAnnounceToAll() {
        return getCachedBoolean("chat.announce_to_all", true);
    }
    
    public int getChatReminderTime() {
        return getCachedInt("chat.reminder_time", 5);
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
        return getCachedBoolean("events.track_statistics", true);
    }
    
    public int getAutoCancelEmptyAfter() {
        return getCachedInt("events.auto_cancel_empty_after", 10);
    }
    
    public boolean isEventTaskerEnabled() {
        return eventTaskerEnabled;
    }
    
    public int getTaskerCheckInterval() {
        return getCachedInt("event_tasker.check_interval", 60);
    }
    
    public int getMinEventInterval() {
        return getCachedInt("event_tasker.min_event_interval", 30);
    }
    
    public int getMaxEventInterval() {
        return getCachedInt("event_tasker.max_event_interval", 120);
    }
    
    public boolean isAnnounceUpcoming() {
        return getCachedBoolean("event_tasker.announce_upcoming", true);
    }
    
    public int getAnnounceTime() {
        return getCachedInt("event_tasker.announce_time", 5);
    }
    
    // Permission group methods
    public List<String> getAutoJoinGroups() {
        return config.getStringList("permission_groups.auto_join_groups");
    }
    
    public List<String> getEventCreatorGroups() {
        return config.getStringList("permission_groups.event_creator_groups");
    }
    
    public int getGroupMaxEvents(String group) {
        return config.getInt("permission_groups.limits." + group + ".max_events_per_player", maxEventsPerPlayer);
    }
    
    public int getGroupCooldown(String group) {
        return config.getInt("permission_groups.limits." + group + ".event_cooldown", playerCooldown);
    }
    
    // Localization methods
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    public boolean isPerPlayerLanguageEnabled() {
        return perPlayerLanguage;
    }
    
    public Map<String, String> getAvailableLanguages() {
        Map<String, String> languages = new ConcurrentHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("localization.languages");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                languages.put(key, section.getString(key));
            }
        }
        return languages;
    }
    
    // Advanced settings
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
        return getCachedBoolean("advanced.performance.enable_caching", true);
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
        return getCachedBoolean("integrations.placeholderapi", true);
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
    
    public String getMessage(String key) {
        if (key == null || key.isEmpty()) {
            plugin.getLogger().warning("Attempted to get message with null/empty key");
            return "§cInvalid message key";
        }
        return getCachedString("messages." + key, "§cMessage not found: " + key);
    }
    
    public String getPrefix() {
        return messagePrefix;
    }
    
    // Runtime configuration modification methods
    public boolean setConfigValue(String path, Object value) {
        try {
            config.set(path, value);
            plugin.saveConfig();
            
            // Update cache for commonly accessed values
            cacheCommonValues();
            
            if (debugMode) {
                plugin.getLogger().info("Configuration value updated: " + path + " = " + value);
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to update configuration value: " + path, e);
            return false;
        }
    }
    
    public Object getConfigValue(String path) {
        return config.get(path);
    }
    
    public boolean hasConfigValue(String path) {
        return config.contains(path);
    }
    
    // Enhanced caching methods
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
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Clear cache
        configCache.clear();
        
        // Validate and cache again
        if (validateConfig()) {
            cacheCommonValues();
            plugin.getLogger().info("Configuration reloaded and validated successfully!");
        } else {
            plugin.getLogger().warning("Configuration reloaded with validation errors. Check logs for details.");
            cacheCommonValues(); // Still cache what we can
        }
    }
    
    /**
     * Safely loads and validates message resources
     * @return true if messages were loaded successfully
     */
    public boolean loadMessages() {
        try {
            // Check if messages file exists
            if (plugin.getResource("messages_en.yml") == null) {
                plugin.getLogger().warning("Default messages file not found in plugin resources");
                return false;
            }
            
            // Validate that required message keys exist
            String[] requiredKeys = {
                "no_permission", "event_joined", "event_left", "event_not_found",
                "creation_success", "creation_failed", "config_reloaded"
            };
            
            for (String key : requiredKeys) {
                if (!config.contains("messages." + key)) {
                    plugin.getLogger().warning("Missing required message key: " + key);
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load messages: " + e.getMessage());
            return false;
        }
    }
    
    public void clearCache() {
        configCache.clear();
        if (debugMode) {
            plugin.getLogger().info("Configuration cache cleared");
        }
    }
} 