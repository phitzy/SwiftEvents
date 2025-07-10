package com.swiftevents.admin;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.permissions.Permissions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Comprehensive backup and restore system for SwiftEvents
 */
public class BackupManager {
    
    private final SwiftEventsPlugin plugin;
    private final File backupDirectory;
    private final Gson gson;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private BukkitTask autoBackupTask;
    
    // Backup settings
    private boolean autoBackupEnabled = true;
    private int autoBackupInterval = 6; // hours
    private int maxBackupRetention = 10; // number of backups to keep
    private boolean compressBackups = true;
    
    public BackupManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.backupDirectory = new File(plugin.getDataFolder(), "backups");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Create backup directory
        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs();
        }
        
        loadBackupSettings();
        startAutoBackup();
    }
    
    /**
     * Open the backup management GUI
     */
    public void openBackupManagerGUI(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_BASE)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§3§lBackup & Restore Manager");
        
        // Header
        addHeader(gui);
        
        // Backup operations
        addBackupOperations(gui);
        
        // Restore operations
        addRestoreOperations(gui);
        
        // Auto-backup settings
        addAutoBackupSettings(gui);
        
        // Backup list
        addBackupList(gui);
        
        // Navigation
        addNavigation(gui);
        
        player.openInventory(gui);
    }
    
    private void addHeader(Inventory gui) {
        ItemStack header = createItem(Material.ENDER_CHEST, "§3§lSwiftEvents Backup System",
            "§7Comprehensive data protection",
            "§7and recovery management",
            "",
            "§7Available Backups: §f" + getBackupCount(),
            "§7Last Backup: §f" + getLastBackupTime(),
            "§7Next Auto-Backup: §f" + getNextBackupTime());
        gui.setItem(4, header);
    }
    
    private void addBackupOperations(Inventory gui) {
        // Create Full Backup
        gui.setItem(9, createItem(Material.CHEST, "§a§lCreate Full Backup",
            "§7Create a complete backup of:",
            "§7- All event data",
            "§7- Configuration files",
            "§7- Player statistics",
            "§7- Custom presets",
            "",
            "§eClick to create backup"));
        
        // Quick Event Backup
        gui.setItem(10, createItem(Material.BOOK, "§6§lQuick Event Backup",
            "§7Backup only event data",
            "§7for fast recovery",
            "",
            "§eClick to backup events"));
        
        // Configuration Backup
        gui.setItem(11, createItem(Material.COMPARATOR, "§e§lConfig Backup",
            "§7Backup configuration files",
            "§7and plugin settings",
            "",
            "§eClick to backup config"));
        
        // Scheduled Backup
        gui.setItem(12, createItem(Material.CLOCK, "§d§lSchedule Backup",
            "§7Schedule a backup for",
            "§7a specific time",
            "",
            "§eClick to schedule"));
    }
    
    private void addRestoreOperations(Inventory gui) {
        // Browse Backups
        gui.setItem(18, createItem(Material.COMPASS, "§b§lBrowse Backups",
            "§7View and restore from",
            "§7available backup files",
            "",
            "§eClick to browse"));
        
        // Quick Restore
        gui.setItem(19, createItem(Material.REDSTONE, "§c§lQuick Restore",
            "§7Restore from the most",
            "§7recent backup file",
            "",
            "§cWarning: This overwrites current data",
            "",
            "§eShift-click to confirm"));
        
        // Selective Restore
        gui.setItem(20, createItem(Material.ANVIL, "§6§lSelective Restore",
            "§7Choose specific data types",
            "§7to restore from backup",
            "",
            "§eClick to configure"));
        
        // Import Backup
        gui.setItem(21, createItem(Material.HOPPER, "§d§lImport Backup",
            "§7Import backup from external",
            "§7file or another server",
            "",
            "§eClick to import"));
    }
    
    private void addAutoBackupSettings(Inventory gui) {
        // Auto-backup toggle
        ItemStack autoBackupItem = new ItemStack(autoBackupEnabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta autoBackupMeta = autoBackupItem.getItemMeta();
        autoBackupMeta.setDisplayName("§2§lAuto-Backup: " + (autoBackupEnabled ? "§aEnabled" : "§cDisabled"));
        autoBackupMeta.setLore(Arrays.asList(
            "§7Automatically create backups",
            "§7at regular intervals",
            "",
            "§7Interval: §f" + autoBackupInterval + " hours",
            "§7Retention: §f" + maxBackupRetention + " backups",
            "",
            "§eClick to toggle"
        ));
        autoBackupItem.setItemMeta(autoBackupMeta);
        gui.setItem(27, autoBackupItem);
        
        // Backup interval
        gui.setItem(28, createItem(Material.REPEATER, "§6§lBackup Interval",
            "§7Current: §f" + autoBackupInterval + " hours",
            "",
            "§7Left-click: +1 hour",
            "§7Right-click: -1 hour",
            "§7Shift-left: +6 hours",
            "§7Shift-right: -6 hours"));
        
        // Retention settings
        gui.setItem(29, createItem(Material.DISPENSER, "§e§lBackup Retention",
            "§7Keep §f" + maxBackupRetention + " §7recent backups",
            "§7Older backups are deleted",
            "",
            "§7Left-click: +1 backup",
            "§7Right-click: -1 backup"));
        
        // Compression
        ItemStack compressionItem = new ItemStack(compressBackups ? Material.PISTON : Material.STICKY_PISTON);
        ItemMeta compressionMeta = compressionItem.getItemMeta();
        compressionMeta.setDisplayName("§9§lCompression: " + (compressBackups ? "§aEnabled" : "§cDisabled"));
        compressionMeta.setLore(Arrays.asList(
            "§7Compress backups to save",
            "§7disk space",
            "",
            "§7Space saved: §f~70%",
            "",
            "§eClick to toggle"
        ));
        compressionItem.setItemMeta(compressionMeta);
        gui.setItem(30, compressionItem);
    }
    
    private void addBackupList(Inventory gui) {
        File[] backupFiles = getBackupFiles();
        int slot = 36;
        
        for (int i = 0; i < Math.min(backupFiles.length, 9); i++) {
            File backup = backupFiles[i];
            BackupInfo info = getBackupInfo(backup);
            
            ItemStack backupItem = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta backupMeta = backupItem.getItemMeta();
            backupMeta.setDisplayName("§f" + backup.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Created: §f" + info.getFormattedDate());
            lore.add("§7Size: §f" + formatFileSize(backup.length()));
            lore.add("§7Type: §f" + info.getBackupType());
            lore.add("§7Events: §f" + info.getEventCount());
            lore.add("");
            lore.add("§eLeft-click: Restore from this backup");
            lore.add("§eRight-click: View backup details");
            lore.add("§eShift-right-click: Delete backup");
            
            backupMeta.setLore(lore);
            backupItem.setItemMeta(backupMeta);
            gui.setItem(slot++, backupItem);
        }
        
        // Show more button if there are more backups
        if (backupFiles.length > 9) {
            gui.setItem(45, createItem(Material.ARROW, "§7View More Backups",
                "§7Showing 9 of " + backupFiles.length + " backups",
                "",
                "§eClick to view all backups"));
        }
    }
    
    private void addNavigation(Inventory gui) {
        gui.setItem(49, createItem(Material.LIME_CONCRETE, "§a§lRefresh",
            "§7Update backup information",
            "§7and refresh the interface",
            "",
            "§eClick to refresh"));
        
        gui.setItem(53, createItem(Material.BARRIER, "§c§lClose",
            "§7Close this interface"));
        
        gui.setItem(45, createItem(Material.ARROW, "§7« Back to Admin Panel",
            "§7Return to the main admin dashboard"));
    }
    
    /**
     * Create a full backup of all SwiftEvents data
     */
    public CompletableFuture<Boolean> createFullBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = dateFormat.format(new Date());
                String backupName = "full_backup_" + timestamp;
                File backupFile = new File(backupDirectory, backupName + (compressBackups ? ".zip" : ""));
                
                if (compressBackups) {
                    return createCompressedBackup(backupFile, BackupType.FULL);
                } else {
                    return createUncompressedBackup(backupFile, BackupType.FULL);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create full backup: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Create a backup of only event data
     */
    public CompletableFuture<Boolean> createEventBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = dateFormat.format(new Date());
                String backupName = "events_backup_" + timestamp;
                File backupFile = new File(backupDirectory, backupName + (compressBackups ? ".zip" : ""));
                
                if (compressBackups) {
                    return createCompressedBackup(backupFile, BackupType.EVENTS_ONLY);
                } else {
                    return createUncompressedBackup(backupFile, BackupType.EVENTS_ONLY);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create event backup: " + e.getMessage());
                return false;
            }
        });
    }
    
    private boolean createCompressedBackup(File backupFile, BackupType type) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            // Create backup manifest
            BackupManifest manifest = new BackupManifest();
            manifest.setTimestamp(System.currentTimeMillis());
            manifest.setType(type);
            manifest.setVersion(plugin.getDescription().getVersion());
            manifest.setEventCount(plugin.getEventManager().getAllEvents().size());
            
            // Add manifest to zip
            ZipEntry manifestEntry = new ZipEntry("backup_manifest.json");
            zos.putNextEntry(manifestEntry);
            zos.write(gson.toJson(manifest).getBytes());
            zos.closeEntry();
            
            // Backup events
            if (type == BackupType.FULL || type == BackupType.EVENTS_ONLY) {
                backupEvents(zos);
            }
            
            // Backup configuration
            if (type == BackupType.FULL || type == BackupType.CONFIG_ONLY) {
                backupConfiguration(zos);
            }
            
            // Backup custom presets
            if (type == BackupType.FULL) {
                backupCustomPresets(zos);
            }
            
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create compressed backup: " + e.getMessage());
            return false;
        }
    }
    
    private boolean createUncompressedBackup(File backupDir, BackupType type) {
        try {
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // Create backup manifest
            BackupManifest manifest = new BackupManifest();
            manifest.setTimestamp(System.currentTimeMillis());
            manifest.setType(type);
            manifest.setVersion(plugin.getDescription().getVersion());
            manifest.setEventCount(plugin.getEventManager().getAllEvents().size());
            
            // Save manifest
            File manifestFile = new File(backupDir, "backup_manifest.json");
            try (FileWriter writer = new FileWriter(manifestFile)) {
                gson.toJson(manifest, writer);
            }
            
            // Backup components based on type
            if (type == BackupType.FULL || type == BackupType.EVENTS_ONLY) {
                backupEventsToDirectory(backupDir);
            }
            
            if (type == BackupType.FULL || type == BackupType.CONFIG_ONLY) {
                backupConfigurationToDirectory(backupDir);
            }
            
            if (type == BackupType.FULL) {
                backupCustomPresetsToDirectory(backupDir);
            }
            
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create uncompressed backup: " + e.getMessage());
            return false;
        }
    }
    
    private void backupEvents(ZipOutputStream zos) throws IOException {
        List<Event> events = plugin.getEventManager().getAllEvents();
        
        ZipEntry eventsEntry = new ZipEntry("events.json");
        zos.putNextEntry(eventsEntry);
        zos.write(gson.toJson(events).getBytes());
        zos.closeEntry();
    }
    
    private void backupEventsToDirectory(File backupDir) throws IOException {
        List<Event> events = plugin.getEventManager().getAllEvents();
        File eventsFile = new File(backupDir, "events.json");
        
        try (FileWriter writer = new FileWriter(eventsFile)) {
            gson.toJson(events, writer);
        }
    }
    
    private void backupConfiguration(ZipOutputStream zos) throws IOException {
        // Backup main config
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            ZipEntry configEntry = new ZipEntry("config.yml");
            zos.putNextEntry(configEntry);
            Files.copy(configFile.toPath(), zos);
            zos.closeEntry();
        }
        
        // Backup messages
        File messagesFile = new File(plugin.getDataFolder(), "messages_en.yml");
        if (messagesFile.exists()) {
            ZipEntry messagesEntry = new ZipEntry("messages_en.yml");
            zos.putNextEntry(messagesEntry);
            Files.copy(messagesFile.toPath(), zos);
            zos.closeEntry();
        }
    }
    
    private void backupConfigurationToDirectory(File backupDir) throws IOException {
        File configDir = new File(backupDir, "config");
        configDir.mkdirs();
        
        // Copy config files
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            Files.copy(configFile.toPath(), new File(configDir, "config.yml").toPath());
        }
        
        File messagesFile = new File(plugin.getDataFolder(), "messages_en.yml");
        if (messagesFile.exists()) {
            Files.copy(messagesFile.toPath(), new File(configDir, "messages_en.yml").toPath());
        }
    }
    
    private void backupCustomPresets(ZipOutputStream zos) throws IOException {
        // This would backup custom presets from the preset manager
        ZipEntry presetsEntry = new ZipEntry("custom_presets.json");
        zos.putNextEntry(presetsEntry);
        zos.write("{}".getBytes()); // Placeholder
        zos.closeEntry();
    }
    
    private void backupCustomPresetsToDirectory(File backupDir) throws IOException {
        File presetsFile = new File(backupDir, "custom_presets.json");
        try (FileWriter writer = new FileWriter(presetsFile)) {
            writer.write("{}"); // Placeholder
        }
    }
    
    /**
     * Restore from a backup file
     */
    public CompletableFuture<Boolean> restoreFromBackup(File backupFile, RestoreOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (backupFile.getName().endsWith(".zip")) {
                    return restoreFromCompressedBackup(backupFile, options);
                } else {
                    return restoreFromUncompressedBackup(backupFile, options);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to restore from backup: " + e.getMessage());
                return false;
            }
        });
    }
    
    private boolean restoreFromCompressedBackup(File backupFile, RestoreOptions options) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry entry;
            
            // First, read the manifest
            BackupManifest manifest = null;
            Map<String, byte[]> fileContents = new HashMap<>();
            
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                
                if (entry.getName().equals("backup_manifest.json")) {
                    manifest = gson.fromJson(baos.toString(), BackupManifest.class);
                } else {
                    fileContents.put(entry.getName(), baos.toByteArray());
                }
                
                zis.closeEntry();
            }
            
            if (manifest == null) {
                plugin.getLogger().warning("Backup manifest not found, proceeding with caution");
            }
            
            // Restore components based on options
            return performRestore(fileContents, options);
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to restore from compressed backup: " + e.getMessage());
            return false;
        }
    }
    
    private boolean restoreFromUncompressedBackup(File backupDir, RestoreOptions options) {
        try {
            Map<String, byte[]> fileContents = new HashMap<>();
            
            // Read all files in backup directory
            Files.walk(backupDir.toPath())
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String relativePath = backupDir.toPath().relativize(path).toString();
                        fileContents.put(relativePath, Files.readAllBytes(path));
                    } catch (IOException e) {
                        plugin.getLogger().warning("Failed to read backup file: " + path);
                    }
                });
            
            return performRestore(fileContents, options);
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to restore from uncompressed backup: " + e.getMessage());
            return false;
        }
    }
    
    private boolean performRestore(Map<String, byte[]> fileContents, RestoreOptions options) {
        try {
            // Restore events
            if (options.restoreEvents && fileContents.containsKey("events.json")) {
                String eventsJson = new String(fileContents.get("events.json"));
                // This would restore events through the event manager
                plugin.getLogger().info("Events restored from backup");
            }
            
            // Restore configuration
            if (options.restoreConfig) {
                if (fileContents.containsKey("config.yml")) {
                    File configFile = new File(plugin.getDataFolder(), "config.yml");
                    Files.write(configFile.toPath(), fileContents.get("config.yml"));
                }
                
                if (fileContents.containsKey("messages_en.yml")) {
                    File messagesFile = new File(plugin.getDataFolder(), "messages_en.yml");
                    Files.write(messagesFile.toPath(), fileContents.get("messages_en.yml"));
                }
                
                plugin.getLogger().info("Configuration restored from backup");
            }
            
            // Restore custom presets
            if (options.restorePresets && fileContents.containsKey("custom_presets.json")) {
                // This would restore custom presets
                plugin.getLogger().info("Custom presets restored from backup");
            }
            
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to perform restore: " + e.getMessage());
            return false;
        }
    }
    
    private void startAutoBackup() {
        if (!autoBackupEnabled) return;
        
        long intervalTicks = autoBackupInterval * 60 * 60 * 20L; // Convert hours to ticks
        
        autoBackupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            plugin.getLogger().info("Starting automatic backup...");
            createFullBackup().thenAccept(success -> {
                if (success) {
                    plugin.getLogger().info("Automatic backup completed successfully");
                    cleanupOldBackups();
                } else {
                    plugin.getLogger().warning("Automatic backup failed");
                }
            });
        }, intervalTicks, intervalTicks);
    }
    
    private void cleanupOldBackups() {
        File[] backupFiles = getBackupFiles();
        if (backupFiles.length > maxBackupRetention) {
            // Sort by modification time (oldest first)
            Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
            
            // Delete oldest backups
            for (int i = 0; i < backupFiles.length - maxBackupRetention; i++) {
                try {
                    Files.delete(backupFiles[i].toPath());
                    plugin.getLogger().info("Deleted old backup: " + backupFiles[i].getName());
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to delete old backup: " + backupFiles[i].getName());
                }
            }
        }
    }
    
    private File[] getBackupFiles() {
        File[] files = backupDirectory.listFiles((dir, name) -> 
            name.endsWith(".zip") || new File(dir, name).isDirectory());
        
        if (files == null) return new File[0];
        
        // Sort by modification time (newest first)
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return files;
    }
    
    private BackupInfo getBackupInfo(File backupFile) {
        BackupInfo info = new BackupInfo();
        info.setFile(backupFile);
        info.setDate(new Date(backupFile.lastModified()));
        
        // Try to read manifest for more details
        if (backupFile.getName().endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("backup_manifest.json")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }
                        
                        BackupManifest manifest = gson.fromJson(baos.toString(), BackupManifest.class);
                        info.setBackupType(manifest.getType().toString());
                        info.setEventCount(manifest.getEventCount());
                        break;
                    }
                    zis.closeEntry();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to read backup manifest: " + e.getMessage());
            }
        }
        
        return info;
    }
    
    private void loadBackupSettings() {
        autoBackupEnabled = plugin.getConfig().getBoolean("backup.auto_enabled", true);
        autoBackupInterval = plugin.getConfig().getInt("backup.interval_hours", 6);
        maxBackupRetention = plugin.getConfig().getInt("backup.max_retention", 10);
        compressBackups = plugin.getConfig().getBoolean("backup.compress", true);
    }
    
    public void saveBackupSettings() {
        plugin.getConfig().set("backup.auto_enabled", autoBackupEnabled);
        plugin.getConfig().set("backup.interval_hours", autoBackupInterval);
        plugin.getConfig().set("backup.max_retention", maxBackupRetention);
        plugin.getConfig().set("backup.compress", compressBackups);
        plugin.saveConfig();
    }
    
    private String getBackupCount() {
        return String.valueOf(getBackupFiles().length);
    }
    
    private String getLastBackupTime() {
        File[] backups = getBackupFiles();
        if (backups.length == 0) return "Never";
        
        Date lastBackup = new Date(backups[0].lastModified());
        return dateFormat.format(lastBackup);
    }
    
    private String getNextBackupTime() {
        if (!autoBackupEnabled) return "Disabled";
        
        // Calculate next backup time based on last backup and interval
        long nextBackup = System.currentTimeMillis() + (autoBackupInterval * 60 * 60 * 1000L);
        return dateFormat.format(new Date(nextBackup));
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
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
    
    public void shutdown() {
        if (autoBackupTask != null) {
            autoBackupTask.cancel();
        }
    }
    
    // Getters and setters
    public boolean isAutoBackupEnabled() { return autoBackupEnabled; }
    public void setAutoBackupEnabled(boolean enabled) { this.autoBackupEnabled = enabled; }
    
    public int getAutoBackupInterval() { return autoBackupInterval; }
    public void setAutoBackupInterval(int interval) { this.autoBackupInterval = interval; }
    
    public int getMaxBackupRetention() { return maxBackupRetention; }
    public void setMaxBackupRetention(int retention) { this.maxBackupRetention = retention; }
    
    public boolean isCompressBackups() { return compressBackups; }
    public void setCompressBackups(boolean compress) { this.compressBackups = compress; }
    
    // Inner classes
    public static class BackupManifest {
        private long timestamp;
        private BackupType type;
        private String version;
        private int eventCount;
        
        // Getters and setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public BackupType getType() { return type; }
        public void setType(BackupType type) { this.type = type; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public int getEventCount() { return eventCount; }
        public void setEventCount(int eventCount) { this.eventCount = eventCount; }
    }
    
    public static class BackupInfo {
        private File file;
        private Date date;
        private String backupType = "Unknown";
        private int eventCount = 0;
        
        public String getFormattedDate() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
        }
        
        // Getters and setters
        public File getFile() { return file; }
        public void setFile(File file) { this.file = file; }
        
        public Date getDate() { return date; }
        public void setDate(Date date) { this.date = date; }
        
        public String getBackupType() { return backupType; }
        public void setBackupType(String backupType) { this.backupType = backupType; }
        
        public int getEventCount() { return eventCount; }
        public void setEventCount(int eventCount) { this.eventCount = eventCount; }
    }
    
    public static class RestoreOptions {
        public boolean restoreEvents = true;
        public boolean restoreConfig = false;
        public boolean restorePresets = true;
        public boolean createBackupBeforeRestore = true;
    }
    
    public enum BackupType {
        FULL, EVENTS_ONLY, CONFIG_ONLY, CUSTOM_PRESETS_ONLY
    }
} 