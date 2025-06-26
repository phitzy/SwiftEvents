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
        openEventsGUI(player, 0); // Open first page by default
    }
    
    public void openEventsGUI(Player player, int page) {
        if (!plugin.getConfigManager().isGUIEnabled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cGUI is currently disabled.");
            return;
        }
        
        List<Event> events = plugin.getEventManager().getAllEvents();
        
        // Handle empty events list
        if (events.isEmpty()) {
            Inventory gui = Bukkit.createInventory(null, 9, plugin.getConfigManager().getGUITitle());
            
            ItemStack noEvents = new ItemStack(Material.BARRIER);
            ItemMeta noEventsMeta = noEvents.getItemMeta();
            noEventsMeta.setDisplayName("§cNo Events Available");
            noEventsMeta.setLore(Arrays.asList("§7There are currently no events.", "§7Check back later!"));
            noEvents.setItemMeta(noEventsMeta);
            gui.setItem(4, noEvents);
            
            addNavigationItems(gui, player, page, 0);
            player.openInventory(gui);
            return;
        }
        
        // Pagination calculation
        int eventsPerPage = 36; // 4 rows for events, leaving room for navigation
        int totalPages = (int) Math.ceil((double) events.size() / eventsPerPage);
        page = Math.max(0, Math.min(page, totalPages - 1)); // Clamp page to valid range
        
        int size = 54; // Fixed size for consistency
        Inventory gui = Bukkit.createInventory(null, size, plugin.getConfigManager().getGUITitle() + " (Page " + (page + 1) + "/" + totalPages + ")");
        
        // Add events for current page
        int startIndex = page * eventsPerPage;
        int endIndex = Math.min(startIndex + eventsPerPage, events.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Event event = events.get(i);
            ItemStack item = createEventItem(event, player);
            gui.setItem(slot, item);
            slot++;
        }
        
        addNavigationItems(gui, player, page, totalPages);
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
        
        // Add creator information if available
        if (event.getCreatedBy() != null) {
            Player creator = Bukkit.getPlayer(event.getCreatedBy());
            String creatorName = creator != null ? creator.getName() : "Unknown";
            lore.add("§7Creator: §f" + creatorName);
        }
        
        // Add location information if available
        if (event.getWorld() != null) {
            lore.add("§7Location: §f" + event.getWorld() + " (" + 
                    (int)event.getX() + ", " + (int)event.getY() + ", " + (int)event.getZ() + ")");
        }
        
        lore.add(""); // Empty line for separation
        
        // Player-specific information
        if (event.isParticipant(player.getUniqueId())) {
            lore.add("§a✓ You are participating");
            lore.add("§7Shift+Click to leave quickly");
        } else if (event.canJoin()) {
            // Check if player has permission for this event type
            if (canPlayerAccessEventGUI(player, event)) {
                lore.add("§eClick to view details");
                lore.add("§7Shift+Click to join quickly");
            } else {
                lore.add("§c✗ No permission for this event type");
            }
        } else {
            lore.add("§c✗ Cannot join: " + getJoinBlockReason(event));
        }
        
        lore.add("");
        lore.add("§7Click for more details");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void addNavigationItems(Inventory gui, Player player) {
        addNavigationItems(gui, player, 0, 1);
    }
    
    private void addNavigationItems(Inventory gui, Player player, int currentPage, int totalPages) {
        int size = gui.getSize();
        
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName("§7← Previous Page");
            prevMeta.setLore(Arrays.asList("§7Go to page " + currentPage));
            prevPage.setItemMeta(prevMeta);
            gui.setItem(size - 9, prevPage);
        }
        
        // Page info
        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.setDisplayName("§6Page " + (currentPage + 1) + "/" + totalPages);
        pageMeta.setLore(Arrays.asList("§7You are viewing page " + (currentPage + 1)));
        pageInfo.setItemMeta(pageMeta);
        gui.setItem(size - 5, pageInfo);
        
        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName("§7Next Page →");
            nextMeta.setLore(Arrays.asList("§7Go to page " + (currentPage + 2)));
            nextPage.setItemMeta(nextMeta);
            gui.setItem(size - 1, nextPage);
        }
        
        // Refresh button
        ItemStack refresh = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.setDisplayName("§aRefresh");
        refreshMeta.setLore(Arrays.asList("§7Update the events list"));
        refresh.setItemMeta(refreshMeta);
        gui.setItem(size - 8, refresh);
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§cClose");
        closeMeta.setLore(Arrays.asList("§7Close this menu"));
        close.setItemMeta(closeMeta);
        gui.setItem(size - 2, close);
        
        // Admin panel button (if player has permission)
        if (player.hasPermission("swiftevents.admin")) {
            ItemStack adminPanel = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta adminMeta = adminPanel.getItemMeta();
            adminMeta.setDisplayName("§4Admin Panel");
            adminMeta.setLore(Arrays.asList("§7Open the admin panel"));
            adminPanel.setItemMeta(adminMeta);
            gui.setItem(size - 6, adminPanel);
        }
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
    
    // Additional utility methods for GUI functionality
    public boolean canPlayerAccessEventGUI(Player player, Event event) {
        // Check if player has permission for this event type
        String eventTypePermission = "swiftevents.event." + event.getType().name().toLowerCase();
        return player.hasPermission("swiftevents.user") && 
               player.hasPermission(eventTypePermission);
    }
    
    public void refreshCurrentGUI(Player player) {
        String title = player.getOpenInventory().getTitle();
        
        if (title.startsWith(plugin.getConfigManager().getGUITitle())) {
            // Extract page from title and refresh events GUI
            int page = extractPageFromTitle(title);
            openEventsGUI(player, page);
        } else if (title.startsWith("§6Event: ")) {
            // Refresh event details GUI
            String eventName = title.substring("§6Event: ".length());
            Event event = findEventByName(eventName);
            if (event != null) {
                openEventDetailsGUI(player, event);
            }
        } else if (title.equals("§4Admin - Event Management")) {
            // Refresh admin GUI
            openAdminGUI(player);
        }
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
    
    private Event findEventByName(String name) {
        return plugin.getEventManager().getAllEvents().stream()
                .filter(event -> event.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
} 