package com.swiftevents.gui;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.gui.GUISession.AdminGUIPage;
import com.swiftevents.gui.GUISession.EventCreationStep;
import com.swiftevents.permissions.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

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
        session.setAdminPage(AdminGUIPage.MAIN);
        
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
        ItemStack createEvent = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createEvent.getItemMeta();
        createMeta.setDisplayName("§a§lCreate New Event");
        createMeta.setLore(Arrays.asList(
            "§7Start the event creation wizard",
            "§7to build a custom event with",
            "§7advanced settings and features",
            "",
            "§eClick to start wizard"
        ));
        createEvent.setItemMeta(createMeta);
        gui.setItem(10, createEvent);
        
        // Manage Events
        ItemStack manageEvents = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta manageMeta = manageEvents.getItemMeta();
        manageMeta.setDisplayName("§6§lManage Events");
        manageMeta.setLore(Arrays.asList(
            "§7View and manage all events",
            "§7- Start/Stop events",
            "§7- Edit event settings",
            "§7- View participants",
            "",
            "§eClick to manage"
        ));
        manageEvents.setItemMeta(manageMeta);
        gui.setItem(11, manageEvents);
        
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
    }
    
    private void addStatisticsSection(Inventory gui) {
        // Event Statistics
        ItemStack eventStats = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = eventStats.getItemMeta();
        statsMeta.setDisplayName("§b§lEvent Statistics");
        statsMeta.setLore(Arrays.asList(
            "§7View comprehensive event analytics",
            "§7- Event performance metrics",
            "§7- Player participation data",
            "§7- Historical trends",
            "",
            "§eClick to view analytics"
        ));
        eventStats.setItemMeta(statsMeta);
        gui.setItem(19, eventStats);
        
        // Server Performance
        ItemStack serverPerf = new ItemStack(Material.REDSTONE);
        ItemMeta perfMeta = serverPerf.getItemMeta();
        perfMeta.setDisplayName("§e§lServer Performance");
        perfMeta.setLore(Arrays.asList(
            "§7Monitor plugin performance",
            "§7- Memory usage",
            "§7- Database performance",
            "§7- Event processing speed",
            "",
            "§eClick to view metrics"
        ));
        serverPerf.setItemMeta(perfMeta);
        gui.setItem(20, serverPerf);
        
        // Player Statistics
        ItemStack playerStats = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playerMeta = playerStats.getItemMeta();
        playerMeta.setDisplayName("§d§lPlayer Statistics");
        playerMeta.setLore(Arrays.asList(
            "§7View player engagement data",
            "§7- Top participants",
            "§7- Event preferences",
            "§7- Activity patterns",
            "",
            "§eClick to view details"
        ));
        playerStats.setItemMeta(playerMeta);
        gui.setItem(21, playerStats);
    }
    
    private void addConfigurationSection(Inventory gui) {
        // Plugin Settings
        ItemStack settings = new ItemStack(Material.COMPARATOR);
        ItemMeta settingsMeta = settings.getItemMeta();
        settingsMeta.setDisplayName("§9§lPlugin Settings");
        settingsMeta.setLore(Arrays.asList(
            "§7Configure plugin settings",
            "§7- GUI preferences",
            "§7- Database settings",
            "§7- Event defaults",
            "",
            "§eClick to configure"
        ));
        settings.setItemMeta(settingsMeta);
        gui.setItem(28, settings);
        
        // Permissions
        ItemStack permissions = new ItemStack(Material.NAME_TAG);
        ItemMeta permMeta = permissions.getItemMeta();
        permMeta.setDisplayName("§5§lPermission Manager");
        permMeta.setLore(Arrays.asList(
            "§7Manage player permissions",
            "§7- Event type access",
            "§7- Admin privileges",
            "§7- Bypass permissions",
            "",
            "§eClick to manage"
        ));
        permissions.setItemMeta(permMeta);
        gui.setItem(29, permissions);
        
        // Backup & Restore
        ItemStack backup = new ItemStack(Material.ENDER_CHEST);
        ItemMeta backupMeta = backup.getItemMeta();
        backupMeta.setDisplayName("§3§lBackup & Restore");
        backupMeta.setLore(Arrays.asList(
            "§7Manage data backups",
            "§7- Create manual backup",
            "§7- Restore from backup",
            "§7- Schedule automatic backups",
            "",
            "§eClick to manage"
        ));
        backup.setItemMeta(backupMeta);
        gui.setItem(30, backup);
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
        
        // Event Presets
        ItemStack presets = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta presetsMeta = presets.getItemMeta();
        presetsMeta.setDisplayName("§a§lEvent Presets");
        presetsMeta.setLore(Arrays.asList(
            "§7Manage event templates",
            "§7- Create new presets",
            "§7- Edit existing templates",
            "§7- Set preset weights",
            "",
            "§eClick to manage"
        ));
        presets.setItemMeta(presetsMeta);
        gui.setItem(38, presets);
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
        ItemStack restrictions = new ItemStack(Material.BARRIER);
        ItemMeta restrictMeta = restrictions.getItemMeta();
        restrictMeta.setDisplayName("§c§lUser Restrictions");
        restrictMeta.setLore(Arrays.asList(
            "§7Manage player restrictions",
            "§7- Event bans",
            "§7- Cooldown overrides",
            "§7- Special permissions",
            "",
            "§eClick to manage"
        ));
        restrictions.setItemMeta(restrictMeta);
        gui.setItem(47, restrictions);
    }
    
    private void addAdminNavigation(Inventory gui, Player player) {
        // Refresh
        ItemStack refresh = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.setDisplayName("§a§lRefresh");
        refreshMeta.setLore(Arrays.asList(
            "§7Update all information",
            "§7and reload data",
            "",
            "§eClick to refresh"
        ));
        refresh.setItemMeta(refreshMeta);
        gui.setItem(49, refresh);
        
        // Return to Events
        ItemStack returnItem = new ItemStack(Material.ARROW);
        ItemMeta returnMeta = returnItem.getItemMeta();
        returnMeta.setDisplayName("§7« Back to Events");
        returnMeta.setLore(Arrays.asList("§7Return to the main events GUI"));
        returnItem.setItemMeta(returnMeta);
        gui.setItem(53, returnItem);
        
        // Help/Documentation
        ItemStack help = new ItemStack(Material.BOOK);
        ItemMeta helpMeta = help.getItemMeta();
        helpMeta.setDisplayName("§e§lHelp & Documentation");
        helpMeta.setLore(Arrays.asList(
            "§7View command help and",
            "§7administration guides",
            "",
            "§eClick for help"
        ));
        help.setItemMeta(helpMeta);
        gui.setItem(45, help);
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
        
        ItemStack nameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = nameItem.getItemMeta();
        nameMeta.setDisplayName("§eEvent Name");
        nameMeta.setLore(Arrays.asList(
            "§7Click to set event name",
            "§7Current: §f" + currentName,
            "",
            "§8Type in chat after clicking",
            "§eClick to edit"
        ));
        nameItem.setItemMeta(nameMeta);
        gui.setItem(13, nameItem);
        
        // Description input placeholder
        String currentDesc = (String) session.getCreationData("description");
        if (currentDesc == null) currentDesc = "Click to set description";
        
        ItemStack descItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta descMeta = descItem.getItemMeta();
        descMeta.setDisplayName("§eEvent Description");
        descMeta.setLore(Arrays.asList(
            "§7Click to set description",
            "§7Current: §f" + currentDesc,
            "",
            "§8Type in chat after clicking",
            "§eClick to edit"
        ));
        descItem.setItemMeta(descMeta);
        gui.setItem(15, descItem);
        
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
        
        ItemStack maxPlayersItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta maxPlayersMeta = maxPlayersItem.getItemMeta();
        maxPlayersMeta.setDisplayName("§eMax Participants");
        maxPlayersMeta.setLore(Arrays.asList(
            "§7Maximum number of players",
            "§7Current: §f" + maxParticipants + " players",
            "",
            "§7Left click: +5 players",
            "§7Right click: -5 players",
            "§7Shift + Left click: +1 player",
            "§7Shift + Right click: -1 player"
        ));
        maxPlayersItem.setItemMeta(maxPlayersMeta);
        gui.setItem(11, maxPlayersItem);
        
        // Duration setting
        Integer duration = (Integer) session.getCreationData("duration");
        if (duration == null) duration = 1800; // 30 minutes default
        
        ItemStack durationItem = new ItemStack(Material.CLOCK);
        ItemMeta durationMeta = durationItem.getItemMeta();
        durationMeta.setDisplayName("§eEvent Duration");
        durationMeta.setLore(Arrays.asList(
            "§7How long the event runs",
            "§7Current: §f" + formatDuration(duration),
            "",
            "§7Left click: +15 minutes",
            "§7Right click: -15 minutes",
            "§7Shift + Left click: +5 minutes",
            "§7Shift + Right click: -5 minutes"
        ));
        durationItem.setItemMeta(durationMeta);
        gui.setItem(13, durationItem);
        
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
        
        ItemStack minPlayersItem = new ItemStack(Material.BARRIER);
        ItemMeta minPlayersMeta = minPlayersItem.getItemMeta();
        minPlayersMeta.setDisplayName("§eMin Participants");
        minPlayersMeta.setLore(Arrays.asList(
            "§7Minimum players to start event",
            "§7Current: §f" + minParticipants + " players",
            "",
            "§7Left click: +1 player",
            "§7Right click: -1 player"
        ));
        minPlayersItem.setItemMeta(minPlayersMeta);
        gui.setItem(20, minPlayersItem);
        
        addWizardNavigation(gui, session, true, true);
        player.openInventory(gui);
    }
    
    private void openLocationGUI(Player player, GUISession session) {
        Inventory gui = Bukkit.createInventory(null, 45, "§6Event Creation - Location");
        
        // Current location setting
        String locationWorld = (String) session.getCreationData("locationWorld");
        Boolean hasLocation = locationWorld != null;
        
        ItemStack currentLocItem = new ItemStack(Material.COMPASS);
        ItemMeta currentLocMeta = currentLocItem.getItemMeta();
        currentLocMeta.setDisplayName("§eCurrent Location");
        if (hasLocation) {
            currentLocMeta.setLore(Arrays.asList(
                "§7Event location is set",
                "§7World: §f" + locationWorld,
                "§7Coordinates: §f" + 
                    Math.round((Double) session.getCreationData("locationX")) + ", " +
                    Math.round((Double) session.getCreationData("locationY")) + ", " +
                    Math.round((Double) session.getCreationData("locationZ")),
                "",
                "§aLocation configured ✓"
            ));
        } else {
            currentLocMeta.setLore(Arrays.asList(
                "§7No location set",
                "§cClick 'Set to My Location' to configure"
            ));
        }
        currentLocItem.setItemMeta(currentLocMeta);
        gui.setItem(11, currentLocItem);
        
        // Set to current location button
        ItemStack setLocationItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta setLocationMeta = setLocationItem.getItemMeta();
        setLocationMeta.setDisplayName("§aSet to My Location");
        setLocationMeta.setLore(Arrays.asList(
            "§7Set event location to where",
            "§7you are currently standing",
            "",
            "§eClick to set location"
        ));
        setLocationItem.setItemMeta(setLocationMeta);
        gui.setItem(13, setLocationItem);
        
        // Clear location button
        if (hasLocation) {
            ItemStack clearLocationItem = new ItemStack(Material.BARRIER);
            ItemMeta clearLocationMeta = clearLocationItem.getItemMeta();
            clearLocationMeta.setDisplayName("§cClear Location");
            clearLocationMeta.setLore(Arrays.asList(
                "§7Remove the set location",
                "",
                "§eClick to clear"
            ));
            clearLocationItem.setItemMeta(clearLocationMeta);
            gui.setItem(15, clearLocationItem);
        }
        
        addWizardNavigation(gui, session, true, true);
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
        ItemStack addRewardItem = new ItemStack(Material.EMERALD);
        ItemMeta addRewardMeta = addRewardItem.getItemMeta();
        addRewardMeta.setDisplayName("§aAdd Reward Command");
        addRewardMeta.setLore(Arrays.asList(
            "§7Add a command to run for winners",
            "§7Use {winner} for player name",
            "",
            "§7Examples:",
            "§8give {winner} diamond 5",
            "§8eco give {winner} 1000",
            "",
            "§eClick to add command"
        ));
        addRewardItem.setItemMeta(addRewardMeta);
        gui.setItem(11, addRewardItem);
        
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
            ItemStack clearRewardsItem = new ItemStack(Material.LAVA_BUCKET);
            ItemMeta clearRewardsMeta = clearRewardsItem.getItemMeta();
            clearRewardsMeta.setDisplayName("§cClear All Rewards");
            clearRewardsMeta.setLore(Arrays.asList(
                "§7Remove all reward commands",
                "",
                "§eClick to clear"
            ));
            clearRewardsItem.setItemMeta(clearRewardsMeta);
            gui.setItem(15, clearRewardsItem);
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
        summaryLore.add("§7Name: §f" + name);
        summaryLore.add("§7Type: §f" + type.name().replace("_", " "));
        summaryLore.add("§7Description: §f" + description);
        summaryLore.add("§7Max Players: §f" + maxParticipants);
        summaryLore.add("§7Duration: §f" + formatDuration(duration));
        summaryLore.add("§7Auto Start: " + (autoStart ? "§aYes" : "§cNo"));
        
        String locationWorld = (String) session.getCreationData("locationWorld");
        if (locationWorld != null) {
            summaryLore.add("§7Location: §aSet");
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
        ItemStack createButton = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta createMeta = createButton.getItemMeta();
        createMeta.setDisplayName("§a§lCreate Event");
        createMeta.setLore(Arrays.asList(
            "§7Create the event with these settings",
            "",
            "§aClick to create!"
        ));
        createButton.setItemMeta(createMeta);
        gui.setItem(29, createButton);
        
        // Cancel button
        ItemStack cancelButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName("§c§lCancel Creation");
        cancelMeta.setLore(Arrays.asList(
            "§7Cancel event creation and",
            "§7return to admin panel",
            "",
            "§cClick to cancel"
        ));
        cancelButton.setItemMeta(cancelMeta);
        gui.setItem(33, cancelButton);
        
        addWizardNavigation(gui, session, true, false);
        player.openInventory(gui);
    }
    
    // Helper methods
    private void addWizardNavigation(Inventory gui, GUISession session, boolean canPrevious, boolean canNext) {
        if (canPrevious) {
            ItemStack previous = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = previous.getItemMeta();
            prevMeta.setDisplayName("§7« Previous Step");
            previous.setItemMeta(prevMeta);
            gui.setItem(36, previous);
        }
        
        if (canNext) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§aNext Step »");
            next.setItemMeta(nextMeta);
            gui.setItem(44, next);
        }
        
        // Cancel button
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§cCancel Creation");
        cancel.setItemMeta(cancelMeta);
        gui.setItem(40, cancel);
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
    
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d minutes %02d seconds", minutes, remainingSeconds);
    }
} 