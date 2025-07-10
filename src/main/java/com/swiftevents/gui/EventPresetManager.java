package com.swiftevents.gui;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.tasker.EventPreset;
import com.swiftevents.permissions.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class EventPresetManager {
    
    private final SwiftEventsPlugin plugin;
    private final Map<String, EventPreset> customPresets = new HashMap<>();
    
    public EventPresetManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        loadCustomPresets();
    }
    
    /**
     * Open the main preset management GUI
     */
    public void openPresetManagerGUI(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_BASE)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§a§lEvent Preset Manager");
        
        // Add header
        addHeader(gui);
        
        // Add existing presets from tasker
        addTaskerPresets(gui, player);
        
        // Add custom presets
        addCustomPresets(gui);
        
        // Add management tools
        addManagementTools(gui);
        
        // Add navigation
        addNavigation(gui);
        
        player.openInventory(gui);
    }
    
    private void addHeader(Inventory gui) {
        ItemStack header = createItem(Material.WRITABLE_BOOK, "§6§lPreset Templates",
            "§7Manage event templates and presets",
            "§7- Create new templates from existing events",
            "§7- Edit preset configurations",
            "§7- Duplicate successful event setups",
            "§7- Set automated weights and schedules",
            "",
            "§eSelect an option below");
        gui.setItem(4, header);
    }
    
    private void addTaskerPresets(Inventory gui, Player player) {
        Map<String, EventPreset> taskerPresets = plugin.getEventTasker().getPresets();
        int slot = 9;
        
        for (Map.Entry<String, EventPreset> entry : taskerPresets.entrySet()) {
            if (slot >= 18) break; // Only show first 9 tasker presets
            
            EventPreset preset = entry.getValue();
            ItemStack item = createPresetItem(preset, true);
            gui.setItem(slot++, item);
        }
        
        // Add "Create New Tasker Preset" button
        if (slot < 18) {
            ItemStack newPreset = createItem(Material.EMERALD, "§a§lCreate Tasker Preset",
                "§7Create a new preset for the",
                "§7automatic event tasker system",
                "",
                "§eClick to create");
            gui.setItem(slot, newPreset);
        }
    }
    
    private void addCustomPresets(Inventory gui) {
        int slot = 18;
        
        for (Map.Entry<String, EventPreset> entry : customPresets.entrySet()) {
            if (slot >= 36) break; // Limit custom presets display
            
            EventPreset preset = entry.getValue();
            ItemStack item = createPresetItem(preset, false);
            gui.setItem(slot++, item);
        }
        
        // Add "Create Custom Template" button
        if (slot < 36) {
            ItemStack newTemplate = createItem(Material.PAPER, "§b§lCreate Custom Template",
                "§7Create a template from an",
                "§7existing event or from scratch",
                "",
                "§eClick to create");
            gui.setItem(slot, newTemplate);
        }
    }
    
    private void addManagementTools(Inventory gui) {
        // Bulk Operations
        gui.setItem(36, createItem(Material.COMMAND_BLOCK, "§6§lBulk Operations",
            "§7Perform operations on multiple presets",
            "§7- Enable/Disable multiple presets",
            "§7- Bulk weight adjustment",
            "§7- Export/Import preset collections",
            "",
            "§eClick for bulk tools"));
        
        // Template Analytics
        gui.setItem(37, createItem(Material.BOOK, "§d§lTemplate Analytics",
            "§7View preset performance statistics",
            "§7- Usage frequency analysis",
            "§7- Success rate by template",
            "§7- Player preference data",
            "",
            "§eClick for analytics"));
        
        // Import/Export
        gui.setItem(38, createItem(Material.ENDER_CHEST, "§3§lImport/Export",
            "§7Manage preset collections",
            "§7- Export presets to file",
            "§7- Import community templates",
            "§7- Share preset configurations",
            "",
            "§eClick to manage"));
    }
    
    private void addNavigation(Inventory gui) {
        gui.setItem(45, createItem(Material.ARROW, "§7« Back to Admin Panel",
            "§7Return to the main admin dashboard"));
        
        gui.setItem(49, createItem(Material.LIME_CONCRETE, "§a§lRefresh Presets",
            "§7Reload all preset configurations",
            "§7and update the display",
            "",
            "§eClick to refresh"));
        
        gui.setItem(53, createItem(Material.BARRIER, "§c§lClose",
            "§7Close this interface"));
    }
    
    private ItemStack createPresetItem(EventPreset preset, boolean isTaskerPreset) {
        Material material = getPresetMaterial(preset.getType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String prefix = isTaskerPreset ? "§2[TASKER] " : "§b[CUSTOM] ";
        meta.setDisplayName(prefix + "§f" + preset.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Type: §f" + preset.getType().name().replace("_", " "));
        lore.add("§7Duration: §f" + preset.getFormattedDuration());
        lore.add("§7Max Players: §f" + (preset.hasUnlimitedParticipants() ? "Unlimited" : preset.getMaxParticipants()));
        lore.add("§7Weight: §f" + preset.getWeight());
        lore.add("§7Status: " + (preset.isEnabled() ? "§aEnabled" : "§cDisabled"));
        lore.add("");
        lore.add("§7Description:");
        
        // Word wrap description
        String[] words = preset.getDescription().split(" ");
        StringBuilder line = new StringBuilder("§7");
        for (String word : words) {
            if (line.length() + word.length() > 35) {
                lore.add(line.toString());
                line = new StringBuilder("§7" + word + " ");
            } else {
                line.append(word).append(" ");
            }
        }
        if (line.length() > 2) {
            lore.add(line.toString().trim());
        }
        
        lore.add("");
        if (preset.hasRewards()) {
            lore.add("§6§lRewards:");
            for (String reward : preset.getRewards()) {
                if (reward.length() > 35) {
                    lore.add("§6- " + reward.substring(0, 32) + "...");
                } else {
                    lore.add("§6- " + reward);
                }
            }
            lore.add("");
        }
        
        lore.add("§eLeft-click: §7Edit preset");
        lore.add("§eRight-click: §7Toggle enabled");
        lore.add("§eShift-click: §7Duplicate");
        lore.add("§eShift-right-click: §7Delete");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private Material getPresetMaterial(Event.EventType type) {
        return switch (type) {
            case PVP -> Material.DIAMOND_SWORD;
            case PVE -> Material.BOW;
            case BUILDING -> Material.GOLDEN_PICKAXE;
            case RACING -> Material.GOLDEN_BOOTS;
            case TREASURE_HUNT -> Material.COMPASS;
            case MINI_GAME -> Material.SLIME_BALL;
            case CUSTOM -> Material.COMMAND_BLOCK;
        };
    }
    
    /**
     * Open preset editor GUI
     */
    public void openPresetEditor(Player player, String presetId, boolean isTaskerPreset) {
        EventPreset preset = isTaskerPreset ? 
            plugin.getEventTasker().getPreset(presetId) : 
            customPresets.get(presetId);
        
        if (preset == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cPreset not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 45, "§e§lEdit Preset: " + preset.getName());
        
        // Basic Info
        gui.setItem(10, createItem(Material.NAME_TAG, "§6Name: §f" + preset.getName(),
            "§7Click to edit the preset name",
            "",
            "§eClick to modify"));
        
        gui.setItem(11, createItem(Material.BOOK, "§6Description",
            "§7Current: §f" + preset.getDescription(),
            "",
            "§eClick to modify"));
        
        gui.setItem(12, createItem(getPresetMaterial(preset.getType()), "§6Type: §f" + preset.getType().name(),
            "§7Click to change event type",
            "",
            "§eClick to modify"));
        
        // Settings
        gui.setItem(19, createItem(Material.CLOCK, "§6Duration: §f" + preset.getFormattedDuration(),
            "§7Left-click: +15 minutes",
            "§7Right-click: -15 minutes",
            "§7Shift-left: +5 minutes",
            "§7Shift-right: -5 minutes"));
        
        gui.setItem(20, createItem(Material.PLAYER_HEAD, "§6Max Players: §f" + 
            (preset.hasUnlimitedParticipants() ? "Unlimited" : preset.getMaxParticipants()),
            "§7Left-click: +5 players",
            "§7Right-click: -5 players",
            "§7Shift-click: Toggle unlimited"));
        
        gui.setItem(21, createItem(Material.ANVIL, "§6Weight: §f" + preset.getWeight(),
            "§7Higher weight = more likely",
            "§7to be selected by tasker",
            "",
            "§7Left-click: +1",
            "§7Right-click: -1"));
        
        // Status
        ItemStack statusItem = new ItemStack(preset.isEnabled() ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta statusMeta = statusItem.getItemMeta();
        statusMeta.setDisplayName("§6Status: " + (preset.isEnabled() ? "§aEnabled" : "§cDisabled"));
        statusMeta.setLore(Arrays.asList("§7Click to toggle", "", "§eClick to toggle"));
        statusItem.setItemMeta(statusMeta);
        gui.setItem(22, statusItem);
        
        // Rewards
        gui.setItem(28, createItem(Material.DIAMOND, "§6§lRewards (" + preset.getRewards().size() + ")",
            "§7Configure event rewards",
            "",
            "§eClick to edit rewards"));
        
        // Save/Cancel
        gui.setItem(35, createItem(Material.EMERALD_BLOCK, "§a§lSave Changes",
            "§7Save the preset configuration",
            "",
            "§eClick to save"));
        
        gui.setItem(36, createItem(Material.BARRIER, "§c§lCancel",
            "§7Discard changes and return",
            "",
            "§eClick to cancel"));
        
        // Delete
        gui.setItem(44, createItem(Material.LAVA_BUCKET, "§4§lDelete Preset",
            "§cPermanently remove this preset",
            "§cThis action cannot be undone!",
            "",
            "§eShift-click to confirm"));
        
        player.openInventory(gui);
    }
    
    /**
     * Create a new preset from an existing event
     */
    public void createPresetFromEvent(Player player, Event event) {
        String presetId = "custom_" + System.currentTimeMillis();
        
        EventPreset newPreset = new EventPreset(
            presetId,
            event.getName() + " Template",
            "Template created from: " + event.getName(),
            event.getType(),
            (int) ((event.getEndTime() - event.getStartTime()) / 1000),
            event.getMaxParticipants(),
            1, // Default min participants
            new ArrayList<>(event.getRewards()),
            true,
            5 // Default weight
        );
        
        customPresets.put(presetId, newPreset);
        saveCustomPresets();
        
        player.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aCreated new preset template: §f" + newPreset.getName());
        
        // Open the editor for immediate customization
        openPresetEditor(player, presetId, false);
    }
    
    /**
     * Bulk operations GUI
     */
    public void openBulkOperationsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, "§6§lBulk Preset Operations");
        
        // Enable All
        gui.setItem(10, createItem(Material.LIME_CONCRETE, "§a§lEnable All Presets",
            "§7Enable all preset templates",
            "§7for automatic selection",
            "",
            "§eClick to enable all"));
        
        // Disable All
        gui.setItem(11, createItem(Material.RED_CONCRETE, "§c§lDisable All Presets",
            "§7Disable all preset templates",
            "§7to stop automatic selection",
            "",
            "§eClick to disable all"));
        
        // Normalize Weights
        gui.setItem(12, createItem(Material.SCULK_SHRIEKER, "§e§lNormalize Weights",
            "§7Reset all preset weights to",
            "§7equal values for fair distribution",
            "",
            "§eClick to normalize"));
        
        // Export All
        gui.setItem(19, createItem(Material.ENDER_CHEST, "§b§lExport All Presets",
            "§7Export all presets to a",
            "§7configuration file for backup",
            "",
            "§eClick to export"));
        
        // Import Presets
        gui.setItem(20, createItem(Material.HOPPER, "§d§lImport Presets",
            "§7Import presets from a",
            "§7configuration file",
            "",
            "§eClick to import"));
        
        // Duplicate Popular
        gui.setItem(21, createItem(Material.PAPER, "§6§lDuplicate Popular",
            "§7Create variations of the",
            "§7most successful presets",
            "",
            "§eClick to duplicate"));
        
        // Analytics
        gui.setItem(28, createItem(Material.BOOK, "§9§lPreset Analytics",
            "§7View usage statistics and",
            "§7performance metrics",
            "",
            "§eClick for analytics"));
        
        // Back
        gui.setItem(36, createItem(Material.ARROW, "§7« Back to Preset Manager"));
        
        player.openInventory(gui);
    }
    
    private void loadCustomPresets() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_presets");
        if (section == null) return;
        
        for (String key : section.getKeys(false)) {
            ConfigurationSection presetSection = section.getConfigurationSection(key);
            if (presetSection == null) continue;
            
            try {
                EventPreset preset = new EventPreset(key, presetSection);
                customPresets.put(key, preset);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load custom preset '" + key + "': " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + customPresets.size() + " custom presets");
    }
    
    private void saveCustomPresets() {
        ConfigurationSection section = plugin.getConfig().createSection("custom_presets");
        
        for (Map.Entry<String, EventPreset> entry : customPresets.entrySet()) {
            EventPreset preset = entry.getValue();
            ConfigurationSection presetSection = section.createSection(entry.getKey());
            
            presetSection.set("name", preset.getName());
            presetSection.set("description", preset.getDescription());
            presetSection.set("type", preset.getType().name());
            presetSection.set("duration", preset.getDuration());
            presetSection.set("max_participants", preset.getMaxParticipants());
            presetSection.set("min_participants", preset.getMinParticipants());
            presetSection.set("rewards", preset.getRewards());
            presetSection.set("enabled", preset.isEnabled());
            presetSection.set("weight", preset.getWeight());
        }
        
        plugin.saveConfig();
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
    
    // Getters
    public Map<String, EventPreset> getCustomPresets() {
        return new HashMap<>(customPresets);
    }
    
    public EventPreset getCustomPreset(String id) {
        return customPresets.get(id);
    }
    
    public void removeCustomPreset(String id) {
        customPresets.remove(id);
        saveCustomPresets();
    }
    
    public void addCustomPreset(String id, EventPreset preset) {
        customPresets.put(id, preset);
        saveCustomPresets();
    }
} 