package com.swiftevents.gui;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.gui.GUISession.EventCreationStep;
import com.swiftevents.permissions.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AdminGUIManager {
    
    private final SwiftEventsPlugin plugin;
    private final GUIManager guiManager;
    
    public AdminGUIManager(SwiftEventsPlugin plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }
    
    public void openAdminGUI(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_BASE)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        GUISession session = guiManager.getOrCreateSession(player.getUniqueId());
        
        Inventory gui = Bukkit.createInventory(null, 54, "§4Admin Dashboard - SwiftEvents");
        
        // Event Management Section
        addEventManagementSection(gui);
        
        // Statistics Section
        addStatisticsSection(gui);
        
        // Configuration Section
        addConfigurationSection(gui);
        
        // Tasker Section
        addTaskerSection(gui);
        
        // User Management Section
        addUserManagementSection(gui);
        
        // Navigation and utilities
        addAdminNavigation(gui, player);
        
        player.openInventory(gui);
    }
    
    private void addEventManagementSection(Inventory gui) {
        // Create Event
        gui.setItem(10, createItem(Material.EMERALD_BLOCK, "§a§lCreate New Event", 
            "§7Start the event creation wizard",
            "§7to build a custom event with",
            "§7advanced settings and features",
            "",
            "§eClick to start wizard"));
        
        // Manage Events
        gui.setItem(11, createItem(Material.COMMAND_BLOCK, "§6§lManage Events",
            "§7View and manage all events",
            "§7- Start/Stop events",
            "§7- Edit event settings",
            "§7- View participants",
            "",
            "§eClick to manage"));
        
        // Active Events
        List<Event> activeEvents = plugin.getEventManager().getActiveEvents();
        ItemStack activeEventsItem = new ItemStack(Material.CLOCK);
        ItemMeta activeMeta = activeEventsItem.getItemMeta();
        activeMeta.setDisplayName("§c§lActive Events");
        List<String> activeLore = new ArrayList<>();
        activeLore.add("§7Currently active events: §f" + activeEvents.size());
        activeLore.add("");
        if (activeEvents.isEmpty()) {
            activeLore.add("§7No active events");
        } else {
            activeLore.add("§7Active events:");
            for (int i = 0; i < Math.min(5, activeEvents.size()); i++) {
                Event event = activeEvents.get(i);
                activeLore.add("§f- " + event.getName() + " §7(" + event.getCurrentParticipants() + " players)");
            }
            if (activeEvents.size() > 5) {
                activeLore.add("§7... and " + (activeEvents.size() - 5) + " more");
            }
        }
        activeLore.add("");
        activeLore.add("§eClick to view details");
        activeMeta.setLore(activeLore);
        activeEventsItem.setItemMeta(activeMeta);
        gui.setItem(12, activeEventsItem);
        
        // Bulk Operations
        gui.setItem(13, createItem(Material.TNT, "§c§lBulk Operations",
            "§7Perform operations on multiple events",
            "§7- Start/Stop multiple events",
            "§7- Delete events in bulk",
            "§7- Modify settings across events",
            "§7- Export/Import event data",
            "",
            "§eClick for bulk tools"));
    }
    
    private void addStatisticsSection(Inventory gui) {
        // Event Statistics
        gui.setItem(19, createItem(Material.BOOK, "§b§lEvent Statistics",
            "§7View comprehensive event analytics",
            "§7- Event performance metrics",
            "§7- Player participation data",
            "§7- Historical trends",
            "",
            "§eClick to view analytics"));
        
        // Server Performance & Debug Dashboard
        gui.setItem(20, createItem(Material.REDSTONE, "§e§lPerformance Dashboard",
            "§7Advanced monitoring and debugging",
            "§7- Memory usage analysis",
            "§7- Database performance metrics",
            "§7- Event processing speed",
            "§7- Debug logging controls",
            "",
            "§eClick to open dashboard"));
        
        // Player Statistics
        gui.setItem(21, createItem(Material.PLAYER_HEAD, "§d§lPlayer Statistics",
            "§7View player engagement data",
            "§7- Top participants",
            "§7- Event preferences",
            "§7- Activity patterns",
            "",
            "§eClick to view details"));
    }
    
    private void addConfigurationSection(Inventory gui) {
        // Plugin Settings
        gui.setItem(28, createItem(Material.COMPARATOR, "§9§lPlugin Settings",
            "§7Configure plugin settings",
            "§7- GUI preferences",
            "§7- Database settings",
            "§7- Event defaults",
            "",
            "§eClick to configure"));
        
        // Permissions
        gui.setItem(29, createItem(Material.NAME_TAG, "§5§lPermission Manager",
            "§7Manage player permissions",
            "§7- Event type access",
            "§7- Admin privileges",
            "§7- Bypass permissions",
            "",
            "§eClick to manage"));
        
        // Backup & Restore
        gui.setItem(30, createItem(Material.ENDER_CHEST, "§3§lAdvanced Backup System",
            "§7Comprehensive data protection",
            "§7- Automatic scheduled backups",
            "§7- Manual backup creation",
            "§7- Selective restore options",
            "§7- Import/Export functionality",
            "§7- Compression and encryption",
            "",
            "§eClick to manage backups"));
    }
    
    private void addTaskerSection(Inventory gui) {
        // Event Tasker
        boolean taskerEnabled = plugin.getConfigManager().isEventTaskerEnabled();
        ItemStack tasker = new ItemStack(taskerEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE);
        ItemMeta taskerMeta = tasker.getItemMeta();
        taskerMeta.setDisplayName("§2§lEvent Tasker " + (taskerEnabled ? "§a[ON]" : "§c[OFF]"));
        List<String> taskerLore = new ArrayList<>();
        taskerLore.add("§7Automated event scheduling");
        taskerLore.add("§7Status: " + (taskerEnabled ? "§aEnabled" : "§cDisabled"));
        if (taskerEnabled) {
            taskerLore.add("§7Next event in: §f" + getNextEventTime());
        }
        taskerLore.add("");
        taskerLore.add("§eClick to configure");
        taskerMeta.setLore(taskerLore);
        tasker.setItemMeta(taskerMeta);
        gui.setItem(37, tasker);
        
        // Event Presets & Templates Manager
        gui.setItem(38, createItem(Material.WRITABLE_BOOK, "§a§lEvent Templates Manager",
            "§7Advanced preset and template system",
            "§7- Create templates from events",
            "§7- Edit existing presets",
            "§7- Set automation weights",
            "§7- Import/Export templates",
            "§7- Template analytics",
            "",
            "§eClick to open manager"));
    }
    
    private void addUserManagementSection(Inventory gui) {
        // Online Players
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        ItemStack playersItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playersMeta = playersItem.getItemMeta();
        playersMeta.setDisplayName("§f§lOnline Players");
        playersMeta.setLore(Arrays.asList(
            "§7Currently online: §f" + onlinePlayers,
            "§7Players in events: §f" + getPlayersInEvents(),
            "",
            "§eClick to view details"
        ));
        playersItem.setItemMeta(playersMeta);
        gui.setItem(46, playersItem);
        
        // Banned/Restricted Users
        gui.setItem(47, createItem(Material.BARRIER, "§c§lUser Restrictions",
            "§7Manage player restrictions",
            "§7- Event bans",
            "§7- Cooldown overrides",
            "§7- Special permissions",
            "",
            "§eClick to manage"));
    }
    
    private void addAdminNavigation(Inventory gui, Player player) {
        // Refresh
        gui.setItem(49, createItem(Material.LIME_CONCRETE, "§a§lRefresh",
            "§7Update all information",
            "§7and reload data",
            "",
            "§eClick to refresh"));
        
        // Return to Events
        gui.setItem(53, createItem(Material.ARROW, "§7« Back to Events",
            "§7Return to the main events GUI"));
        
        // Help/Documentation
        gui.setItem(45, createItem(Material.BOOK, "§e§lHelp & Documentation",
            "§7View command help and",
            "§7administration guides",
            "",
            "§eClick for help"));
    }
    
    // Event Creation Wizard
    public void openEventCreationWizard(Player player) {
        GUISession session = guiManager.getOrCreateSession(player.getUniqueId());
        session.clearCreationData();
        openEventCreationStep(player, EventCreationStep.TYPE_SELECTION);
    }
    
    public void openEventCreationStep(Player player, EventCreationStep step) {
        GUISession session = guiManager.getOrCreateSession(player.getUniqueId());
        session.setCreationStep(step);
        
        switch (step) {
            case TYPE_SELECTION -> openTypeSelectionGUI(player, session);
            case BASIC_INFO -> openBasicInfoGUI(player, session);
            case SETTINGS -> openSettingsGUI(player, session);
            case LOCATION -> openLocationGUI(player, session);
            case REWARDS -> openRewardsGUI(player, session);
            case CONFIRMATION -> openConfirmationGUI(player, session);
        }
    }
    
    private void openTypeSelectionGUI(Player player, GUISession session) {
        Inventory gui = Bukkit.createInventory(null, 45, "§6Event Creation - Select Type");
        
        // Add event type options
        Event.EventType[] types = Event.EventType.values();
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        
        for (int i = 0; i < types.length && i < slots.length; i++) {
            Event.EventType type = types[i];
            ItemStack item = new ItemStack(getEventTypeMaterial(type));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + type.name().replace("_", " "));
            meta.setLore(Arrays.asList(
                "§7" + getEventTypeDescription(type),
                "",
                "§eClick to select"
            ));
            item.setItemMeta(meta);
            gui.setItem(slots[i], item);
        }
        
        addWizardNavigation(gui, session, false, false);
        player.openInventory(gui);
    }
    
    private void openBasicInfoGUI(Player player, GUISession session) {
        Inventory gui = Bukkit.createInventory(null, 45, "§6Event Creation - Basic Info");
        
        // Show current selections
        Event.EventType selectedType = (Event.EventType) session.getCreationData("type");
        
        ItemStack typeItem = new ItemStack(getEventTypeMaterial(selectedType));
        ItemMeta typeMeta = typeItem.getItemMeta();
        typeMeta.setDisplayName("§aSelected Type: §f" + selectedType.name().replace("_", " "));
        typeMeta.setLore(Arrays.asList("§7Event type has been selected"));
        typeItem.setItemMeta(typeMeta);
        gui.setItem(11, typeItem);
        
        // Name input placeholder
        String currentName = (String) session.getCreationData("name");
        if (currentName == null) currentName = "Click to set name";
        
        gui.setItem(13, createItem(Material.NAME_TAG, "§eEvent Name",
            "§7Click to set event name",
            "§7Current: §f" + currentName,
            "",
            "§8Type in chat after clicking",
            "§eClick to edit"));
        
        // Description input placeholder
        String currentDesc = (String) session.getCreationData("description");
        if (currentDesc == null) currentDesc = "Click to set description";
        
        gui.setItem(15, createItem(Material.WRITABLE_BOOK, "§eEvent Description",
            "§7Click to set description",
            "§7Current: §f" + currentDesc,
            "",
            "§8Type in chat after clicking",
            "§eClick to edit"));
        
        // Check if we can proceed
        boolean canProceed = currentName != null && !currentName.equals("Click to set name") &&
                           currentDesc != null && !currentDesc.equals("Click to set description");
        
        addWizardNavigation(gui, session, true, canProceed);
        player.openInventory(gui);
    }
    
    private void openSettingsGUI(Player player, GUISession session) {
        Inventory gui = Bukkit.createInventory(null, 45, "§6Event Creation - Settings");
        
        // Max participants setting
        Integer maxParticipants = (Integer) session.getCreationData("maxParticipants");
        if (maxParticipants == null) maxParticipants = 20;
        
        gui.setItem(11, createItem(Material.PLAYER_HEAD, "§eMax Participants",
            "§7Maximum number of players",
            "§7Current: §f" + maxParticipants + " players",
            "",
            "§7Left click: +5 players",
            "§7Right click: -5 players",
            "§7Shift + Left click: +1 player",
            "§7Shift + Right click: -1 player"));
        
        // Duration setting
        Integer duration = (Integer) session.getCreationData("duration");
        if (duration == null) duration = 1800; // 30 minutes default
        
        gui.setItem(13, createItem(Material.CLOCK, "§eEvent Duration",
            "§7How long the event runs",
            "§7Current: §f" + formatDuration(duration),
            "",
            "§7Left click: +15 minutes",
            "§7Right click: -15 minutes",
            "§7Shift + Left click: +5 minutes",
            "§7Shift + Right click: -5 minutes"));
        
        // Auto-start setting
        Boolean autoStart = (Boolean) session.getCreationData("autoStart");
        if (autoStart == null) autoStart = false;
        
        ItemStack autoStartItem = new ItemStack(autoStart ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta autoStartMeta = autoStartItem.getItemMeta();
        autoStartMeta.setDisplayName("§eAuto Start");
        autoStartMeta.setLore(Arrays.asList(
            "§7Automatically start when ready",
            "§7Current: " + (autoStart ? "§aEnabled" : "§cDisabled"),
            "",
            "§eClick to toggle"
        ));
        autoStartItem.setItemMeta(autoStartMeta);
        gui.setItem(15, autoStartItem);
        
        // Minimum participants
        Integer minParticipants = (Integer) session.getCreationData("minParticipants");
        if (minParticipants == null) minParticipants = 2;
        
        gui.setItem(20, createItem(Material.BARRIER, "§eMin Participants",
            "§7Minimum players to start event",
            "§7Current: §f" + minParticipants + " players",
            "",
            "§7Left click: +1 player",
            "§7Right click: -1 player"));
        
        addWizardNavigation(gui, session, true, true);
        player.openInventory(gui);
    }
    
    private void openLocationGUI(Player player, GUISession session) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6Event Creation - Location");
        
        // Current location setting
        String locationWorld = (String) session.getCreationData("locationWorld");
        String presetLocationName = (String) session.getCreationData("presetLocationName");
        Boolean hasLocation = locationWorld != null || presetLocationName != null;
        
        ItemStack currentLocItem = new ItemStack(Material.COMPASS);
        ItemMeta currentLocMeta = currentLocItem.getItemMeta();
        currentLocMeta.setDisplayName("§eCurrent Location");
        if (hasLocation) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Event location is set");
            
            if (presetLocationName != null) {
                lore.add("§7Type: §aPreset Location");
                lore.add("§7Name: §f" + presetLocationName);
                // Try to get coordinates from preset
                plugin.getLocationManager().getPresetLocation(presetLocationName).ifPresent(preset -> {
                    lore.add("§7World: §f" + preset.location().getWorld().getName());
                    lore.add("§7Coordinates: §f" + 
                        Math.round(preset.location().getX()) + ", " +
                        Math.round(preset.location().getY()) + ", " +
                        Math.round(preset.location().getZ()));
                });
            } else {
                lore.add("§7Type: §bCustom Location");
                lore.add("§7World: §f" + locationWorld);
                lore.add("§7Coordinates: §f" + 
                    Math.round((Double) session.getCreationData("locationX")) + ", " +
                    Math.round((Double) session.getCreationData("locationY")) + ", " +
                    Math.round((Double) session.getCreationData("locationZ")));
            }
            lore.add("");
            lore.add("§aLocation configured ✓");
            currentLocMeta.setLore(lore);
        } else {
            currentLocMeta.setLore(Arrays.asList(
                "§7No location set",
                "§cChoose a location type below"
            ));
        }
        currentLocItem.setItemMeta(currentLocMeta);
        gui.setItem(13, currentLocItem);
        
        // Location type selection
        gui.setItem(19, createItem(Material.ENDER_PEARL, "§bSet Custom Location",
            "§7Set event location to where",
            "§7you are currently standing",
            "",
            "§eClick to set current position"));
        
        gui.setItem(21, createItem(Material.LODESTONE, "§aChoose Preset Location",
            "§7Select from saved preset locations",
            "§7configured by administrators",
            "",
            "§eClick to browse presets"));
        
        // Show available preset locations (first 5)
        Set<String> presetNames = plugin.getLocationManager().getPresetLocationNames();
        if (!presetNames.isEmpty()) {
            ItemStack presetListItem = new ItemStack(Material.MAP);
            ItemMeta presetListMeta = presetListItem.getItemMeta();
            presetListMeta.setDisplayName("§eAvailable Presets");
            List<String> presetLore = new ArrayList<>();
            presetLore.add("§7Saved preset locations:");
            presetLore.add("");
            
            int count = 0;
            for (String name : presetNames) {
                if (count >= 5) {
                    presetLore.add("§7... and " + (presetNames.size() - 5) + " more");
                    break;
                }
                presetLore.add("§a• §f" + name);
                count++;
            }
            presetLore.add("");
            presetLore.add("§7Click §aChoose Preset Location §7to select");
            presetListMeta.setLore(presetLore);
            presetListItem.setItemMeta(presetListMeta);
            gui.setItem(23, presetListItem);
        }
        
        // Clear location button
        if (hasLocation) {
            gui.setItem(25, createItem(Material.BARRIER, "§cClear Location",
                "§7Remove the set location",
                "",
                "§eClick to clear"));
        }
        
        // Teleport to location button (if location is set)
        if (hasLocation) {
            gui.setItem(31, createItem(Material.ELYTRA, "§dTeleport to Location",
                "§7Teleport to the current",
                "§7event location to preview it",
                "",
                "§eClick to teleport"));
        }
        
        addWizardNavigation(gui, session, true, true);
        player.openInventory(gui);
    }
    
    public void openPresetLocationSelectionGUI(Player player, GUISession session) {
        Set<String> presetNames = plugin.getLocationManager().getPresetLocationNames();
        if (presetNames.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cNo preset locations are available! Ask an administrator to create some.");
            openLocationGUI(player, session);
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§6Select Preset Location");
        
        // Convert to list for indexing
        List<String> presetList = new ArrayList<>(presetNames);
        
        // Display preset locations (max 45 slots for locations)
        for (int i = 0; i < Math.min(45, presetList.size()); i++) {
            String presetName = presetList.get(i);
            final int slot = i; // Final variable for lambda
            
            plugin.getLocationManager().getPresetLocation(presetName).ifPresent(preset -> {
                ItemStack presetItem = new ItemStack(Material.BEACON);
                ItemMeta presetMeta = presetItem.getItemMeta();
                presetMeta.setDisplayName("§a" + presetName);
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Location: §f" + preset.location().getWorld().getName());
                lore.add("§7Coordinates: §f" + 
                    Math.round(preset.location().getX()) + ", " +
                    Math.round(preset.location().getY()) + ", " +
                    Math.round(preset.location().getZ()));
                lore.add("");
                lore.add("§eClick to select this location");
                lore.add("§7Shift+Click to teleport for preview");
                
                presetMeta.setLore(lore);
                presetItem.setItemMeta(presetMeta);
                gui.setItem(slot, presetItem);
            });
        }
        
        // Back button
        gui.setItem(49, createItem(Material.ARROW, "§7« Back to Location Selection",
            "§7Return to location options"));
        
        player.openInventory(gui);
    }
    
    private void openRewardsGUI(Player player, GUISession session) {
        Inventory gui = Bukkit.createInventory(null, 45, "§6Event Creation - Rewards");
        
        // Reward commands list
        @SuppressWarnings("unchecked")
        List<String> rewardCommands = (List<String>) session.getCreationData("rewardCommands");
        if (rewardCommands == null) {
            rewardCommands = new ArrayList<>();
            session.setCreationData("rewardCommands", rewardCommands);
        }
        
        // Add reward command button
        gui.setItem(11, createItem(Material.EMERALD, "§aAdd Reward Command",
            "§7Add a command to run for winners",
            "§7Use {winner} for player name",
            "",
            "§7Examples:",
            "§8give {winner} diamond 5",
            "§8eco give {winner} 1000",
            "",
            "§eClick to add command"));
        
        // Display current rewards
        ItemStack rewardsListItem = new ItemStack(Material.CHEST);
        ItemMeta rewardsListMeta = rewardsListItem.getItemMeta();
        rewardsListMeta.setDisplayName("§eCurrent Rewards");
        List<String> rewardsLore = new ArrayList<>();
        rewardsLore.add("§7Commands to run for winners:");
        rewardsLore.add("");
        
        if (rewardCommands.isEmpty()) {
            rewardsLore.add("§7No rewards configured");
        } else {
            for (int i = 0; i < Math.min(5, rewardCommands.size()); i++) {
                rewardsLore.add("§f" + (i + 1) + ". §7" + rewardCommands.get(i));
            }
            if (rewardCommands.size() > 5) {
                rewardsLore.add("§7... and " + (rewardCommands.size() - 5) + " more");
            }
        }
        rewardsLore.add("");
        rewardsLore.add("§eClick to manage rewards");
        rewardsListMeta.setLore(rewardsLore);
        rewardsListItem.setItemMeta(rewardsListMeta);
        gui.setItem(13, rewardsListItem);
        
        // Clear rewards button
        if (!rewardCommands.isEmpty()) {
            gui.setItem(15, createItem(Material.LAVA_BUCKET, "§cClear All Rewards",
                "§7Remove all reward commands",
                "",
                "§eClick to clear"));
        }
        
        addWizardNavigation(gui, session, true, true);
        player.openInventory(gui);
    }
    
    private void openConfirmationGUI(Player player, GUISession session) {
        Inventory gui = Bukkit.createInventory(null, 45, "§6Event Creation - Confirmation");
        
        // Event summary
        Event.EventType type = (Event.EventType) session.getCreationData("type");
        String name = (String) session.getCreationData("name");
        String description = (String) session.getCreationData("description");
        Integer maxParticipants = (Integer) session.getCreationData("maxParticipants");
        Integer duration = (Integer) session.getCreationData("duration");
        Boolean autoStart = (Boolean) session.getCreationData("autoStart");
        if (autoStart == null) {
            autoStart = false;
        }
        
        ItemStack summaryItem = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta summaryMeta = summaryItem.getItemMeta();
        summaryMeta.setDisplayName("§eEvent Summary");
        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("§7Review your event details:");
        summaryLore.add("");
        summaryLore.add("§7Name: §f" + (name != null ? name : "Not set"));
        summaryLore.add("§7Type: §f" + (type != null ? type.name().replace("_", " ") : "Not set"));
        summaryLore.add("§7Description: §f" + (description != null ? description : "Not set"));
        summaryLore.add("§7Max Players: §f" + (maxParticipants != null ? maxParticipants : "Unlimited"));
        summaryLore.add("§7Duration: §f" + formatDuration(duration));
        summaryLore.add("§7Auto Start: " + (autoStart ? "§aYes" : "§cNo"));
        
        String locationWorld = (String) session.getCreationData("locationWorld");
        String presetLocationName = (String) session.getCreationData("presetLocationName");
        if (presetLocationName != null) {
            summaryLore.add("§7Location: §aPreset (" + presetLocationName + ")");
        } else if (locationWorld != null) {
            summaryLore.add("§7Location: §bCustom (" + locationWorld + ")");
        } else {
            summaryLore.add("§7Location: §cNot set");
        }
        
        @SuppressWarnings("unchecked")
        List<String> rewards = (List<String>) session.getCreationData("rewardCommands");
        summaryLore.add("§7Rewards: §f" + (rewards != null ? rewards.size() : 0) + " commands");
        
        summaryMeta.setLore(summaryLore);
        summaryItem.setItemMeta(summaryMeta);
        gui.setItem(13, summaryItem);
        
        // Create event button
        gui.setItem(29, createItem(Material.LIME_CONCRETE, "§a§lCreate Event",
            "§7Create the event with these settings",
            "",
            "§aClick to create!"));
        
        // Cancel button
        gui.setItem(33, createItem(Material.RED_CONCRETE, "§c§lCancel Creation",
            "§7Cancel event creation and",
            "§7return to admin panel",
            "",
            "§cClick to cancel"));
        
        addWizardNavigation(gui, session, true, false);
        player.openInventory(gui);
    }
    
    // Helper methods
    private void addWizardNavigation(Inventory gui, GUISession session, boolean canPrevious, boolean canNext) {
        if (canPrevious) {
            gui.setItem(36, createItem(Material.ARROW, "§7« Previous Step"));
        }
        
        if (canNext) {
            gui.setItem(44, createItem(Material.ARROW, "§aNext Step »"));
        }
        
        // Cancel button
        gui.setItem(40, createItem(Material.BARRIER, "§cCancel Creation"));
    }
    
    // Helper method to create items with lore
    private ItemStack createItem(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
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
        };
    }
    
    private String getEventTypeDescription(Event.EventType type) {
        return switch (type) {
            case PVP -> "Player vs Player combat events";
            case PVE -> "Player vs Environment challenges";
            case BUILDING -> "Creative building competitions";
            case RACING -> "Speed and agility challenges";
            case TREASURE_HUNT -> "Exploration and discovery events";
            case MINI_GAME -> "Fun mini-games and activities";
            case CUSTOM -> "Fully customizable event type";
        };
    }
    
    private String getNextEventTime() {
        // This would integrate with the event tasker to get actual next event time
        return "15 minutes";
    }
    
    private int getPlayersInEvents() {
        return plugin.getEventManager().getAllEvents().stream()
                .mapToInt(Event::getCurrentParticipants)
                .sum();
    }
    
    private String formatDuration(Integer seconds) {
        if (seconds == null) {
            return "Not set";
        }
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d minutes %02d seconds", minutes, remainingSeconds);
    }
} 