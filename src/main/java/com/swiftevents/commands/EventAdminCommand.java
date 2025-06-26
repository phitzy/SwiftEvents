package com.swiftevents.commands;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class EventAdminCommand implements CommandExecutor {
    
    private final SwiftEventsPlugin plugin;
    
    public EventAdminCommand(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("swiftevents.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }
        
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getGUIManager().openAdminGUI(player);
            } else {
                showHelp(sender);
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreate(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "start":
                handleStart(sender, args);
                break;
            case "stop":
                handleStop(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "gui":
                handleGUI(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "tasker":
                handleTasker(sender, args);
                break;
            case "config":
                handleConfig(sender, args);
                break;
            case "backup":
                handleBackup(sender, args);
                break;
            case "help":
                showHelp(sender);
                break;
            default:
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /eventadmin create <name> <type> <description>");
            sender.sendMessage("§7Available types: PVP, PVE, BUILDING, RACING, TREASURE_HUNT, MINI_GAME, CUSTOM");
            return;
        }
        
        String name = args[1];
        String typeStr = args[2].toUpperCase();
        String description = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
        
        Event.EventType type;
        try {
            type = Event.EventType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cInvalid event type: " + typeStr);
            sender.sendMessage("§7Available types: PVP, PVE, BUILDING, RACING, TREASURE_HUNT, MINI_GAME, CUSTOM");
            return;
        }
        
        Event event = plugin.getEventManager().createEvent(
                name, 
                description, 
                type, 
                sender instanceof Player ? ((Player) sender).getUniqueId() : null
        );
        
        if (event != null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully created event: " + name);
            sender.sendMessage("§7Event ID: " + event.getId());
            
            if (sender instanceof Player player) {
                event.setLocation(player.getWorld().getName(), 
                        player.getLocation().getX(), 
                        player.getLocation().getY(), 
                        player.getLocation().getZ());
                plugin.getEventManager().saveEvent(event);
                sender.sendMessage("§7Location set to your current position");
            }
        } else {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to create event. Maximum concurrent events reached?");
        }
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /eventadmin delete <event_name>");
            return;
        }
        
        String eventName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Event event = findEventByName(eventName);
        
        if (event == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent not found: " + eventName);
            return;
        }
        
        boolean success = plugin.getEventManager().deleteEvent(event.getId());
        if (success) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully deleted event: " + event.getName());
        } else {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to delete event. Please try again.");
        }
    }
    
    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /eventadmin start <event_name>");
            return;
        }
        
        String eventName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Event event = findEventByName(eventName);
        
        if (event == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent not found: " + eventName);
            return;
        }
        
        boolean success = plugin.getEventManager().startEvent(event.getId());
        if (success) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully started event: " + event.getName());
        } else {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to start event. Check if event can be started.");
        }
    }
    
    private void handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /eventadmin stop <event_name>");
            return;
        }
        
        String eventName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Event event = findEventByName(eventName);
        
        if (event == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent not found: " + eventName);
            return;
        }
        
        boolean success = plugin.getEventManager().endEvent(event.getId());
        if (success) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully stopped event: " + event.getName());
        } else {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to stop event. Check if event is active.");
        }
    }
    
    private void handleList(CommandSender sender) {
        List<Event> events = plugin.getEventManager().getAllEvents();
        
        if (events.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§7No events found.");
            return;
        }
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6All Events:");
        sender.sendMessage("§7" + "─".repeat(50));
        
        for (Event event : events) {
            String statusColor = getStatusColor(event.getStatus());
            String participantInfo = event.getCurrentParticipants() + "/" + 
                    (event.getMaxParticipants() > 0 ? event.getMaxParticipants() : "∞");
            
            sender.sendMessage("§f" + event.getName() + " " + statusColor + "[" + 
                    event.getStatus().name() + "]");
            sender.sendMessage("  §7ID: §f" + event.getId());
            sender.sendMessage("  §7Type: §f" + event.getType().name() + 
                    " §7| Participants: §f" + participantInfo);
            
            if (event.getCreatedBy() != null) {
                sender.sendMessage("  §7Created by: §f" + event.getCreatedBy());
            }
            
            sender.sendMessage("");
        }
        
        sender.sendMessage("§7Total: " + events.size() + " events");
        sender.sendMessage("§7Active: " + plugin.getEventManager().getActiveEvents().size() + " events");
    }
    
    private void handleGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }
        
        plugin.getGUIManager().openAdminGUI(player);
    }
    
    private void handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aConfiguration reloaded successfully!");
                    
            // Restart event tasker if needed
            if (plugin.getEventTasker() != null) {
                plugin.getEventTasker().restart();
            }
        } catch (Exception e) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to reload configuration: " + e.getMessage());
        }
    }
    
    private void handleTasker(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /eventadmin tasker <start|stop|status|force|presets>");
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "start":
                if (!plugin.getConfigManager().isEventTaskerEnabled()) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§cEvent Tasker is disabled in config! Enable it first.");
                    return;
                }
                plugin.getEventTasker().start();
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§aEvent Tasker has been started!");
                break;
                
            case "stop":
                plugin.getEventTasker().stop();
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cEvent Tasker has been stopped!");
                break;
                
            case "status":
                boolean running = plugin.getEventTasker().isRunning();
                boolean enabled = plugin.getConfigManager().isEventTaskerEnabled();
                long timeUntilNext = plugin.getEventTasker().getTimeUntilNextEvent();
                
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§eEvent Tasker Status:");
                sender.sendMessage("§7Enabled in config: " + (enabled ? "§aYes" : "§cNo"));
                sender.sendMessage("§7Currently running: " + (running ? "§aYes" : "§cNo"));
                if (running && timeUntilNext > 0) {
                    long minutes = timeUntilNext / (60 * 1000);
                    sender.sendMessage("§7Next event in: §e" + minutes + " minutes");
                }
                sender.sendMessage("§7Loaded presets: §e" + plugin.getEventTasker().getPresets().size());
                break;
                
            case "force":
                if (!plugin.getEventTasker().isRunning()) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§cEvent Tasker is not running!");
                    return;
                }
                plugin.getEventTasker().forceNextEvent();
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§aForced next automatic event to start immediately!");
                break;
                
            case "presets":
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§eLoaded Event Presets:");
                plugin.getEventTasker().getPresets().forEach((id, preset) -> {
                    String status = preset.isEnabled() ? "§aEnabled" : "§cDisabled";
                    sender.sendMessage("§7- " + id + " §8(" + preset.getType() + ") " + status + 
                            " §7Weight: " + preset.getWeight());
                });
                break;
                
            default:
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cUsage: /eventadmin tasker <start|stop|status|force|presets>");
                break;
        }
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§7" + "─".repeat(50));
        sender.sendMessage("§6SwiftEvents Admin Commands:");
        sender.sendMessage("§7" + "─".repeat(50));
        sender.sendMessage("§f/eventadmin create <name> <type> <description> §7- Create new event");
        sender.sendMessage("§f/eventadmin delete <name> §7- Delete an event");
        sender.sendMessage("§f/eventadmin start <name> §7- Start an event");
        sender.sendMessage("§f/eventadmin stop <name> §7- Stop an event");
        sender.sendMessage("§f/eventadmin list §7- List all events");
        sender.sendMessage("§f/eventadmin gui §7- Open admin GUI");
        sender.sendMessage("§f/eventadmin reload §7- Reload configuration");
        sender.sendMessage("§f/eventadmin tasker <start|stop|status|force|presets> §7- Manage auto tasker");
        sender.sendMessage("§f/eventadmin config <get|set|validate|list> §7- Manage configuration");
        sender.sendMessage("§f/eventadmin backup <create|info> §7- Manage backups");
        sender.sendMessage("§7" + "─".repeat(50));
        sender.sendMessage("§7Event Types: §fPVP, PVE, BUILDING, RACING, TREASURE_HUNT, MINI_GAME, CUSTOM");
    }
    
    private Event findEventByName(String name) {
        List<Event> events = plugin.getEventManager().getAllEvents();
        
        // First try exact match
        for (Event event : events) {
            if (event.getName().equalsIgnoreCase(name)) {
                return event;
            }
        }
        
        // Then try partial match
        for (Event event : events) {
            if (event.getName().toLowerCase().contains(name.toLowerCase())) {
                return event;
            }
        }
        
        return null;
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
    
    private void handleConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("swiftevents.admin.config")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /eventadmin config <get|set|validate|list> [key] [value]");
            return;
        }
        
        String configSub = args[1].toLowerCase();
        
        switch (configSub) {
            case "get":
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§cUsage: /eventadmin config get <key>");
                    return;
                }
                
                String getKey = args[2];
                Object value = plugin.getConfigManager().getConfigValue(getKey);
                if (value != null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§7" + getKey + ": §f" + value);
                } else {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§cConfiguration key not found: " + getKey);
                }
                break;
                
            case "set":
                if (args.length < 4) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§cUsage: /eventadmin config set <key> <value>");
                    return;
                }
                
                String setKey = args[2];
                String setValue = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                
                // Try to parse the value appropriately
                Object parsedValue = parseConfigValue(setValue);
                
                if (plugin.getConfigManager().setConfigValue(setKey, parsedValue)) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§aConfiguration updated: " + setKey + " = " + parsedValue);
                } else {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§cFailed to update configuration key: " + setKey);
                }
                break;
                
            case "validate":
                plugin.getConfigManager().validateConfig();
                List<String> errors = plugin.getConfigManager().getValidationErrors();
                
                if (errors.isEmpty()) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§aConfiguration validation passed!");
                } else {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                            "§cConfiguration validation failed with " + errors.size() + " errors:");
                    for (String error : errors) {
                        sender.sendMessage("§c  - " + error);
                    }
                }
                break;
                
            case "list":
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Key Configuration Settings:");
                sender.sendMessage("§7database.enabled: §f" + plugin.getConfigManager().isDatabaseEnabled());
                sender.sendMessage("§7events.max_concurrent: §f" + plugin.getConfigManager().getMaxConcurrentEvents());
                sender.sendMessage("§7events.player_cooldown: §f" + plugin.getConfigManager().getPlayerCooldown());
                sender.sendMessage("§7events.max_events_per_player: §f" + plugin.getConfigManager().getMaxEventsPerPlayer());
                sender.sendMessage("§7event_tasker.enabled: §f" + plugin.getConfigManager().isEventTaskerEnabled());
                sender.sendMessage("§7hud.enabled: §f" + plugin.getConfigManager().isHUDEnabled());
                sender.sendMessage("§7chat.enabled: §f" + plugin.getConfigManager().isChatEnabled());
                sender.sendMessage("§7advanced.debug_mode: §f" + plugin.getConfigManager().isDebugMode());
                break;
                
            default:
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cUsage: /eventadmin config <get|set|validate|list> [key] [value]");
                break;
        }
    }
    
    private void handleBackup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("swiftevents.admin.backup")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /eventadmin backup <create|info>");
            return;
        }
        
        String backupSub = args[1].toLowerCase();
        
        switch (backupSub) {
            case "create":
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§aCreating event data backup...");
                
                plugin.getEventManager().saveAllEvents();
                
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("backup_created"));
                break;
                
            case "info":
                int totalEvents = plugin.getEventManager().getAllEvents().size();
                int activeEvents = plugin.getEventManager().getActiveEvents().size();
                boolean jsonBackupsEnabled = plugin.getConfigManager().isJsonAutoBackupEnabled();
                
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Backup Information:");
                sender.sendMessage("§7Total events: §f" + totalEvents);
                sender.sendMessage("§7Active events: §f" + activeEvents);
                sender.sendMessage("§7Auto-backup enabled: §f" + jsonBackupsEnabled);
                
                if (jsonBackupsEnabled) {
                    int backupInterval = plugin.getConfigManager().getJsonBackupInterval();
                    int maxBackups = plugin.getConfigManager().getMaxJsonBackups();
                    sender.sendMessage("§7Backup interval: §f" + backupInterval + " seconds");
                    sender.sendMessage("§7Max backups kept: §f" + maxBackups);
                }
                break;
                
            default:
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cUsage: /eventadmin backup <create|info>");
                break;
        }
    }
    
    private Object parseConfigValue(String value) {
        // Try to parse as boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        
        // Try to parse as integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        
        // Try to parse as double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}
        
        // Return as string
        return value;
    }
} 