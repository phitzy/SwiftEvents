package com.swiftevents.api.examples;

import com.swiftevents.api.SwiftEventsAPI;
import com.swiftevents.api.events.*;
import com.swiftevents.api.hooks.SwiftEventsHook;
import com.swiftevents.events.Event;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Example integration class showing how other plugins can integrate with SwiftEvents
 * 
 * This demonstrates:
 * 1. Using the static API
 * 2. Listening to custom Bukkit events
 * 3. Implementing a SwiftEventsHook
 * 4. Best practices for integration
 */
public class ExampleIntegration extends JavaPlugin implements Listener, SwiftEventsHook {
    
    private Logger logger;
    
    @Override
    public void onEnable() {
        logger = getLogger();
        
        // Method 1: Try to initialize SwiftEvents API
        if (!SwiftEventsAPI.tryInitialize()) {
            logger.warning("SwiftEvents not found! Some features will be disabled.");
            return;
        }
        
        logger.info("SwiftEvents found! Version: " + SwiftEventsAPI.getVersion());
        
        // Method 2: Register as a Bukkit event listener for SwiftEvents events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Method 3: Register as a SwiftEvents hook for deeper integration
        if (SwiftEventsAPI.registerHook(this)) {
            logger.info("Successfully registered SwiftEvents integration hook!");
        }
        
        // Example: Create a test event
        createExampleEvent();
    }
    
    @Override
    public void onDisable() {
        // Clean up: Unregister hook
        if (SwiftEventsAPI.isAvailable()) {
            SwiftEventsAPI.unregisterHook(getHookName());
        }
    }
    
    // ===== BUKKIT EVENT LISTENERS =====
    
    @EventHandler
    public void onSwiftEventCreate(SwiftEventCreateEvent event) {
        Event swiftEvent = event.getSwiftEvent();
        logger.info("SwiftEvent created: " + swiftEvent.getName() + " (Type: " + swiftEvent.getType() + ")");
        
        // Example: Add custom metadata to PvP events
        if (swiftEvent.getType() == Event.EventType.PVP) {
            swiftEvent.addMetadata("example_plugin_enhanced", true);
            logger.info("Enhanced PvP event with custom features!");
        }
    }
    
    @EventHandler
    public void onSwiftEventStart(SwiftEventStartEvent event) {
        Event swiftEvent = event.getSwiftEvent();
        logger.info("SwiftEvent started: " + swiftEvent.getName());
        
        // Example: Announce special events
        if (swiftEvent.getType() == Event.EventType.TREASURE_HUNT) {
            Bukkit.broadcastMessage("§6[ExamplePlugin] §eA treasure hunt has begun! Good luck!");
        }
    }
    
    @EventHandler
    public void onSwiftEventEnd(SwiftEventEndEvent event) {
        Event swiftEvent = event.getSwiftEvent();
        String reason = event.getEndReason();
        logger.info("SwiftEvent ended: " + swiftEvent.getName() + " (Reason: " + reason + ")");
        
        // Example: Give bonus rewards for completed events
        if ("completed".equals(reason) && swiftEvent.getCurrentParticipants() >= 5) {
            SwiftEventsAPI.broadcastToEvent(swiftEvent, 
                "§a[ExamplePlugin] Bonus reward for large participation!");
        }
    }
    
    @EventHandler
    public void onPlayerJoinSwiftEvent(SwiftEventPlayerJoinEvent event) {
        Player player = event.getPlayer();
        Event swiftEvent = event.getSwiftEvent();
        
        if (player != null) {
            logger.info(player.getName() + " joined event: " + swiftEvent.getName());
            
            // Example: Cancel join if player doesn't meet custom requirements
            if (swiftEvent.getType() == Event.EventType.PVP && !player.hasPermission("example.pvp")) {
                event.setCancelled(true);
                player.sendMessage("§c[ExamplePlugin] You need PvP permission to join this event!");
            }
        }
    }
    
    @EventHandler
    public void onPlayerLeaveSwiftEvent(SwiftEventPlayerLeaveEvent event) {
        Player player = event.getPlayer();
        Event swiftEvent = event.getSwiftEvent();
        String reason = event.getLeaveReason();
        
        if (player != null) {
            logger.info(player.getName() + " left event: " + swiftEvent.getName() + " (Reason: " + reason + ")");
        }
    }
    
    // ===== SWIFTEVENTS HOOK IMPLEMENTATION =====
    
    @Override
    public String getHookName() {
        return "ExamplePlugin";
    }
    
    @Override
    public int getPriority() {
        return 50; // Medium priority
    }
    
