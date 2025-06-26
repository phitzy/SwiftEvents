package com.swiftevents.gui;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager {
    
    private final SwiftEventsPlugin plugin;
    
    public GUIManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void openEventsGUI(Player player) {
        if (!plugin.getConfigManager().isGUIEnabled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cGUI is currently disabled.");
            return;
        }
        
        List<Event> events = plugin.getEventManager().getAllEvents();
        int size = Math.min(54, ((events.size() + 8) / 9) * 9);
        
        Inventory gui = Bukkit.createInventory(null, size, plugin.getConfigManager().getGUITitle());
        
        int slot = 0;
        for (Event event : events) {
            if (slot >= size - 9) break;
            
            ItemStack item = createEventItem(event, player);
            gui.setItem(slot, item);
            slot++;
        }
        
        addNavigationItems(gui, player);
        player.openInventory(gui);
    }
    
    public void openAdminGUI(Player player) {
        if (!player.hasPermission("swiftevents.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 45, "§4Admin - Event Management");
        
        // Create basic admin items
        ItemStack createEvent = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = createEvent.getItemMeta();
        createMeta.setDisplayName("§aCreate New Event");
        createMeta.setLore(Arrays.asList("§7Click to create a new event"));
        createEvent.setItemMeta(createMeta);
        gui.setItem(10, createEvent);
        
        ItemStack activeEvents = new ItemStack(Material.CLOCK);
        ItemMeta activeMeta = activeEvents.getItemMeta();
        activeMeta.setDisplayName("§6Active Events");
        activeMeta.setLore(Arrays.asList("§7View active events"));
        activeEvents.setItemMeta(activeMeta);
        gui.setItem(12, activeEvents);
        
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§cClose");
        close.setItemMeta(closeMeta);
        gui.setItem(40, close);
        
        player.openInventory(gui);
    }
    
    public void openEventDetailsGUI(Player player, Event event) {
        Inventory gui = Bukkit.createInventory(null, 45, "§6Event: " + event.getName());
        
        // Event info item
        ItemStack info = createEventItem(event, player);
        gui.setItem(13, info);
        
        // Join/Leave button
        ItemStack joinLeave;
        if (event.isParticipant(player.getUniqueId())) {
            joinLeave = new ItemStack(Material.RED_CONCRETE);
            ItemMeta leaveMeta = joinLeave.getItemMeta();
            leaveMeta.setDisplayName("§cLeave Event");
            leaveMeta.setLore(Arrays.asList(
                    "§7You are currently participating",
                    "",
                    "§eClick to leave this event"
            ));
            joinLeave.setItemMeta(leaveMeta);
        } else {
            joinLeave = new ItemStack(Material.GREEN_CONCRETE);
            ItemMeta joinMeta = joinLeave.getItemMeta();
            joinMeta.setDisplayName("§aJoin Event");
            if (event.canJoin()) {
                joinMeta.setLore(Arrays.asList(
                        "§7Join this event",
                        "",
                        "§eClick to participate"
                ));
            } else {
                joinMeta.setDisplayName("§cCannot Join");
                joinMeta.setLore(Arrays.asList(
                        "§7This event is not joinable",
                        "",
                        "§cReason: " + getJoinBlockReason(event)
                ));
            }
            joinLeave.setItemMeta(joinMeta);
        }
        gui.setItem(20, joinLeave);
        
        // Participants list
        ItemStack participants = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta partMeta = participants.getItemMeta();
        partMeta.setDisplayName("§9Participants");
        List<String> partLore = new ArrayList<>();
        partLore.add("§7Current participants:");
        partLore.add("");
        
        Set<UUID> eventParticipants = event.getParticipants();
        if (eventParticipants.isEmpty()) {
            partLore.add("§7No participants yet");
        } else {
            int count = 0;
            for (UUID participantId : eventParticipants) {
                if (count >= 10) {
                    partLore.add("§7... and " + (eventParticipants.size() - count) + " more");
                    break;
                }
                Player participant = Bukkit.getPlayer(participantId);
                String name = participant != null ? participant.getName() : "Unknown";
                partLore.add("§f- " + name);
                count++;
            }
        }
        
        partMeta.setLore(partLore);
        participants.setItemMeta(partMeta);
        gui.setItem(24, participants);
        
        // Admin controls (if player has permission)
        if (player.hasPermission("swiftevents.admin")) {
            addAdminControls(gui, event);
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§7← Back");
        backMeta.setLore(Arrays.asList("§7Return to events list"));
        back.setItemMeta(backMeta);
        gui.setItem(36, back);
        
        player.openInventory(gui);
    }
    
    private ItemStack createEventItem(Event event, Player player) {
        Material material = getEventMaterial(event);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String statusColor = getStatusColor(event.getStatus());
        meta.setDisplayName(statusColor + event.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Type: §f" + event.getType().name());
        lore.add("§7Status: " + statusColor + event.getStatus().name());
        lore.add("§7Participants: §f" + event.getCurrentParticipants() + "/" + 
                (event.getMaxParticipants() > 0 ? event.getMaxParticipants() : "∞"));
        
        if (event.isParticipant(player.getUniqueId())) {
            lore.add("§a✓ You are participating");
        } else if (event.canJoin()) {
            lore.add("§eClick to join!");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void addNavigationItems(Inventory gui, Player player) {
        int size = gui.getSize();
        
        ItemStack refresh = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.setDisplayName("§aRefresh");
        refresh.setItemMeta(refreshMeta);
        gui.setItem(size - 9, refresh);
        
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§cClose");
        close.setItemMeta(closeMeta);
        gui.setItem(size - 1, close);
    }
    
    private void addAdminControls(Inventory gui, Event event) {
        // Start/Stop button
        ItemStack control;
        if (event.isActive()) {
            control = new ItemStack(Material.RED_CONCRETE);
            ItemMeta stopMeta = control.getItemMeta();
            stopMeta.setDisplayName("§cStop Event");
            stopMeta.setLore(Arrays.asList("§7End this event", "", "§eClick to stop"));
            control.setItemMeta(stopMeta);
        } else if (event.canStart()) {
            control = new ItemStack(Material.GREEN_CONCRETE);
            ItemMeta startMeta = control.getItemMeta();
            startMeta.setDisplayName("§aStart Event");
            startMeta.setLore(Arrays.asList("§7Start this event", "", "§eClick to start"));
            control.setItemMeta(startMeta);
        } else {
            control = new ItemStack(Material.GRAY_CONCRETE);
            ItemMeta grayMeta = control.getItemMeta();
            grayMeta.setDisplayName("§7Cannot Control");
            grayMeta.setLore(Arrays.asList("§7Event cannot be controlled", "§7in its current state"));
            control.setItemMeta(grayMeta);
        }
        gui.setItem(29, control);
        
        // Delete button
        ItemStack delete = new ItemStack(Material.TNT);
        ItemMeta deleteMeta = delete.getItemMeta();
        deleteMeta.setDisplayName("§cDelete Event");
        deleteMeta.setLore(Arrays.asList(
                "§7Permanently delete this event",
                "",
                "§c§lWarning: §cThis cannot be undone!",
                "§eClick to delete"
        ));
        delete.setItemMeta(deleteMeta);
        gui.setItem(33, delete);
    }
    
    // Utility methods
    private Material getEventMaterial(Event event) {
        return switch (event.getType()) {
            case PVP -> Material.DIAMOND_SWORD;
            case PVE -> Material.IRON_SWORD;
            case BUILDING -> Material.BRICKS;
            case RACING -> Material.GOLDEN_BOOTS;
            case TREASURE_HUNT -> Material.CHEST;
            case MINI_GAME -> Material.JUKEBOX;
            case CUSTOM -> Material.COMMAND_BLOCK;
        };
    }
    
    private String getStatusColor(Event.EventStatus status) {
        return switch (status) {
            case CREATED -> "§7";
            case SCHEDULED -> "§e";
            case ACTIVE -> "§a";
            case PAUSED -> "§6";
            case COMPLETED -> "§2";
            case CANCELLED -> "§c";
        };
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
} 