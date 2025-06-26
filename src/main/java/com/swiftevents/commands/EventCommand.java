package com.swiftevents.commands;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class EventCommand implements CommandExecutor {
    
    private final SwiftEventsPlugin plugin;
    
    public EventCommand(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        if (!player.hasPermission("swiftevents.user")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "join":
                handleJoin(player, args);
                break;
            case "leave":
                handleLeave(player, args);
                break;
            case "list":
                handleList(player);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "teleport":
                handleTeleport(player, args);
                break;
            case "help":
                showHelp(player);
                break;
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /event join <event_name>");
            return;
        }
        
        String eventName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Event event = findEventByName(eventName);
        
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent not found: " + eventName);
            return;
        }
        
        if (event.isParticipant(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cYou are already participating in this event!");
            return;
        }
        
        if (!event.canJoin()) {
            String reason = "Event is not accepting participants";
            if (event.isFull()) {
                reason = "Event is full";
            } else if (event.isCompleted()) {
                reason = "Event has ended";
            } else if (event.isCancelled()) {
                reason = "Event was cancelled";
            }
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + reason);
            return;
        }
        
        boolean success = plugin.getEventManager().joinEvent(event.getId(), player.getUniqueId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully joined event: " + event.getName());
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to join event. Please try again.");
        }
    }
    
    private void handleLeave(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /event leave <event_name>");
            return;
        }
        
        String eventName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Event event = findEventByName(eventName);
        
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent not found: " + eventName);
            return;
        }
        
        if (!event.isParticipant(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cYou are not participating in this event!");
            return;
        }
        
        boolean success = plugin.getEventManager().leaveEvent(event.getId(), player.getUniqueId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§aSuccessfully left event: " + event.getName());
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to leave event. Please try again.");
        }
    }
    
    private void handleList(Player player) {
        List<Event> events = plugin.getEventManager().getAllEvents();
        
        if (events.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§7No events are currently available.");
            return;
        }
        
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§6Available Events:");
        player.sendMessage("§7" + "─".repeat(40));
        
        for (Event event : events) {
            String statusColor = getStatusColor(event.getStatus());
            String participantInfo = event.getCurrentParticipants() + "/" + 
                    (event.getMaxParticipants() > 0 ? event.getMaxParticipants() : "∞");
            
            player.sendMessage("§f" + event.getName() + " " + statusColor + "[" + 
                    event.getStatus().name() + "]");
            player.sendMessage("  §7Type: §f" + event.getType().name() + 
                    " §7| Participants: §f" + participantInfo);
            
            if (event.isParticipant(player.getUniqueId())) {
                player.sendMessage("  §a✓ You are participating");
            }
            
            player.sendMessage("");
        }
        
        player.sendMessage("§7Use §f/event info <name> §7for more details");
        player.sendMessage("§7Use §f/eventgui §7to open the events menu");
    }
    
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /event info <event_name>");
            return;
        }
        
        String eventName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Event event = findEventByName(eventName);
        
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent not found: " + eventName);
            return;
        }
        
        showEventInfo(player, event);
    }
    
    private void handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cUsage: /event teleport <event_id>");
            return;
        }
        
        String eventId = args[1];
        Event event = plugin.getEventManager().getEvent(eventId);
        
        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cEvent not found: " + eventId);
            return;
        }
        
        boolean success = plugin.getChatManager().teleportToEvent(player, event);
        if (!success) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cFailed to teleport to event.");
        }
    }
    
    private void showEventInfo(Player player, Event event) {
        String statusColor = getStatusColor(event.getStatus());
        
        player.sendMessage("§7" + "─".repeat(40));
        player.sendMessage("§6Event Information: §f" + event.getName());
        player.sendMessage("§7" + "─".repeat(40));
        
        player.sendMessage("§7Type: §f" + event.getType().name());
        player.sendMessage("§7Status: " + statusColor + event.getStatus().name());
        
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            player.sendMessage("§7Description: §f" + event.getDescription());
        }
        
        String participantInfo = event.getCurrentParticipants() + "/" + 
                (event.getMaxParticipants() > 0 ? event.getMaxParticipants() : "∞");
        player.sendMessage("§7Participants: §f" + participantInfo);
        
        if (event.getStartTime() != null) {
            if (event.hasStarted()) {
                if (event.getEndTime() != null) {
                    player.sendMessage("§7Time remaining: §e" + event.getFormattedRemainingTime());
                } else {
                    player.sendMessage("§7Status: §aIn progress");
                }
            } else {
                long timeUntilStart = event.getStartTime() - System.currentTimeMillis();
                player.sendMessage("§7Starts in: §e" + formatTime(timeUntilStart / 1000));
            }
        }
        
        if (event.getWorld() != null) {
            player.sendMessage("§7Location: §f" + event.getWorld() + " (" + 
                    (int)event.getX() + ", " + (int)event.getY() + ", " + (int)event.getZ() + ")");
        }
        
        if (event.isParticipant(player.getUniqueId())) {
            player.sendMessage("§a✓ You are participating in this event");
        } else if (event.canJoin()) {
            player.sendMessage("§eUse §f/event join " + event.getName() + " §eto participate!");
        } else {
            player.sendMessage("§cThis event is not accepting participants");
        }
        
        if (!event.getRewards().isEmpty()) {
            player.sendMessage("§6Rewards are available for participants!");
        }
        
        player.sendMessage("§7" + "─".repeat(40));
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§7" + "─".repeat(40));
        player.sendMessage("§6SwiftEvents Commands:");
        player.sendMessage("§7" + "─".repeat(40));
        player.sendMessage("§f/event list §7- View all available events");
        player.sendMessage("§f/event info <name> §7- Get detailed event information");
        player.sendMessage("§f/event join <name> §7- Join an event");
        player.sendMessage("§f/event leave <name> §7- Leave an event");
        player.sendMessage("§f/event teleport <id> §7- Teleport to an event location");
        player.sendMessage("§f/eventgui §7- Open the events GUI");
        player.sendMessage("§7" + "─".repeat(40));
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
    
    private String formatTime(long seconds) {
        if (seconds <= 0) {
            return "Starting now!";
        }
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return secs + "s";
        }
    }
} 