    @Override
    public boolean onEventPreCreate(Event event) {
        // Example: Limit the number of building contests per day
        if (event.getType() == Event.EventType.BUILDING) {
            long buildingEventsToday = SwiftEventsAPI.getEventsByType(Event.EventType.BUILDING)
                    .stream()
                    .filter(e -> isToday(e.getCreatedAt()))
                    .count();
            
            if (buildingEventsToday >= 3) {
                logger.info("Blocked creation of building event - daily limit reached");
                return false; // Block creation
            }
        }
        return true;
    }
    
    @Override
    public void onEventCreated(Event event) {
        logger.info("Hook: Event created - " + event.getName());
        
        // Example: Set custom requirements for racing events
        if (event.getType() == Event.EventType.RACING) {
            event.addRequirement("min_speed", 5);
            event.addMetadata("example_track_id", "track_001");
        }
    }
    
    @Override
    public boolean onEventPreStart(Event event) {
        // Example: Ensure minimum participants for certain event types
        if (event.getType() == Event.EventType.PVP && event.getCurrentParticipants() < 2) {
            logger.info("Blocked start of PvP event - not enough participants");
            return false;
        }
        return true;
    }
    
    @Override
    public void onEventStarted(Event event) {
        logger.info("Hook: Event started - " + event.getName());
        
        // Example: Start custom timers or mechanics
        if (event.getType() == Event.EventType.TREASURE_HUNT) {
            startTreasureHuntMechanics(event);
        }
    }
    
    @Override
    public void onEventEnded(Event event, String reason) {
        logger.info("Hook: Event ended - " + event.getName() + " (Reason: " + reason + ")");
        
        // Example: Clean up custom mechanics
        cleanupCustomMechanics(event);
    }
    
    @Override
    public boolean onPlayerPreJoin(Player player, Event event) {
        // Example: Custom join validation
        if (event.getType() == Event.EventType.BUILDING && !hasBuilderRank(player)) {
            player.sendMessage("§c[ExamplePlugin] You need builder rank to join building events!");
            return false;
        }
        return true;
    }
    
    @Override
    public void onPlayerJoined(Player player, Event event) {
        logger.info("Hook: Player joined - " + player.getName() + " -> " + event.getName());
        
        // Example: Give player custom items for the event
        giveEventItems(player, event);
    }
    
    @Override
    public void onPlayerLeft(Player player, UUID playerId, Event event, String reason) {
        if (player != null) {
            logger.info("Hook: Player left - " + player.getName() + " <- " + event.getName());
            
            // Example: Remove custom items
            removeEventItems(player, event);
        }
    }
    
    @Override
    public void onEventUpdate(Event event) {
        // Called every minute for active events
        // Example: Check custom win conditions
        if (event.getType() == Event.EventType.CUSTOM) {
            checkCustomWinConditions(event);
        }
    }
    
    @Override
    public void onPluginDisable() {
        logger.info("SwiftEvents is shutting down - cleaning up");
        // Clean up any resources
    }
    
    // ===== HELPER METHODS =====
    
    private void createExampleEvent() {
        if (!SwiftEventsAPI.isAvailable()) return;
        
        Event example = SwiftEventsAPI.createEvent(
            "Example Event",
            "An example event created by ExamplePlugin",
            Event.EventType.MINI_GAME,
            null // No specific creator
        );
        
        if (example != null) {
            logger.info("Created example event: " + example.getId());
        }
    }
    
    private boolean isToday(long timestamp) {
        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
        long eventDay = timestamp / (1000 * 60 * 60 * 24);
        return today == eventDay;
    }
    
    private boolean hasBuilderRank(Player player) {
        // Example implementation - check permission or rank
        return player.hasPermission("example.builder");
    }
    
    private void giveEventItems(Player player, Event event) {
        // Example: Give items based on event type
        logger.info("Giving event items to " + player.getName() + " for " + event.getType());
    }
    
    private void removeEventItems(Player player, Event event) {
        // Example: Remove event-specific items
        logger.info("Removing event items from " + player.getName());
    }
    
    private void startTreasureHuntMechanics(Event event) {
        // Example: Start custom treasure hunt mechanics
        logger.info("Starting treasure hunt mechanics for " + event.getName());
    }
    
    private void cleanupCustomMechanics(Event event) {
        // Example: Clean up any custom mechanics
        logger.info("Cleaning up mechanics for " + event.getName());
    }
    
    private void checkCustomWinConditions(Event event) {
        // Example: Check if someone won the custom event
        // This could end the event early if conditions are met
    }
} 