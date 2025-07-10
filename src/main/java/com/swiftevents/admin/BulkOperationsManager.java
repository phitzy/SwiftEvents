package com.swiftevents.admin;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.permissions.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles bulk operations on multiple events for administrative efficiency
 */
public class BulkOperationsManager {
    
    private final SwiftEventsPlugin plugin;
    private final Map<UUID, BulkOperationSession> activeSessions = new HashMap<>();
    
    public BulkOperationsManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open the main bulk operations GUI
     */
    public void openBulkOperationsGUI(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_BASE)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§c§lBulk Event Operations");
        
        // Header
        addHeader(gui);
        
        // Selection Tools
        addSelectionTools(gui);
        
        // Operation Tools
        addOperationTools(gui);
        
        // Status Information
        addStatusInfo(gui, player);
        
        // Navigation
        addNavigation(gui);
        
        player.openInventory(gui);
    }
    
    private void addHeader(Inventory gui) {
        ItemStack header = createItem(Material.COMMAND_BLOCK, "§6§lBulk Operations Dashboard",
            "§7Perform operations on multiple events",
            "§7Select events and choose an operation",
            "§7to apply to all selected events",
            "",
            "§eSelect events below to get started");
        gui.setItem(4, header);
    }
    
    private void addSelectionTools(Inventory gui) {
        // Select All Active
        gui.setItem(9, createItem(Material.LIME_CONCRETE, "§a§lSelect All Active",
            "§7Select all currently active events",
            "§7for bulk operations",
            "",
            "§eClick to select"));
        
        // Select by Type
        gui.setItem(10, createItem(Material.GOLDEN_PICKAXE, "§e§lSelect by Type",
            "§7Choose events by their type",
            "§7(PvP, Building, Racing, etc.)",
            "",
            "§eClick to filter"));
        
        // Select by Status
        gui.setItem(11, createItem(Material.WRITTEN_BOOK, "§6§lSelect by Status",
            "§7Choose events by their status",
            "§7(Active, Scheduled, Completed, etc.)",
            "",
            "§eClick to filter"));
        
        // Select by Date Range
        gui.setItem(12, createItem(Material.CLOCK, "§d§lSelect by Date",
            "§7Choose events within a date range",
            "§7for time-based operations",
            "",
            "§eClick to configure"));
        
        // Custom Selection
        gui.setItem(13, createItem(Material.PAPER, "§b§lCustom Selection",
            "§7Manually select specific events",
            "§7from a detailed list",
            "",
            "§eClick to browse"));
        
        // Clear Selection
        gui.setItem(14, createItem(Material.BARRIER, "§c§lClear Selection",
            "§7Clear all selected events",
            "",
            "§eClick to clear"));
    }
    
    private void addOperationTools(Inventory gui) {
        // Start Operations
        gui.setItem(19, createItem(Material.GREEN_CONCRETE, "§a§lStart Selected",
            "§7Start all selected events",
            "§cWarning: This will immediately",
            "§cstart all selected events",
            "",
            "§eClick to execute"));
        
        // Stop Operations
        gui.setItem(20, createItem(Material.RED_CONCRETE, "§c§lStop Selected",
            "§7Stop all selected events",
            "§cWarning: This will immediately",
            "§cend all selected events",
            "",
            "§eClick to execute"));
        
        // Cancel Operations
        gui.setItem(21, createItem(Material.LAVA_BUCKET, "§4§lCancel Selected",
            "§7Cancel all selected events",
            "§cWarning: This action cannot",
            "§cbe undone",
            "",
            "§eShift-click to confirm"));
        
        // Delete Operations
        gui.setItem(22, createItem(Material.TNT, "§4§lDelete Selected",
            "§7Permanently delete selected events",
            "§cDANGER: This completely removes",
            "§cevents and cannot be undone",
            "",
            "§cShift-click TWICE to confirm"));
        
        // Modify Settings
        gui.setItem(28, createItem(Material.ANVIL, "§6§lModify Settings",
            "§7Bulk modify event settings",
            "§7- Change max participants",
            "§7- Adjust duration",
            "§7- Update descriptions",
            "",
            "§eClick to configure"));
        
        // Export Selected
        gui.setItem(29, createItem(Material.ENDER_CHEST, "§b§lExport Selected",
            "§7Export selected events to file",
            "§7for backup or transfer",
            "",
            "§eClick to export"));
        
        // Clone Selected
        gui.setItem(30, createItem(Material.PAPER, "§d§lClone Selected",
            "§7Create copies of selected events",
            "§7with optional modifications",
            "",
            "§eClick to clone"));
        
        // Schedule Selected
        gui.setItem(31, createItem(Material.CLOCK, "§e§lSchedule Selected",
            "§7Set start times for selected events",
            "§7or reschedule existing events",
            "",
            "§eClick to schedule"));
    }
    
    private void addStatusInfo(Inventory gui, Player player) {
        BulkOperationSession session = activeSessions.get(player.getUniqueId());
        int selectedCount = session != null ? session.getSelectedEvents().size() : 0;
        
        ItemStack statusItem = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta statusMeta = statusItem.getItemMeta();
        statusMeta.setDisplayName("§f§lSelection Status");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Selected Events: §f" + selectedCount);
        
        if (selectedCount > 0) {
            Map<Event.EventType, Long> typeCount = session.getSelectedEvents().stream()
                .collect(Collectors.groupingBy(Event::getType, Collectors.counting()));
            
            lore.add("§7Breakdown:");
            for (Map.Entry<Event.EventType, Long> entry : typeCount.entrySet()) {
                lore.add("§7- " + entry.getKey().name() + ": §f" + entry.getValue());
            }
        }
        
        lore.add("");
        lore.add("§eClick to view selected events");
        
        statusMeta.setLore(lore);
        statusItem.setItemMeta(statusMeta);
        gui.setItem(40, statusItem);
        
        // Quick Stats
        List<Event> allEvents = plugin.getEventManager().getAllEvents();
        gui.setItem(41, createItem(Material.BOOK, "§f§lQuick Stats",
            "§7Total Events: §f" + allEvents.size(),
            "§7Active: §a" + allEvents.stream().mapToInt(e -> e.isActive() ? 1 : 0).sum(),
            "§7Scheduled: §e" + allEvents.stream().mapToInt(e -> e.isScheduled() ? 1 : 0).sum(),
            "§7Completed: §2" + allEvents.stream().mapToInt(e -> e.isCompleted() ? 1 : 0).sum(),
            "",
            "§eGeneral event overview"));
    }
    
    private void addNavigation(Inventory gui) {
        gui.setItem(45, createItem(Material.ARROW, "§7« Back to Admin Panel",
            "§7Return to the main admin dashboard"));
        
        gui.setItem(49, createItem(Material.LIME_CONCRETE, "§a§lRefresh",
            "§7Update event information",
            "§7and refresh the interface",
            "",
            "§eClick to refresh"));
        
        gui.setItem(53, createItem(Material.BARRIER, "§c§lClose",
            "§7Close this interface"));
    }
    
    /**
     * Open event selection GUI by type
     */
    public void openTypeSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, "§e§lSelect Events by Type");
        
        Event.EventType[] types = Event.EventType.values();
        int slot = 9;
        
        for (Event.EventType type : types) {
            List<Event> typeEvents = plugin.getEventManager().getEventsByType(type);
            
            ItemStack typeItem = new ItemStack(getTypeMaterial(type));
            ItemMeta meta = typeItem.getItemMeta();
            meta.setDisplayName("§f" + type.name().replace("_", " ") + " Events");
            meta.setLore(Arrays.asList(
                "§7Available: §f" + typeEvents.size(),
                "§7Active: §a" + typeEvents.stream().mapToInt(e -> e.isActive() ? 1 : 0).sum(),
                "",
                "§eClick to select all " + type.name().toLowerCase() + " events"
            ));
            typeItem.setItemMeta(meta);
            
            gui.setItem(slot++, typeItem);
        }
        
        gui.setItem(31, createItem(Material.ARROW, "§7« Back to Bulk Operations"));
        
        player.openInventory(gui);
    }
    
    /**
     * Execute bulk operation on selected events
     */
    public void executeBulkOperation(Player player, BulkOperationType operation) {
        BulkOperationSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.getSelectedEvents().isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNo events selected!");
            return;
        }
        
        List<Event> selectedEvents = new ArrayList<>(session.getSelectedEvents());
        
        player.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§7Starting bulk operation: §f" + operation.name() + " §7on §f" + selectedEvents.size() + " §7events...");
        
        // Execute operation asynchronously with progress updates
        new BukkitRunnable() {
            private final AtomicInteger processed = new AtomicInteger(0);
            private final AtomicInteger successful = new AtomicInteger(0);
            
            @Override
            public void run() {
                CompletableFuture.supplyAsync(() -> {
                    for (Event event : selectedEvents) {
                        try {
                            boolean success = executeOperationOnEvent(event, operation);
                            if (success) {
                                successful.incrementAndGet();
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to execute " + operation.name() + 
                                " on event " + event.getName() + ": " + e.getMessage());
                        }
                        
                        int current = processed.incrementAndGet();
                        
                        // Update progress every 5 events or at the end
                        if (current % 5 == 0 || current == selectedEvents.size()) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                                    "§7Progress: §f" + current + "/" + selectedEvents.size() + 
                                    " §7(§a" + successful.get() + " §7successful)");
                            });
                        }
                    }
                    return null;
                }).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§aBulk operation completed! §f" + successful.get() + "/" + selectedEvents.size() + 
                            " §aevents processed successfully.");
                        
                        // Clear selection after operation
                        session.clearSelection();
                        
                        // Play completion sound
                        player.playSound(player.getLocation(), 
                            org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    });
                });
            }
        }.runTaskAsynchronously(plugin);
    }
    
    private boolean executeOperationOnEvent(Event event, BulkOperationType operation) {
        return switch (operation) {
            case START -> plugin.getEventManager().startEvent(event.getId());
            case STOP -> plugin.getEventManager().endEvent(event.getId());
            case CANCEL -> plugin.getEventManager().cancelEvent(event.getId());
            case DELETE -> plugin.getEventManager().deleteEvent(event.getId());
            case PAUSE -> plugin.getEventManager().pauseEvent(event.getId());
            case RESUME -> plugin.getEventManager().resumeEvent(event.getId());
            default -> false;
        };
    }
    
    /**
     * Get or create bulk operation session for player
     */
    public BulkOperationSession getOrCreateSession(Player player) {
        return activeSessions.computeIfAbsent(player.getUniqueId(), 
            k -> new BulkOperationSession(player.getUniqueId()));
    }
    
    private Material getTypeMaterial(Event.EventType type) {
        return switch (type) {
            case PVP -> Material.DIAMOND_SWORD;
            case PVE -> Material.BOW;
            case BUILDING -> Material.GOLDEN_PICKAXE;
            case RACING -> Material.GOLDEN_BOOTS;
            case TREASURE_HUNT -> Material.COMPASS;
            case MINI_GAME -> Material.SLIME_BALL;
            case CUSTOM -> Material.COMMAND_BLOCK;
            case TOURNAMENT -> Material.DIAMOND_SWORD;
            case CHALLENGE -> Material.DIAMOND_SWORD;
            default -> Material.BARRIER;
        };
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
    
    /**
     * Bulk operation session data
     */
    public static class BulkOperationSession {
        private final UUID playerId;
        private final Set<Event> selectedEvents = new HashSet<>();
        private final long createdAt = System.currentTimeMillis();
        
        public BulkOperationSession(UUID playerId) {
            this.playerId = playerId;
        }
        
        public void addEvent(Event event) {
            selectedEvents.add(event);
        }
        
        public void removeEvent(Event event) {
            selectedEvents.remove(event);
        }
        
        public void clearSelection() {
            selectedEvents.clear();
        }
        
        public Set<Event> getSelectedEvents() {
            return new HashSet<>(selectedEvents);
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public boolean hasSelection() {
            return !selectedEvents.isEmpty();
        }
    }
    
    /**
     * Available bulk operation types
     */
    public enum BulkOperationType {
        START, STOP, CANCEL, DELETE, PAUSE, RESUME
    }
} 