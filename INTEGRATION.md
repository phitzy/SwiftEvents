# SwiftEvents Integration Guide

This guide explains how other plugins can integrate with SwiftEvents to create powerful event-based interactions.

## Table of Contents
- [Quick Start](#quick-start)
- [Integration Methods](#integration-methods)
- [API Reference](#api-reference)
- [Custom Events](#custom-events)
- [Integration Hooks](#integration-hooks)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Quick Start

### Add SwiftEvents as a Dependency

**plugin.yml:**
```yaml
depend: [SwiftEvents]
# or
softdepend: [SwiftEvents]
```

**pom.xml** (if using Maven):
```xml
<dependency>
    <groupId>com.swiftevents</groupId>
    <artifactId>SwiftEvents</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Basic Integration

```java
import com.swiftevents.api.SwiftEventsAPI;
import com.swiftevents.events.Event;

public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Check if SwiftEvents is available
        if (SwiftEventsAPI.tryInitialize()) {
            getLogger().info("SwiftEvents integration enabled!");
            
            // Check if a player is in an event
            Player player = // ... get player
            if (SwiftEventsAPI.isPlayerInEvent(player)) {
                // Player is in an event
            }
        }
    }
}
```

## Integration Methods

SwiftEvents provides three main ways to integrate:

### 1. Static API (`SwiftEventsAPI`)
- **Best for:** Simple queries and operations
- **Usage:** Static methods for common operations
- **Example:** Checking if a player is in an event

### 2. Custom Bukkit Events
- **Best for:** Reacting to SwiftEvents actions
- **Usage:** Standard Bukkit event listeners
- **Example:** Giving rewards when events end

### 3. Integration Hooks (`SwiftEventsHook`)
- **Best for:** Deep integration and event modification
- **Usage:** Implement the `SwiftEventsHook` interface
- **Example:** Custom validation logic, event enhancement

## API Reference

### Static API Methods

```java
// Player queries
boolean isPlayerInEvent(Player player)
List<Event> getPlayerEvents(Player player)

// Event queries
List<Event> getActiveEvents()
List<Event> getJoinableEvents()
List<Event> getEventsByType(Event.EventType type)

// Event management
Event createEvent(String name, String description, Event.EventType type, Player creator)
Event createAndStartEvent(String name, String description, Event.EventType type, Player creator, long durationSeconds)
boolean startEvent(Event event)
boolean endEvent(Event event)
boolean cancelEvent(Event event)

// Player management
boolean joinEvent(Player player, Event event)
boolean leaveEvent(Player player, Event event)
int broadcastToEvent(Event event, String message)

// Hook management
boolean registerHook(SwiftEventsHook hook)
boolean unregisterHook(String hookName)

// Utility
boolean isAvailable()
String getVersion()
boolean isFeatureEnabled(String feature)
```

### Event Types

```java
public enum EventType {
    PVP,            // Player vs Player events
    PVE,            // Player vs Environment events
    BUILDING,       // Building contests
    RACING,         // Racing events
    TREASURE_HUNT,  // Treasure hunt events
    MINI_GAME,      // Mini-games
    CUSTOM          // Custom event types
}
```

### Event Status

```java
public enum EventStatus {
    CREATED,    // Event created but not started
    SCHEDULED,  // Event scheduled to start later
    ACTIVE,     // Event currently running
    PAUSED,     // Event temporarily paused
    COMPLETED,  // Event finished successfully
    CANCELLED   // Event was cancelled
}
```

## Custom Events

SwiftEvents fires custom Bukkit events that you can listen to:

### Available Events

- `SwiftEventCreateEvent` - When an event is created (cancellable)
- `SwiftEventStartEvent` - When an event starts (cancellable)
- `SwiftEventEndEvent` - When an event ends
- `SwiftEventPlayerJoinEvent` - When a player joins an event (cancellable)
- `SwiftEventPlayerLeaveEvent` - When a player leaves an event

### Example Event Listener

```java
@EventHandler
public void onSwiftEventStart(SwiftEventStartEvent event) {
    Event swiftEvent = event.getSwiftEvent();
    
    // Announce PvP events
    if (swiftEvent.getType() == Event.EventType.PVP) {
        Bukkit.broadcastMessage("§c[PvP] Event started: " + swiftEvent.getName());
    }
    
    // Cancel events with no participants
    if (swiftEvent.getCurrentParticipants() == 0) {
        event.setCancelled(true);
    }
}

@EventHandler
public void onPlayerJoinEvent(SwiftEventPlayerJoinEvent event) {
    Player player = event.getPlayer();
    Event swiftEvent = event.getSwiftEvent();
    
    // Custom validation
    if (swiftEvent.getType() == Event.EventType.BUILDING && !player.hasPermission("myplugin.build")) {
        event.setCancelled(true);
        player.sendMessage("§cYou need build permission to join building events!");
    }
}
```

## Integration Hooks

For deeper integration, implement the `SwiftEventsHook` interface:

```java
public class MyIntegrationHook implements SwiftEventsHook {
    
    @Override
    public String getHookName() {
        return "MyPlugin";
    }
    
    @Override
    public int getPriority() {
        return 100; // Lower number = higher priority
    }
    
    @Override
    public boolean onEventPreCreate(Event event) {
        // Called before event creation
        // Return false to prevent creation
        
        // Example: Limit daily events
        if (getTodayEventCount() >= 10) {
            return false;
        }
        return true;
    }
    
    @Override
    public void onEventCreated(Event event) {
        // Called after event creation
        // Modify event properties here
        
        // Example: Add custom metadata
        if (event.getType() == Event.EventType.PVP) {
            event.addMetadata("pvp_mode", "hardcore");
        }
    }
    
    @Override
    public boolean onPlayerPreJoin(Player player, Event event) {
        // Called before player joins
        // Return false to prevent join
        
        // Example: Check custom requirements
        return hasRequiredItems(player, event);
    }
    
    @Override
    public void onPlayerJoined(Player player, Event event) {
        // Called after player joins
        giveEventItems(player, event);
    }
    
    @Override
    public void onEventUpdate(Event event) {
        // Called every minute for active events
        checkCustomWinConditions(event);
    }
}

// Register the hook
SwiftEventsAPI.registerHook(new MyIntegrationHook());
```

## Examples

### Example 1: Economy Integration

```java
@EventHandler
public void onEventEnd(SwiftEventEndEvent event) {
    Event swiftEvent = event.getSwiftEvent();
    
    if ("completed".equals(event.getEndReason())) {
        // Give money to all participants
        for (UUID participantId : swiftEvent.getParticipants()) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null) {
                EconomyAPI.addMoney(player, 100);
                player.sendMessage("§a+$100 for participating in " + swiftEvent.getName());
            }
        }
    }
}
```

### Example 2: Custom Event Type Handler

```java
public class CustomEventHandler implements SwiftEventsHook {
    
    @Override
    public String getHookName() {
        return "CustomEventHandler";
    }
    
    @Override
    public void onEventStarted(Event event) {
        if (event.getType() == Event.EventType.CUSTOM) {
            String customType = (String) event.getMetadata("custom_type");
            
            switch (customType) {
                case "zombie_survival":
                    startZombieSurvival(event);
                    break;
                case "parkour_race":
                    startParkourRace(event);
                    break;
            }
        }
    }
    
    @Override
    public void onEventUpdate(Event event) {
        if (event.getType() == Event.EventType.CUSTOM) {
            checkCustomWinConditions(event);
        }
    }
}
```

### Example 3: Permission-Based Event Access

```java
@EventHandler
public void onPlayerJoinEvent(SwiftEventPlayerJoinEvent event) {
    Player player = event.getPlayer();
    Event swiftEvent = event.getSwiftEvent();
    
    // Check event-specific permissions
    String permission = "events.join." + swiftEvent.getType().name().toLowerCase();
    
    if (!player.hasPermission(permission)) {
        event.setCancelled(true);
        player.sendMessage("§cYou don't have permission to join " + 
                           swiftEvent.getType().name().toLowerCase() + " events!");
    }
}
```

## Best Practices

### 1. Always Check API Availability
```java
if (!SwiftEventsAPI.isAvailable()) {
    // Handle SwiftEvents not being available
    return;
}
```

### 2. Use Soft Dependencies
Use `softdepend` instead of `depend` in plugin.yml if SwiftEvents is optional:
```yaml
softdepend: [SwiftEvents]
```

### 3. Handle Null Values
```java
Player player = event.getPlayer();
if (player != null) {
    // Player might be offline
    player.sendMessage("...");
}
```

### 4. Clean Up Resources
```java
@Override
public void onDisable() {
    if (SwiftEventsAPI.isAvailable()) {
        SwiftEventsAPI.unregisterHook("MyPlugin");
    }
}
```

### 5. Use Appropriate Integration Method
- **Static API**: Simple queries and operations
- **Bukkit Events**: Reacting to SwiftEvents actions
- **Hooks**: Deep integration and modification

### 6. Error Handling
```java
try {
    Event event = SwiftEventsAPI.createEvent(...);
    if (event != null) {
        // Success
    } else {
        // Handle failure (e.g., too many active events)
    }
} catch (Exception e) {
    getLogger().warning("Failed to create event: " + e.getMessage());
}
```

### 7. Performance Considerations
- Don't perform heavy operations in hook methods
- Cache frequently accessed data
- Use async operations when possible

## Troubleshooting

### Common Issues

1. **API returns null**: Check if SwiftEvents is loaded and enabled
2. **Events not firing**: Ensure you've registered the event listener
3. **Hook not called**: Check hook registration and priority
4. **Permission denied**: Verify your plugin has necessary permissions

### Debug Information

```java
// Check SwiftEvents status
getLogger().info("SwiftEvents available: " + SwiftEventsAPI.isAvailable());
getLogger().info("SwiftEvents version: " + SwiftEventsAPI.getVersion());
getLogger().info("GUI enabled: " + SwiftEventsAPI.isFeatureEnabled("gui"));
getLogger().info("Active events: " + SwiftEventsAPI.getActiveEvents().size());
```

## Support

For additional support:
1. Check the SwiftEvents documentation
2. Review the example integration class
3. Join the SwiftEvents Discord server
4. Submit issues on GitHub

---

*This integration guide covers the core concepts of SwiftEvents integration. For more advanced features and examples, refer to the JavaDocs and example implementations.* 