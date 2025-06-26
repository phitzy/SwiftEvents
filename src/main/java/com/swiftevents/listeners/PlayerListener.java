package com.swiftevents.listeners;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    
    private final SwiftEventsPlugin plugin;
    
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
        
        // Clear any HUD elements for the player
        plugin.getHUDManager().clearPlayerHUD(player);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // Handle Events GUI
        if (title.equals(plugin.getConfigManager().getGUITitle()) || 
            title.startsWith("§6Event:")) {
            event.setCancelled(true);
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }
            
            String itemName = clickedItem.getItemMeta().getDisplayName();
            
            // Handle refresh button
            if (itemName.equals("§aRefresh")) {
                plugin.getGUIManager().openEventsGUI(player);
                return;
            }
            
            // Handle close button
            if (itemName.equals("§cClose")) {
                player.closeInventory();
                return;
            }
            
            // Handle event items
            if (itemName.startsWith("§") && !itemName.equals("§cClose") && !itemName.equals("§aRefresh")) {
                String eventName = itemName.substring(2); // Remove color code
                Event gameEvent = findEventByName(eventName);
                
                if (gameEvent != null) {
                    if (gameEvent.isParticipant(player.getUniqueId())) {
                        // Player is already in event, ask if they want to leave
                        boolean success = plugin.getEventManager().leaveEvent(gameEvent.getId(), player.getUniqueId());
                        if (success) {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                                    "§cYou left the event: " + gameEvent.getName());
                        }
                    } else if (gameEvent.canJoin()) {
                        // Player can join the event
                        boolean success = plugin.getEventManager().joinEvent(gameEvent.getId(), player.getUniqueId());
                        if (success) {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                                    "§aYou joined the event: " + gameEvent.getName());
                        }
                    }
                    
                    // Refresh the GUI
                    plugin.getGUIManager().openEventsGUI(player);
                }
            }
        }
        
        // Handle Admin GUI
        else if (title.equals("§4Admin - Event Management")) {
            event.setCancelled(true);
            
            if (!player.hasPermission("swiftevents.admin")) {
                player.closeInventory();
                return;
            }
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }
            
            String itemName = clickedItem.getItemMeta().getDisplayName();
            
            switch (itemName) {
                case "§aCreate New Event":
                    player.closeInventory();
                    player.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§7Use §f/eventadmin create <name> <type> <description> §7to create an event");
                    break;
                    
                case "§6Active Events":
                    player.closeInventory();
                    player.performCommand("eventadmin list");
                    break;
                    
                case "§cClose":
                    player.closeInventory();
                    break;
                    
                default:
                    break;
            }
        }
    }
    
    private Event findEventByName(String name) {
        return plugin.getEventManager().getAllEvents().stream()
                .filter(event -> event.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
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
} 