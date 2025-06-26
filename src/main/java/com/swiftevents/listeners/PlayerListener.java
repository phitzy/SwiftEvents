package com.swiftevents.listeners;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.gui.GUISession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerListener implements Listener {
    
    private final SwiftEventsPlugin plugin;
    
    // Optimization: Cache GUI sessions to avoid repeated lookups and improve performance
    private final Map<UUID, GuiSession> activeGuiSessions = new HashMap<>();
    
    // Optimization: Debounce click events to prevent double-clicking issues
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private static final long CLICK_COOLDOWN = 250; // 250ms cooldown
    
    // Input handling for chat-based GUI input
    private final Map<UUID, InputWaiting> awaitingInput = new HashMap<>();
    
    public PlayerListener(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has any active events
        if (plugin.getEventManager().isPlayerInEvent(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aYou have active events! Use /event list to see them.");
        }
        
        // Send notifications for scheduled events
        plugin.getEventManager().getEventsByStatus(Event.EventStatus.SCHEDULED)
                .stream()
                .filter(gameEvent -> gameEvent.getStartTime() > 0)
                .filter(gameEvent -> gameEvent.getStartTime() - System.currentTimeMillis() < 300000) // 5 minutes
                .forEach(gameEvent -> {
                    long timeUntilStart = (gameEvent.getStartTime() - System.currentTimeMillis()) / 1000;
                    if (timeUntilStart > 0) {
                        plugin.getHUDManager().sendEventNotification(player, 
                                gameEvent.getName(), "update", 
                                "Starting in " + formatTime(timeUntilStart));
                    }
                });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Clear any HUD elements for the player
        plugin.getHUDManager().clearPlayerHUD(player);
        
        // Clean up GUI session data
        activeGuiSessions.remove(playerId);
        lastClickTime.remove(playerId);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            UUID playerId = player.getUniqueId();
            // Clean up GUI session when inventory is closed
            activeGuiSessions.remove(playerId);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        String title = event.getView().getTitle();
        
        // Check if this is one of our GUI interfaces
        if (!isSwiftEventsGUI(title)) {
            return;
        }
        
        // Cancel the event to prevent item manipulation
        event.setCancelled(true);
        
        // Implement click cooldown to prevent double-clicking
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastClickTime.get(playerId);
        if (lastClick != null && (currentTime - lastClick) < CLICK_COOLDOWN) {
            return;
        }
        lastClickTime.put(playerId, currentTime);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = clickedItem.getItemMeta();
        String itemName = meta.getDisplayName();
        
        // Handle different GUI types
        if (title.startsWith(plugin.getConfigManager().getGUITitle())) {
            handleMainEventsGUI(player, itemName, clickedItem, event.getSlot());
        } else if (title.startsWith("§6Event: ")) {
            handleEventDetailsGUI(player, title, itemName, clickedItem);
        } else if (title.equals("§4Admin Dashboard - SwiftEvents")) {
            handleAdminGUI(player, itemName);
        } else if (title.startsWith("§6Event Creation")) {
            handleEventCreationGUI(player, title, itemName, clickedItem);
        } else if (title.equals("§b§lEvent Statistics Dashboard")) {
            handleStatisticsGUI(player, itemName);
        }
    }
    
    private boolean isSwiftEventsGUI(String title) {
        return title.startsWith(plugin.getConfigManager().getGUITitle()) ||
               title.startsWith("§6Event: ") ||
               title.equals("§4Admin - Event Management") ||
               title.equals("§4Admin Dashboard - SwiftEvents") ||
               title.startsWith("§6Event Creation") ||
               title.equals("§b§lEvent Statistics Dashboard");
    }
    
    private void handleMainEventsGUI(Player player, String itemName, ItemStack clickedItem, int slot) {
        // Extract current page from title if it exists
        String title = player.getOpenInventory().getTitle();
        int currentPage = extractPageFromTitle(title);
        
        switch (itemName) {
            case "§aRefresh":
                // Smooth refresh with visual feedback
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§7Refreshing events...");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getGUIManager().openEventsGUI(player, currentPage);
                    }
                }.runTaskLater(plugin, 1L);
                break;
                
            case "§cClose":
                player.closeInventory();
                break;
                
            case "§7← Previous Page":
                if (currentPage > 0) {
                    plugin.getGUIManager().openEventsGUI(player, currentPage - 1);
                }
                break;
                
            case "§7Next Page →":
                plugin.getGUIManager().openEventsGUI(player, currentPage + 1);
                break;
                
            case "§4Admin Panel":
                if (player.hasPermission("swiftevents.admin")) {
                    plugin.getAdminGUIManager().openAdminGUI(player);
                }
                break;
                
            default:
                // Handle page info clicks (do nothing)
                if (itemName.startsWith("§6Page ")) {
                    return;
                }
                
                // Handle event item clicks
                if (itemName.startsWith("§")) {
                    handleEventItemClick(player, itemName, clickedItem);
                }
                break;
        }
    }
    
    private void handleEventItemClick(Player player, String itemName, ItemStack clickedItem) {
        // Extract event name from colored display name
        String eventName = stripColorCodes(itemName);
        Event event = findEventByName(eventName);
        
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent not found!");
            return;
        }
        
        // Store GUI session for context
        activeGuiSessions.put(player.getUniqueId(), new GuiSession(GuiType.EVENTS_LIST, event.getId()));
        
        // Check if player is holding shift for quick actions
        if (player.isSneaking()) {
            handleQuickEventAction(player, event);
        } else {
            // Open detailed event GUI
            plugin.getGUIManager().openEventDetailsGUI(player, event);
        }
    }
    
    private void handleQuickEventAction(Player player, Event event) {
        // Check if player has permission for this event type
        if (!plugin.getGUIManager().canPlayerAccessEventGUI(player, event)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cYou don't have permission to interact with this event type.");
            return;
        }
        
        if (event.isParticipant(player.getUniqueId())) {
            // Quick leave
            boolean success = plugin.getEventManager().leaveEvent(event.getId(), player.getUniqueId());
            if (success) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cYou left the event: " + event.getName());
                // Play sound effect if available
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                } catch (Exception ignored) {}
                
                // Refresh GUI after a short delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getGUIManager().refreshCurrentGUI(player);
                    }
                }.runTaskLater(plugin, 2L);
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cFailed to leave event. Please try again.");
            }
        } else if (event.canJoin()) {
            // Quick join
            boolean success = plugin.getEventManager().joinEvent(event.getId(), player.getUniqueId());
            if (success) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§aYou joined the event: " + event.getName());
                // Play sound effect if available
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                } catch (Exception ignored) {}
                
                // Refresh GUI after a short delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getGUIManager().refreshCurrentGUI(player);
                    }
                }.runTaskLater(plugin, 2L);
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cFailed to join event. Please try again.");
            }
        } else {
            String reason = getJoinBlockReason(event);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + reason);
        }
    }
    
    private void handleEventDetailsGUI(Player player, String title, String itemName, ItemStack clickedItem) {
        // Extract event name from title
        String eventName = title.substring("§6Event: ".length());
        Event event = findEventByName(eventName);
        
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent not found!");
            player.closeInventory();
            return;
        }
        
        switch (itemName) {
            case "§aJoin Event":
                handleJoinEvent(player, event);
                break;
                
            case "§cLeave Event":
                handleLeaveEvent(player, event);
                break;
                
            case "§cCannot Join":
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cYou cannot join this event at this time.");
                break;
                
            case "§7← Back":
                plugin.getGUIManager().openEventsGUI(player);
                break;
                
            // Admin controls
            case "§aStart Event":
                handleStartEvent(player, event);
                break;
                
            case "§cStop Event":
                handleStopEvent(player, event);
                break;
                
            case "§cDelete Event":
                handleDeleteEvent(player, event);
                break;
                
            case "§9Participants":
                handleViewParticipants(player, event);
                break;
                
            default:
                // Handle teleport to event if clicking on event info
                if (clickedItem.getType() != Material.BARRIER && 
                    clickedItem.getType() != Material.ARROW &&
                    event.getWorld() != null) {
                    handleTeleportToEvent(player, event);
                }
                break;
        }
    }
    
    private void handleJoinEvent(Player player, Event event) {
        if (event.isParticipant(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cYou are already participating in this event!");
            return;
        }
        
        if (!event.canJoin()) {
            String reason = getJoinBlockReason(event);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + reason);
            return;
        }
        
        boolean success = plugin.getEventManager().joinEvent(event.getId(), player.getUniqueId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully joined event: " + event.getName());
            
            // Refresh the GUI to show updated state
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getGUIManager().openEventDetailsGUI(player, event);
                }
            }.runTaskLater(plugin, 2L);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to join event. Please try again.");
        }
    }
    
    private void handleLeaveEvent(Player player, Event event) {
        if (!event.isParticipant(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cYou are not participating in this event!");
            return;
        }
        
        boolean success = plugin.getEventManager().leaveEvent(event.getId(), player.getUniqueId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cYou left the event: " + event.getName());
            
            // Refresh the GUI to show updated state
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getGUIManager().openEventDetailsGUI(player, event);
                }
            }.runTaskLater(plugin, 2L);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to leave event. Please try again.");
        }
    }
    
    private void handleStartEvent(Player player, Event event) {
        if (!player.hasPermission("swiftevents.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        if (!event.canStart()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent cannot be started in its current state.");
            return;
        }
        
        boolean success = plugin.getEventManager().startEvent(event.getId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully started event: " + event.getName());
            
            // Refresh the GUI after starting
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getGUIManager().openEventDetailsGUI(player, event);
                }
            }.runTaskLater(plugin, 2L);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to start event. Please try again.");
        }
    }
    
    private void handleStopEvent(Player player, Event event) {
        if (!player.hasPermission("swiftevents.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        if (!event.isActive()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent is not currently active.");
            return;
        }
        
        boolean success = plugin.getEventManager().endEvent(event.getId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully stopped event: " + event.getName());
            
            // Refresh the GUI after stopping
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getGUIManager().openEventDetailsGUI(player, event);
                }
            }.runTaskLater(plugin, 2L);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to stop event. Please try again.");
        }
    }
    
    private void handleDeleteEvent(Player player, Event event) {
        if (!player.hasPermission("swiftevents.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        // Require confirmation for deletion - implement a confirmation system
        UUID playerId = player.getUniqueId();
        GuiSession session = activeGuiSessions.get(playerId);
        
        if (session != null && session.awaitingConfirmation && 
            session.confirmationAction == ConfirmationAction.DELETE_EVENT) {
            // User clicked delete again - confirm deletion
            boolean success = plugin.getEventManager().deleteEvent(event.getId());
            if (success) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§aSuccessfully deleted event: " + event.getName());
                player.closeInventory();
                
                // Open events list after short delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getGUIManager().openEventsGUI(player);
                    }
                }.runTaskLater(plugin, 10L);
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cFailed to delete event. Please try again.");
            }
            
            // Clear confirmation state
            session.awaitingConfirmation = false;
            session.confirmationAction = null;
        } else {
            // First click - ask for confirmation
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§c§lWARNING: §cClick delete again to permanently delete this event!");
            
            // Set confirmation state
            if (session == null) {
                session = new GuiSession(GuiType.EVENT_DETAILS, event.getId());
                activeGuiSessions.put(playerId, session);
            }
            session.awaitingConfirmation = true;
            session.confirmationAction = ConfirmationAction.DELETE_EVENT;
            
            // Clear confirmation after 10 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    GuiSession currentSession = activeGuiSessions.get(playerId);
                    if (currentSession != null && currentSession.awaitingConfirmation) {
                        currentSession.awaitingConfirmation = false;
                        currentSession.confirmationAction = null;
                        player.sendMessage(plugin.getConfigManager().getPrefix() + 
                                "§7Deletion confirmation expired.");
                    }
                }
            }.runTaskLater(plugin, 200L); // 10 seconds
        }
    }
    
    private void handleViewParticipants(Player player, Event event) {
        if (event.getParticipants().isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§7No participants in this event yet.");
            return;
        }
        
        player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§6Participants in " + event.getName() + ":");
        
        int count = 0;
        for (UUID participantId : event.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            String name = participant != null ? participant.getName() : "Unknown";
            String status = participant != null && participant.isOnline() ? "§a●" : "§7●";
            player.sendMessage("  " + status + " §f" + name);
            
            count++;
            if (count >= 20) { // Limit to prevent spam
                int remaining = event.getParticipants().size() - count;
                if (remaining > 0) {
                    player.sendMessage("  §7... and " + remaining + " more");
                }
                break;
            }
        }
    }
    
    private void handleTeleportToEvent(Player player, Event event) {
        if (!player.hasPermission("swiftevents.teleport")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        if (event.getWorld() == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent location is not set.");
            return;
        }
        
        // Use the existing teleport logic from EventCommand
        player.performCommand("event teleport " + event.getName());
        player.closeInventory();
    }
    
    private void handleAdminGUI(Player player, String itemName) {
        if (!player.hasPermission("swiftevents.admin")) {
            player.closeInventory();
            return;
        }
        
        switch (itemName) {
            // Legacy admin GUI items (for backward compatibility)
            case "§aCreate New Event":
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§7Use §f/eventadmin create <n> <type> <description> §7to create an event");
                player.sendMessage("§7Available types: §fPVP, PVE, BUILDING, RACING, TREASURE_HUNT, MINI_GAME, CUSTOM");
                break;
                
            case "§6Active Events":
                player.closeInventory();
                player.performCommand("eventadmin list");
                break;
                
            // Enhanced admin GUI items
            case "§a§lCreate New Event":
                // Open event creation wizard
                plugin.getAdminGUIManager().openEventCreationWizard(player);
                break;
                
            case "§6§lManage Events":
                // Open event management GUI (show all events with admin controls)
                plugin.getGUIManager().openEventsGUI(player);
                break;
                
            case "§c§lActive Events":
                // Show detailed active events list
                player.closeInventory();
                player.performCommand("eventadmin list");
                break;
                
            case "§b§lEvent Statistics":
                // Open statistics dashboard
                plugin.getStatisticsGUIManager().openEventStatisticsGUI(player);
                break;
                
            case "§e§lServer Performance":
                // Show server performance metrics
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§eServer Performance Metrics:");
                player.sendMessage("§7Online Players: §f" + org.bukkit.Bukkit.getOnlinePlayers().size());
                player.sendMessage("§7Active Events: §f" + plugin.getEventManager().getActiveEvents().size());
                player.sendMessage("§7Total Events: §f" + plugin.getEventManager().getAllEvents().size());
                break;
                
            case "§d§lPlayer Statistics":
                // Show player engagement data
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§dPlayer Statistics:");
                player.sendMessage("§7Online players: §f" + org.bukkit.Bukkit.getOnlinePlayers().size());
                // Count players in events
                int playersInEvents = plugin.getEventManager().getActiveEvents().stream()
                        .mapToInt(event -> event.getCurrentParticipants())
                        .sum();
                player.sendMessage("§7Players in events: §f" + playersInEvents);
                break;
                
            case "§9§lPlugin Settings":
                // Open settings management
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§9Use §f/eventadmin config §9to manage plugin settings");
                break;
                
            case "§5§lPermission Manager":
                // Permission management
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§5Permission management via in-game GUI not yet implemented.");
                player.sendMessage("§7Use your permission plugin's commands to manage SwiftEvents permissions.");
                break;
                
            case "§3§lBackup & Restore":
                // Backup management
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§3Use §f/eventadmin backup §3to manage backups");
                break;
                
            case "§2§lEvent Tasker §a[ON]":
            case "§2§lEvent Tasker §c[OFF]":
                // Tasker management
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§2Use §f/eventadmin tasker §2to manage the event tasker");
                break;
                
            case "§a§lEvent Presets":
                // Preset management
                player.closeInventory();
                player.performCommand("eventadmin tasker presets");
                break;
                
            case "§f§lOnline Players":
                // Show online player details
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§fOnline Players Details:");
                player.sendMessage("§7Total online: §f" + org.bukkit.Bukkit.getOnlinePlayers().size());
                break;
                
            case "§c§lUser Restrictions":
                // User restriction management
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cUser restriction management not yet implemented.");
                break;
                
            case "§a§lRefresh":
                // Refresh the admin GUI
                plugin.getAdminGUIManager().openAdminGUI(player);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§aAdmin panel refreshed!");
                break;
                
            case "§7« Back to Events":
                // Return to main events GUI
                plugin.getGUIManager().openEventsGUI(player);
                break;
                
            case "§e§lHelp & Documentation":
                // Show help information
                player.closeInventory();
                player.performCommand("eventadmin help");
                break;
                
            case "§cClose":
                player.closeInventory();
                break;
        }
    }
    
    // Utility methods
    private Event findEventByName(String name) {
        return plugin.getEventManager().getAllEvents().stream()
                .filter(event -> event.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    private String stripColorCodes(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
    
    private int extractPageFromTitle(String title) {
        // Extract page number from titles like "Events (Page 2/5)"
        if (title.contains("(Page ")) {
            try {
                int start = title.indexOf("(Page ") + 6;
                int end = title.indexOf("/", start);
                if (start > 5 && end > start) {
                    return Integer.parseInt(title.substring(start, end)) - 1; // Convert to 0-based
                }
            } catch (NumberFormatException e) {
                // Ignore and return 0
            }
        }
        return 0; // Default to first page
    }
    
    private String getJoinBlockReason(Event event) {
        if (event.isFull()) {
            return "Event is full";
        }
        if (event.isCompleted()) {
            return "Event has ended";
        }
        if (event.isCancelled()) {
            return "Event was cancelled";
        }
        return "Event not accepting participants";
    }
    
    private String formatTime(long seconds) {
        if (seconds <= 0) {
            return "now";
        }
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return secs + "s";
        }
    }
    
    // Inner classes for session management
    private static class GuiSession {
        final GuiType type;
        final String eventId;
        boolean awaitingConfirmation = false;
        ConfirmationAction confirmationAction = null;
        
        GuiSession(GuiType type, String eventId) {
            this.type = type;
            this.eventId = eventId;
        }
    }
    
    private enum GuiType {
        EVENTS_LIST, EVENT_DETAILS, ADMIN_PANEL
    }
    
    private enum ConfirmationAction {
        DELETE_EVENT
    }
    
    // Input handling class for chat-based GUI input
    private static class InputWaiting {
        final String inputType;
        final GUISession session;
        
        InputWaiting(String inputType, GUISession session) {
            this.inputType = inputType;
            this.session = session;
        }
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        InputWaiting waiting = awaitingInput.get(playerId);
        if (waiting == null) {
            return;
        }
        
        event.setCancelled(true);
        awaitingInput.remove(playerId);
        
        String message = event.getMessage();
        if ("cancel".equalsIgnoreCase(message)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cInput cancelled.");
            // Reopen the appropriate wizard step
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getAdminGUIManager().openEventCreationStep(player, waiting.session.getCreationStep());
                }
            }.runTask(plugin);
            return;
        }
        
        // Handle different input types
        switch (waiting.inputType) {
            case "event_name":
                if (message.length() > 32) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent name too long! Maximum 32 characters.");
                    return;
                }
                waiting.session.setCreationData("name", message);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§aEvent name set to: " + message);
                break;
                
            case "event_description":
                if (message.length() > 100) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§cDescription too long! Maximum 100 characters.");
                    return;
                }
                waiting.session.setCreationData("description", message);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§aEvent description set to: " + message);
                break;
                
            case "reward_command":
                @SuppressWarnings("unchecked")
                List<String> rewardCommands = (List<String>) waiting.session.getCreationData("rewardCommands");
                if (rewardCommands == null) {
                    rewardCommands = new ArrayList<>();
                    waiting.session.setCreationData("rewardCommands", rewardCommands);
                }
                rewardCommands.add(message);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§aReward command added: " + message);
                break;
        }
        
        // Reopen the GUI after input
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getAdminGUIManager().openEventCreationStep(player, waiting.session.getCreationStep());
            }
        }.runTask(plugin);
    }
    
    private void handleEventCreationGUI(Player player, String title, String itemName, ItemStack clickedItem) {
        GUISession session = plugin.getGUIManager().getOrCreateSession(player.getUniqueId());
        
        // Handle common navigation buttons first
        switch (itemName) {
            case "§cCancel Creation":
                session.clearCreationData();
                plugin.getAdminGUIManager().openAdminGUI(player);
                return;
                
            case "§7« Previous Step":
                navigatePrevious(player, session);
                return;
                
            case "§aNext Step »":
                navigateNext(player, session);
                return;
                
            case "§c§lCancel Creation":
                session.clearCreationData();
                plugin.getAdminGUIManager().openAdminGUI(player);
                return;
                
            case "§a§lCreate Event":
                createEventFromSession(player, session);
                return;
        }
        
        // Handle step-specific items based on current title
        if (title.equals("§6Event Creation - Select Type")) {
            handleTypeSelection(player, session, itemName);
        } else if (title.equals("§6Event Creation - Basic Info")) {
            handleBasicInfoInput(player, session, itemName);
        } else if (title.equals("§6Event Creation - Settings")) {
            handleSettingsInput(player, session, itemName, clickedItem);
        } else if (title.equals("§6Event Creation - Location")) {
            handleLocationInput(player, session, itemName);
        } else if (title.equals("§6Event Creation - Rewards")) {
            handleRewardsInput(player, session, itemName);
        }
    }
    
    private void handleTypeSelection(Player player, GUISession session, String itemName) {
        // Handle event type selection
        for (Event.EventType type : Event.EventType.values()) {
            String typeName = "§e" + type.name().replace("_", " ");
            if (itemName.equals(typeName)) {
                session.setCreationData("type", type);
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSelected event type: " + type.name().replace("_", " "));
                navigateNext(player, session);
                return;
            }
        }
    }
    
    private void handleBasicInfoInput(Player player, GUISession session, String itemName) {
        switch (itemName) {
            case "§eEvent Name":
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§eType the event name in chat (or 'cancel' to abort):");
                
                // Set up chat listener for name input
                awaitingInput.put(player.getUniqueId(), new InputWaiting("event_name", session));
                break;
                
            case "§eEvent Description":
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§eType the event description in chat (or 'cancel' to abort):");
                
                // Set up chat listener for description input
                awaitingInput.put(player.getUniqueId(), new InputWaiting("event_description", session));
                break;
        }
    }
    
    private void handleSettingsInput(Player player, GUISession session, String itemName, ItemStack clickedItem) {
        boolean isShiftClick = player.isSneaking();
        boolean isRightClick = false; // Would need to track this from InventoryClickEvent
        
        switch (itemName) {
            case "§eMax Participants":
                Integer maxParticipants = (Integer) session.getCreationData("maxParticipants");
                if (maxParticipants == null) maxParticipants = 20;
                
                if (isShiftClick) {
                    maxParticipants += isRightClick ? -1 : 1;
                } else {
                    maxParticipants += isRightClick ? -5 : 5;
                }
                
                maxParticipants = Math.max(1, Math.min(100, maxParticipants));
                session.setCreationData("maxParticipants", maxParticipants);
                plugin.getAdminGUIManager().openEventCreationStep(player, GUISession.EventCreationStep.SETTINGS);
                break;
                
            case "§eEvent Duration":
                Integer duration = (Integer) session.getCreationData("duration");
                if (duration == null) duration = 1800;
                
                if (isShiftClick) {
                    duration += isRightClick ? -300 : 300; // 5 minutes
                } else {
                    duration += isRightClick ? -900 : 900; // 15 minutes
                }
                
                duration = Math.max(300, Math.min(7200, duration)); // 5 minutes to 2 hours
                session.setCreationData("duration", duration);
                plugin.getAdminGUIManager().openEventCreationStep(player, GUISession.EventCreationStep.SETTINGS);
                break;
                
            case "§eAuto Start":
                Boolean autoStart = (Boolean) session.getCreationData("autoStart");
                session.setCreationData("autoStart", !Boolean.TRUE.equals(autoStart));
                plugin.getAdminGUIManager().openEventCreationStep(player, GUISession.EventCreationStep.SETTINGS);
                break;
                
            case "§eMin Participants":
                Integer minParticipants = (Integer) session.getCreationData("minParticipants");
                if (minParticipants == null) minParticipants = 2;
                
                minParticipants += isRightClick ? -1 : 1;
                minParticipants = Math.max(1, Math.min(50, minParticipants));
                session.setCreationData("minParticipants", minParticipants);
                plugin.getAdminGUIManager().openEventCreationStep(player, GUISession.EventCreationStep.SETTINGS);
                break;
        }
    }
    
    private void handleLocationInput(Player player, GUISession session, String itemName) {
        switch (itemName) {
            case "§aSet to My Location":
                Location loc = player.getLocation();
                session.setCreationData("locationWorld", loc.getWorld().getName());
                session.setCreationData("locationX", loc.getX());
                session.setCreationData("locationY", loc.getY());
                session.setCreationData("locationZ", loc.getZ());
                
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aEvent location set to your current position!");
                plugin.getAdminGUIManager().openEventCreationStep(player, GUISession.EventCreationStep.LOCATION);
                break;
                
            case "§cClear Location":
                session.setCreationData("locationWorld", null);
                session.setCreationData("locationX", null);
                session.setCreationData("locationY", null);
                session.setCreationData("locationZ", null);
                
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent location cleared!");
                plugin.getAdminGUIManager().openEventCreationStep(player, GUISession.EventCreationStep.LOCATION);
                break;
        }
    }
    
    private void handleRewardsInput(Player player, GUISession session, String itemName) {
        switch (itemName) {
            case "§aAdd Reward Command":
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§eType a reward command in chat (use {winner} for player name, or 'cancel' to abort):");
                
                awaitingInput.put(player.getUniqueId(), new InputWaiting("reward_command", session));
                break;
                
            case "§cClear All Rewards":
                session.setCreationData("rewardCommands", new ArrayList<String>());
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cAll reward commands cleared!");
                plugin.getAdminGUIManager().openEventCreationStep(player, GUISession.EventCreationStep.REWARDS);
                break;
        }
    }
    
    private void navigatePrevious(Player player, GUISession session) {
        GUISession.EventCreationStep currentStep = session.getCreationStep();
        GUISession.EventCreationStep previousStep;
        
        switch (currentStep) {
            case BASIC_INFO -> previousStep = GUISession.EventCreationStep.TYPE_SELECTION;
            case SETTINGS -> previousStep = GUISession.EventCreationStep.BASIC_INFO;
            case LOCATION -> previousStep = GUISession.EventCreationStep.SETTINGS;
            case REWARDS -> previousStep = GUISession.EventCreationStep.LOCATION;
            case CONFIRMATION -> previousStep = GUISession.EventCreationStep.REWARDS;
            default -> {
                return; // Can't go back from TYPE_SELECTION
            }
        }
        
        plugin.getAdminGUIManager().openEventCreationStep(player, previousStep);
    }
    
    private void navigateNext(Player player, GUISession session) {
        GUISession.EventCreationStep currentStep = session.getCreationStep();
        GUISession.EventCreationStep nextStep;
        
        switch (currentStep) {
            case TYPE_SELECTION -> nextStep = GUISession.EventCreationStep.BASIC_INFO;
            case BASIC_INFO -> {
                // Check if name and description are set
                String name = (String) session.getCreationData("name");
                String description = (String) session.getCreationData("description");
                if (name == null || name.equals("Click to set name") || 
                    description == null || description.equals("Click to set description")) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cPlease set both event name and description before proceeding!");
                    return;
                }
                nextStep = GUISession.EventCreationStep.SETTINGS;
            }
            case SETTINGS -> nextStep = GUISession.EventCreationStep.LOCATION;
            case LOCATION -> nextStep = GUISession.EventCreationStep.REWARDS;
            case REWARDS -> nextStep = GUISession.EventCreationStep.CONFIRMATION;
            default -> {
                return; // Can't go forward from CONFIRMATION
            }
        }
        
        plugin.getAdminGUIManager().openEventCreationStep(player, nextStep);
    }
    
    private void createEventFromSession(Player player, GUISession session) {
        // Extract data from session
        Event.EventType type = (Event.EventType) session.getCreationData("type");
        String name = (String) session.getCreationData("name");
        String description = (String) session.getCreationData("description");
        Integer maxParticipants = (Integer) session.getCreationData("maxParticipants");
        Integer duration = (Integer) session.getCreationData("duration");
        Boolean autoStart = (Boolean) session.getCreationData("autoStart");
        String locationWorld = (String) session.getCreationData("locationWorld");
        
        // Create the event
        Event event = plugin.getEventManager().createEvent(name, description, type, player.getUniqueId());
        
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cFailed to create event! Maximum concurrent events reached?");
            return;
        }
        
        // Apply settings
        if (maxParticipants != null) {
            event.setMaxParticipants(maxParticipants);
        }
        // Note: Duration setting may need to be handled differently based on Event class implementation
        
        // Set location if provided
        if (locationWorld != null) {
            Double x = (Double) session.getCreationData("locationX");
            Double y = (Double) session.getCreationData("locationY");
            Double z = (Double) session.getCreationData("locationZ");
            event.setLocation(locationWorld, x, y, z);
        }
        
        // Add reward commands
        @SuppressWarnings("unchecked")
        List<String> rewardCommands = (List<String>) session.getCreationData("rewardCommands");
        if (rewardCommands != null && !rewardCommands.isEmpty()) {
            event.setRewards(rewardCommands);
        }
        
        // Save the event
        plugin.getEventManager().saveEvent(event);
        
        // Auto-start if requested
        if (Boolean.TRUE.equals(autoStart)) {
            plugin.getEventManager().startEvent(event.getId());
        }
        
        // Clear session and return to admin panel
        session.clearCreationData();
        player.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aSuccessfully created event: " + event.getName());
        plugin.getAdminGUIManager().openAdminGUI(player);
    }
    
    private void handleStatisticsGUI(Player player, String itemName) {
        switch (itemName) {
            case "§7« Back to Admin Panel":
                plugin.getAdminGUIManager().openAdminGUI(player);
                break;
                
            case "§2§lRefresh Statistics":
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§2Refreshing statistics...");
                // Would refresh the statistics GUI
                break;
                
            default:
                // Handle clicks on statistics items
                if (itemName.contains("§l")) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§bViewing: " + itemName);
                }
                break;
        }
    }
} 