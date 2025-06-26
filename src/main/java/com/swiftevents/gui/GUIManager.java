package com.swiftevents.gui;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.permissions.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GUIManager {
    
    private final SwiftEventsPlugin plugin;
    
    // Optimization: Concurrent session management with cleanup
    private final Map<UUID, GUISession> activeSessions = new ConcurrentHashMap<>(32);
    private final Map<UUID, Long> sessionLastAccess = new ConcurrentHashMap<>(32);
    
    // Optimization: ItemStack pooling to reduce garbage collection
    private final Map<String, ItemStack> itemCache = new HashMap<>(64);
    private final Map<String, Long> itemCacheTimestamps = new HashMap<>(64);
    
    // Optimization: Pre-allocated collections
    private final List<Event> eventBuffer = new ArrayList<>(32);
    private final List<String> loreBuffer = new ArrayList<>(8);
    
    // Optimization: String builder pool
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = 
        ThreadLocal.withInitial(() -> new StringBuilder(128));
    
    // Cache management
    private static final long SESSION_TIMEOUT = 600000; // 10 minutes
    private static final long ITEM_CACHE_DURATION = 300000; // 5 minutes
    private static final long CLEANUP_INTERVAL = 60000; // 1 minute
    
    private BukkitTask cleanupTask;
    
    public GUIManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        
        // Start periodic cleanup task
        startCleanupTask();
    }
    
    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
            this::cleanupExpiredData, 20L * 60, 20L * 60); // Every minute
    }
    
    private void cleanupExpiredData() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up expired sessions
        Iterator<Map.Entry<UUID, Long>> sessionIterator = sessionLastAccess.entrySet().iterator();
        while (sessionIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = sessionIterator.next();
            if (currentTime - entry.getValue() > SESSION_TIMEOUT) {
                sessionIterator.remove();
                activeSessions.remove(entry.getKey());
            }
        }
        
        // Clean up expired item cache
        Iterator<Map.Entry<String, Long>> itemIterator = itemCacheTimestamps.entrySet().iterator();
        while (itemIterator.hasNext()) {
            Map.Entry<String, Long> entry = itemIterator.next();
            if (currentTime - entry.getValue() > ITEM_CACHE_DURATION) {
                itemIterator.remove();
                itemCache.remove(entry.getKey());
            }
        }
    }
    
    public GUISession getOrCreateSession(UUID playerId) {
        sessionLastAccess.put(playerId, System.currentTimeMillis());
        return activeSessions.computeIfAbsent(playerId, GUISession::new);
    }
    
    public void removeSession(UUID playerId) {
        activeSessions.remove(playerId);
        sessionLastAccess.remove(playerId);
    }
    
    // Optimization: Efficient event filtering without creating intermediate streams
    private List<Event> filterEvents(List<Event> events, EventFilter filter, Player player) {
        eventBuffer.clear();
        
        for (Event event : events) {
            if (filter.matches(event, player)) {
                eventBuffer.add(event);
            }
        }
        
        return new ArrayList<>(eventBuffer);
    }
    
    private List<Event> sortEvents(List<Event> events, EventSort sort) {
        events.sort(sort.getComparator());
        return events;
    }
    
    // Enhanced Events GUI with filtering and sorting
    public void openEventsGUI(Player player) {
        openEventsGUI(player, 0, EventFilter.ALL, EventSort.NAME); 
    }
    
    public void openEventsGUI(Player player, int page) {
        openEventsGUI(player, page, EventFilter.ALL, EventSort.NAME);
    }
    
    public void openEventsGUI(Player player, int page, EventFilter filter, EventSort sort) {
        if (!plugin.getConfigManager().isGUIEnabled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cGUI is currently disabled.");
            return;
        }
        
        // Create or update GUI session
        GUISession session = getOrCreateSession(player.getUniqueId());
        session.setCurrentFilter(filter);
        session.setCurrentSort(sort);
        session.setCurrentPage(page);
        
        List<Event> allEvents = plugin.getEventManager().getAllEvents();
        List<Event> filteredEvents = filterEvents(allEvents, filter, player);
        List<Event> sortedEvents = sortEvents(filteredEvents, sort);
        
        // Handle empty events list
        if (sortedEvents.isEmpty()) {
            openEmptyEventsGUI(player, filter);
            return;
        }
        
        // Pagination calculation
        int eventsPerPage = 28; // 4 rows for events, more room for controls
        int totalPages = (int) Math.ceil((double) sortedEvents.size() / eventsPerPage);
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        int size = 54;
        
        // Optimization: Use StringBuilder for title construction
        StringBuilder titleBuilder = STRING_BUILDER.get();
        titleBuilder.setLength(0);
        titleBuilder.append(plugin.getConfigManager().getGUITitle())
                   .append(" (").append(page + 1).append("/").append(totalPages).append(")");
        
        if (filter != EventFilter.ALL) {
            titleBuilder.append(" [").append(filter.getDisplayName()).append("]");
        }
        
        String title = titleBuilder.toString();
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Add events for current page
        int startIndex = page * eventsPerPage;
        int endIndex = Math.min(startIndex + eventsPerPage, sortedEvents.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Event event = sortedEvents.get(i);
            ItemStack item = createEventItem(event, player);
            gui.setItem(i - startIndex, item);
        }
        
        // Add filter and sort controls
        addFilterControls(gui, player, filter, sort);
        addNavigationItems(gui, player, page, totalPages);
        
        player.openInventory(gui);
    }
    
    private void openEmptyEventsGUI(Player player, EventFilter filter) {
        Inventory gui = Bukkit.createInventory(null, 54, plugin.getConfigManager().getGUITitle());
        
        // Use cached barrier item
        ItemStack noEvents = getCachedItem("barrier_no_events", () -> {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cNo Events Available");
                loreBuffer.clear();
                loreBuffer.add("§7There are currently no events.");
                loreBuffer.add("§7Check back later!");
                meta.setLore(new ArrayList<>(loreBuffer));
                item.setItemMeta(meta);
            }
            return item;
        });
        
        // Customize based on filter
        if (filter != EventFilter.ALL) {
            ItemStack customNoEvents = noEvents.clone();
            ItemMeta meta = customNoEvents.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cNo " + filter.getDisplayName() + " Events");
                loreBuffer.clear();
                loreBuffer.add("§7No events match the current filter.");
                loreBuffer.add("§7Try changing the filter or check back later!");
                meta.setLore(new ArrayList<>(loreBuffer));
                customNoEvents.setItemMeta(meta);
            }
            gui.setItem(22, customNoEvents);
        } else {
            gui.setItem(22, noEvents);
        }
        
        addFilterControls(gui, player, filter, EventSort.NAME);
        addNavigationItems(gui, player, 0, 0);
        player.openInventory(gui);
    }
    
    // Optimization: ItemStack caching with supplier pattern
    private ItemStack getCachedItem(String cacheKey, ItemStackSupplier supplier) {
        long currentTime = System.currentTimeMillis();
        
        ItemStack cached = itemCache.get(cacheKey);
        Long cacheTime = itemCacheTimestamps.get(cacheKey);
        
        if (cached != null && cacheTime != null && 
            (currentTime - cacheTime) < ITEM_CACHE_DURATION) {
            return cached.clone(); // Return clone to prevent modification
        }
        
        ItemStack newItem = supplier.get();
        itemCache.put(cacheKey, newItem.clone());
        itemCacheTimestamps.put(cacheKey, currentTime);
        
        return newItem;
    }
    
    @FunctionalInterface
    private interface ItemStackSupplier {
        ItemStack get();
    }
    
    private void addFilterControls(Inventory gui, Player player, EventFilter currentFilter, EventSort currentSort) {
        // Filter button - use cached base item
        ItemStack filterButton = getCachedItem("filter_button_base", () -> {
            ItemStack item = new ItemStack(Material.HOPPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Filter Events");
                item.setItemMeta(meta);
            }
            return item;
        });
        
        // Customize for current filter
        ItemMeta filterMeta = filterButton.getItemMeta();
        if (filterMeta != null) {
            filterMeta.setDisplayName("§6Filter: " + currentFilter.getDisplayName());
            
            loreBuffer.clear();
            loreBuffer.add("§7Current filter: §f" + currentFilter.getDisplayName());
            loreBuffer.add("");
            loreBuffer.add("§7Available filters:");
            
            for (EventFilter filter : EventFilter.values()) {
                String prefix = filter == currentFilter ? "§a▶ " : "§7- ";
                loreBuffer.add(prefix + filter.getDisplayName());
            }
            loreBuffer.add("");
            loreBuffer.add("§eClick to change filter");
            
            filterMeta.setLore(new ArrayList<>(loreBuffer));
            filterButton.setItemMeta(filterMeta);
        }
        gui.setItem(45, filterButton);
        
        // Sort button - similar optimization
        ItemStack sortButton = getCachedItem("sort_button_base", () -> {
            ItemStack item = new ItemStack(Material.COMPARATOR);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Sort Events");
                item.setItemMeta(meta);
            }
            return item;
        });
        
        ItemMeta sortMeta = sortButton.getItemMeta();
        if (sortMeta != null) {
            sortMeta.setDisplayName("§6Sort: " + currentSort.getDisplayName());
            
            loreBuffer.clear();
            loreBuffer.add("§7Current sort: §f" + currentSort.getDisplayName());
            loreBuffer.add("");
            loreBuffer.add("§7Available sorts:");
            
            for (EventSort sort : EventSort.values()) {
                String prefix = sort == currentSort ? "§a▶ " : "§7- ";
                loreBuffer.add(prefix + sort.getDisplayName());
            }
            loreBuffer.add("");
            loreBuffer.add("§eClick to change sort");
            
            sortMeta.setLore(new ArrayList<>(loreBuffer));
            sortButton.setItemMeta(sortMeta);
        }
        gui.setItem(46, sortButton);
        
        // Statistics button - cached
        ItemStack statsButton = getCachedItem("stats_button", () -> {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§bEvent Statistics");
                loreBuffer.clear();
                loreBuffer.add("§7View detailed event statistics");
                loreBuffer.add("§7and analytics dashboard");
                loreBuffer.add("");
                loreBuffer.add("§eClick to view statistics");
                meta.setLore(new ArrayList<>(loreBuffer));
                item.setItemMeta(meta);
            }
            return item;
        });
        gui.setItem(47, statsButton);
    }
    
    public void openAdminGUI(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_BASE)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou don't have permission to access the admin GUI.");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 45, "§4Admin - Event Management");
        
        // Use cached admin items
        ItemStack createEvent = getCachedItem("admin_create", () -> {
            ItemStack item = new ItemStack(Material.EMERALD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aCreate New Event");
                loreBuffer.clear();
                loreBuffer.add("§7Click to create a new event");
                meta.setLore(new ArrayList<>(loreBuffer));
                item.setItemMeta(meta);
            }
            return item;
        });
        gui.setItem(10, createEvent);
        
        ItemStack activeEvents = getCachedItem("admin_active", () -> {
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Active Events");
                loreBuffer.clear();
                loreBuffer.add("§7View and manage active events");
                meta.setLore(new ArrayList<>(loreBuffer));
                item.setItemMeta(meta);
            }
            return item;
        });
        gui.setItem(12, activeEvents);
        
        ItemStack close = getCachedItem("close_button", () -> {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cClose");
                loreBuffer.clear();
                loreBuffer.add("§7Close this GUI");
                meta.setLore(new ArrayList<>(loreBuffer));
                item.setItemMeta(meta);
            }
            return item;
        });
        gui.setItem(40, close);
        
        player.openInventory(gui);
    }
    
    public void openEventDetailsGUI(Player player, Event event) {
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§6Event: " + event.getName());
        
        // Event info item - dynamically generated due to changing data
        ItemStack eventInfo = new ItemStack(getEventMaterial(event));
        ItemMeta infoMeta = eventInfo.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6" + event.getName());
            
            loreBuffer.clear();
            loreBuffer.add("§7Type: §f" + event.getType().name());
            loreBuffer.add("§7Status: " + getStatusColor(event.getStatus()) + event.getStatus().name());
            loreBuffer.add("§7Participants: §f" + event.getCurrentParticipants() + 
                          (event.hasUnlimitedSlots() ? " (Unlimited)" : "/" + event.getMaxParticipants()));
            
            if (event.hasLocation()) {
                loreBuffer.add("§7Location: §f" + event.getWorld() + " " + 
                              (int)event.getX() + ", " + (int)event.getY() + ", " + (int)event.getZ());
            }
            
            if (event.getStartTime() > 0) {
                loreBuffer.add("§7Remaining: §f" + event.getFormattedRemainingTime());
            }
            
            loreBuffer.add("");
            loreBuffer.add("§7Description:");
            loreBuffer.add("§f" + event.getDescription());
            
            if (event.isParticipant(player.getUniqueId())) {
                loreBuffer.add("");
                loreBuffer.add("§a✓ You are participating in this event");
            } else if (event.canJoin()) {
                loreBuffer.add("");
                loreBuffer.add("§eClick to join this event!");
            } else {
                loreBuffer.add("");
                loreBuffer.add("§c" + getJoinBlockReason(event));
            }
            
            infoMeta.setLore(new ArrayList<>(loreBuffer));
            eventInfo.setItemMeta(infoMeta);
        }
        gui.setItem(13, eventInfo);
        
        // Action buttons
        if (event.isParticipant(player.getUniqueId())) {
            ItemStack leaveButton = getCachedItem("leave_button", () -> {
                ItemStack item = new ItemStack(Material.RED_CONCRETE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§cLeave Event");
                    loreBuffer.clear();
                    loreBuffer.add("§7Click to leave this event");
                    meta.setLore(new ArrayList<>(loreBuffer));
                    item.setItemMeta(meta);
                }
                return item;
            });
            gui.setItem(29, leaveButton);
        } else if (event.canJoin()) {
            ItemStack joinButton = getCachedItem("join_button", () -> {
                ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§aJoin Event");
                    loreBuffer.clear();
                    loreBuffer.add("§7Click to join this event");
                    meta.setLore(new ArrayList<>(loreBuffer));
                    item.setItemMeta(meta);
                }
                return item;
            });
            gui.setItem(29, joinButton);
        }
        
        // Teleport button if location is set
        if (event.hasLocation()) {
            ItemStack teleportButton = getCachedItem("teleport_button", () -> {
                ItemStack item = new ItemStack(Material.ENDER_PEARL);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§bTeleport to Event");
                    loreBuffer.clear();
                    loreBuffer.add("§7Click to teleport to the event location");
                    meta.setLore(new ArrayList<>(loreBuffer));
                    item.setItemMeta(meta);
                }
                return item;
            });
            gui.setItem(31, teleportButton);
        }
        
        // Back button
        ItemStack backButton = getCachedItem("back_button", () -> {
            ItemStack item = new ItemStack(Material.ARROW);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§7← Back to Events");
                loreBuffer.clear();
                loreBuffer.add("§7Return to the events list");
                meta.setLore(new ArrayList<>(loreBuffer));
                item.setItemMeta(meta);
            }
            return item;
        });
        gui.setItem(45, backButton);
        
        // Admin controls
        if (player.hasPermission(Permissions.ADMIN_BASE)) {
            addAdminControls(gui, event);
        }
        
        player.openInventory(gui);
    }
    
    // Optimization: Efficient event item creation with caching where possible
    private ItemStack createEventItem(Event event, Player player) {
        ItemStack item = new ItemStack(getEventMaterial(event));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.setDisplayName("§6" + event.getName());
        
        // Build lore efficiently
        loreBuffer.clear();
        loreBuffer.add("§7Type: §f" + event.getType().name());
        loreBuffer.add("§7Status: " + getStatusColor(event.getStatus()) + event.getStatus().name());
        loreBuffer.add("§7Participants: §f" + event.getCurrentParticipants() + 
                      (event.hasUnlimitedSlots() ? " (Unlimited)" : "/" + event.getMaxParticipants()));
        
        if (event.getStartTime() > 0) {
            loreBuffer.add("§7Time: §f" + event.getFormattedRemainingTime());
        }
        
        loreBuffer.add("");
        loreBuffer.add("§7" + event.getDescription());
        loreBuffer.add("");
        
        if (event.isParticipant(player.getUniqueId())) {
            loreBuffer.add("§a✓ Participating");
        } else if (event.canJoin()) {
            loreBuffer.add("§eClick to join!");
        } else {
            loreBuffer.add("§c" + getJoinBlockReason(event));
        }
        
        meta.setLore(new ArrayList<>(loreBuffer));
        item.setItemMeta(meta);
        return item;
    }
    
    private void addNavigationItems(Inventory gui, Player player, int currentPage, int totalPages) {
        // Previous page
        if (currentPage > 0) {
            ItemStack prevPage = getCachedItem("prev_page", () -> {
                ItemStack item = new ItemStack(Material.ARROW);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§7← Previous Page");
                    item.setItemMeta(meta);
                }
                return item;
            });
            gui.setItem(48, prevPage);
        }
        
        // Page info
        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§7Page " + (currentPage + 1) + " of " + totalPages);
            pageInfo.setItemMeta(pageMeta);
        }
        gui.setItem(49, pageInfo);
        
        // Next page
        if (currentPage < totalPages - 1) {
            ItemStack nextPage = getCachedItem("next_page", () -> {
                ItemStack item = new ItemStack(Material.ARROW);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§7Next Page →");
                    item.setItemMeta(meta);
                }
                return item;
            });
            gui.setItem(50, nextPage);
        }
        
        // Admin Panel button
        if (player.hasPermission(Permissions.ADMIN_BASE)) {
            ItemStack adminButton = getCachedItem("admin_panel_button", () -> {
                ItemStack item = new ItemStack(Material.COMMAND_BLOCK);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§4Admin Panel");
                    loreBuffer.clear();
                    loreBuffer.add("§7Click to open the admin panel");
                    meta.setLore(new ArrayList<>(loreBuffer));
                    item.setItemMeta(meta);
                }
                return item;
            });
            gui.setItem(48, adminButton);
        }
        
        // Close button
        ItemStack close = getCachedItem("close_button", () -> {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cClose");
                item.setItemMeta(meta);
            }
            return item;
        });
        gui.setItem(53, close);
    }
    
    private void addAdminControls(Inventory gui, Event event) {
        // Start/Stop event button
        ItemStack controlButton;
        if (event.isActive()) {
            controlButton = getCachedItem("admin_stop", () -> {
                ItemStack item = new ItemStack(Material.RED_CONCRETE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§cStop Event");
                    loreBuffer.clear();
                    loreBuffer.add("§7End this event");
                    meta.setLore(new ArrayList<>(loreBuffer));
                    item.setItemMeta(meta);
                }
                return item;
            });
        } else if (event.canStart()) {
            controlButton = getCachedItem("admin_start", () -> {
                ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§aStart Event");
                    loreBuffer.clear();
                    loreBuffer.add("§7Start this event");
                    meta.setLore(new ArrayList<>(loreBuffer));
                    item.setItemMeta(meta);
                }
                return item;
            });
        } else {
            return; // No control available
        }
        gui.setItem(37, controlButton);
        
        // Delete event button
        ItemStack deleteButton = getCachedItem("admin_delete", () -> {
            ItemStack item = new ItemStack(Material.TNT);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cDelete Event");
                loreBuffer.clear();
                loreBuffer.add("§7§lWARNING: This cannot be undone!");
                loreBuffer.add("§7Right-click to delete this event");
                meta.setLore(new ArrayList<>(loreBuffer));
                item.setItemMeta(meta);
            }
            return item;
        });
        gui.setItem(43, deleteButton);
    }
    
    private Material getEventMaterial(Event event) {
        return switch (event.getType()) {
            case PVP -> Material.DIAMOND_SWORD;
            case PVE -> Material.IRON_SWORD;
            case BUILDING -> Material.BRICKS;
            case RACING -> Material.GOLDEN_BOOTS;
            case TREASURE_HUNT -> Material.CHEST;
            case MINI_GAME -> Material.LIME_CONCRETE;
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
        } else if (event.isCompleted()) {
            return "Event has ended";
        } else if (event.isCancelled()) {
            return "Event was cancelled";
        } else {
            return "Unknown reason";
        }
    }
    
    public boolean canPlayerAccessEventGUI(Player player, Event event) {
        if (player.hasPermission(Permissions.ADMIN_BASE)) {
            return true;
        }
        String permission;
        switch (event.getType()) {
            case PVP:
                permission = Permissions.EVENT_TYPE_PVP;
                break;
            case BUILDING:
                permission = Permissions.EVENT_TYPE_BUILDING;
                break;
            case RACING:
                permission = Permissions.EVENT_TYPE_RACING;
                break;
            case TREASURE_HUNT:
                permission = Permissions.EVENT_TYPE_TREASURE;
                break;
            default:
                permission = Permissions.EVENT_TYPE_CUSTOM;
                break;
        }
        return player.hasPermission(permission);
    }
    
    public void refreshCurrentGUI(Player player) {
        GUISession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            openEventsGUI(player, session.getCurrentPage(), 
                         session.getCurrentFilter(), session.getCurrentSort());
        }
    }
    
    public void shutdown() {
        // Cancel cleanup task
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        // Clear all caches and sessions
        activeSessions.clear();
        sessionLastAccess.clear();
        itemCache.clear();
        itemCacheTimestamps.clear();
        
        plugin.getLogger().info("GUIManager shutdown complete");
    }
} 