package com.swiftevents.listeners;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    
    private final SwiftEventsPlugin plugin;
    
    // Optimization: Cache GUI sessions to avoid repeated lookups and improve performance
    private final Map<UUID, GuiSession> activeGuiSessions = new HashMap<>();
    
    // Optimization: Debounce click events to prevent double-clicking issues
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private static final long CLICK_COOLDOWN = 250; // 250ms cooldown
    
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
                .filter(gameEvent -> gameEvent.getStartTime() != null)
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
        } else if (title.equals("§4Admin - Event Management")) {
            handleAdminGUI(player, itemName);
        }
    }
    
    private boolean isSwiftEventsGUI(String title) {
        return title.startsWith(plugin.getConfigManager().getGUITitle()) ||
               title.startsWith("§6Event: ") ||
               title.equals("§4Admin - Event Management");
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
                    plugin.getGUIManager().openAdminGUI(player);
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
            case "§aCreate New Event":
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§7Use §f/eventadmin create <name> <type> <description> §7to create an event");
                player.sendMessage("§7Available types: §fPVP, PVE, BUILDING, RACING, TREASURE_HUNT, MINI_GAME, CUSTOM");
                break;
                
            case "§6Active Events":
                player.closeInventory();
                player.performCommand("eventadmin list");
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
} 