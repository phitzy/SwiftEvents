package com.swiftevents.commands;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class SwiftEventCommand implements CommandExecutor {

    private final SwiftEventsPlugin plugin;

    public SwiftEventCommand(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getGUIManager().openEventsGUI((Player) sender);
            } else {
                showHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("admin")) {
            handleAdminCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players for non-admin commands!");
            return true;
        }

        if (!player.hasPermission("swiftevents.user")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

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
            case "gui":
                plugin.getGUIManager().openEventsGUI(player);
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

    private void handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("swiftevents.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getAdminGUIManager().openAdminGUI(player);
            } else {
                showAdminHelp(sender);
            }
            return;
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
                handleAdminList(sender);
                break;
            case "gui":
                handleAdminGUI(sender);
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
                showAdminHelp(sender);
                break;
            default:
                showAdminHelp(sender);
                break;
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cUsage: /swiftevent join <event_name_or_id>");
            return;
        }

        String eventIdentifier = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        // First try to find by ID (for [JOIN] buttons), then by name (for manual commands)
        Event event = plugin.getEventManager().getEvent(eventIdentifier);
        if (event == null) {
            event = findEventByName(eventIdentifier);
        }

        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cEvent not found: " + eventIdentifier);
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
                    "§cUsage: /swiftevent leave <event_name_or_id>");
            return;
        }

        String eventIdentifier = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        // First try to find by ID, then by name
        Event event = plugin.getEventManager().getEvent(eventIdentifier);
        if (event == null) {
            event = findEventByName(eventIdentifier);
        }

        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cEvent not found: " + eventIdentifier);
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

        player.sendMessage("§7Use §f/swiftevent info <name> §7for more details");
        player.sendMessage("§7Use §f/swiftevent gui §7to open the events menu");
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cUsage: /swiftevent info <event_name_or_id>");
            return;
        }

        String eventIdentifier = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        // First try to find by ID, then by name
        Event event = plugin.getEventManager().getEvent(eventIdentifier);
        if (event == null) {
            event = findEventByName(eventIdentifier);
        }

        if (event == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cEvent not found: " + eventIdentifier);
            return;
        }

        showEventInfo(player, event);
    }

    private void handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cUsage: /swiftevent teleport <event_id>");
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

        player.sendMessage("§7Participants: §f" + event.getCurrentParticipants() + "/" +
                (event.getMaxParticipants() > 0 ? event.getMaxParticipants() : "∞"));

        if (event.getStartTime() > 0) {
            player.sendMessage("§7Starts in: §f" + formatTime(event.getStartTime() - System.currentTimeMillis() / 1000));
        }
        if (event.getEndTime() > 0) {
            player.sendMessage("§7Ends in: §f" + formatTime(event.getEndTime() - System.currentTimeMillis() / 1000));
        }

        if (event.getLocation() != null) {
            player.sendMessage("§7Location: §f" + event.getLocation().getWorldName() + " " +
                    String.format("%.1f, %.1f, %.1f", event.getLocation().getX(), event.getLocation().getY(), event.getLocation().getZ()));
        }

        if (event.isParticipant(player.getUniqueId())) {
            player.sendMessage("§a✓ You are participating");
        }
        player.sendMessage("§7" + "─".repeat(40));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6SwiftEvents Commands:");
        sender.sendMessage("§7/swiftevent join <event> - Join an event.");
        sender.sendMessage("§7/swiftevent leave <event> - Leave an event.");
        sender.sendMessage("§7/swiftevent list - List available events.");
        sender.sendMessage("§7/swiftevent info <event> - Get info about an event.");
        sender.sendMessage("§7/swiftevent gui - Open the events GUI.");
        if (sender.hasPermission("swiftevents.admin")) {
            sender.sendMessage("§7/swiftevent admin - Admin commands.");
        }
    }

    // Admin command handlers from EventAdminCommand
    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cUsage: /swiftevent admin create <name> <type> <description>");
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
                    "§cUsage: /swiftevent admin delete <event_name>");
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
                    "§cUsage: /swiftevent admin start <event_name>");
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
                    "§cUsage: /swiftevent admin stop <event_name>");
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

    private void handleAdminList(CommandSender sender) {
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

    private void handleAdminGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        plugin.getAdminGUIManager().openAdminGUI(player);
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadConfig();
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aConfiguration reloaded.");
    }

    private void handleTasker(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showTaskerHelp(sender);
            return;
        }
        String taskerAction = args[1].toLowerCase();
        switch (taskerAction) {
            case "list":
                // Logic to list tasks
                sender.sendMessage("Tasker list functionality coming soon.");
                break;
            case "add":
                // Logic to add a task
                sender.sendMessage("Tasker add functionality coming soon.");
                break;
            case "remove":
                // Logic to remove a task
                sender.sendMessage("Tasker remove functionality coming soon.");
                break;
            default:
                showTaskerHelp(sender);
                break;
        }
    }

    private void showTaskerHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Event Tasker Commands:");
        sender.sendMessage("§7/swiftevent admin tasker list - List scheduled event tasks.");
        sender.sendMessage("§7/swiftevent admin tasker add <preset> <cron> - Schedule an event from a preset.");
        sender.sendMessage("§7/swiftevent admin tasker remove <id> - Remove a scheduled task.");
    }

    private void showAdminHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cSwiftEvents Admin Help:");
        sender.sendMessage("§7/swiftevent admin create <name> <type> <desc> - Create a new event.");
        sender.sendMessage("§7/swiftevent admin delete <name> - Delete an event.");
        sender.sendMessage("§7/swiftevent admin start <name> - Start an event.");
        sender.sendMessage("§7/swiftevent admin stop <name> - Stop an event.");
        sender.sendMessage("§7/swiftevent admin list - List all events.");
        sender.sendMessage("§7/swiftevent admin gui - Open the admin GUI.");
        sender.sendMessage("§7/swiftevent admin reload - Reload the config.");
        sender.sendMessage("§7/swiftevent admin tasker - Manage scheduled tasks.");
        sender.sendMessage("§7/swiftevent admin config <key> [value] - View or edit config.");
        sender.sendMessage("§7/swiftevent admin backup - Manage backups.");
    }

    private void handleConfig(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /swiftevent admin config <key> [new_value]");
            return;
        }

        String key = args[1];
        if (args.length == 2) {
            // Get value
            Object value = plugin.getConfig().get(key);
            if (value != null) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§7" + key + " = §f" + value);
            } else {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cKey not found: " + key);
            }
        } else {
            // Set value
            String valueStr = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            Object value = parseConfigValue(valueStr);
            plugin.getConfig().set(key, value);
            plugin.saveConfig();
            plugin.getConfigManager().reloadConfig(); // To apply changes
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aSet " + key + " to " + value);
        }
    }

    private void handleBackup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /swiftevent admin backup <create|restore|list> [name]");
            return;
        }

        String backupAction = args[1].toLowerCase();
        switch (backupAction) {
            case "create":
                // Logic to create a backup
                sender.sendMessage("Backup creation functionality coming soon.");
                break;
            case "restore":
                // Logic to restore a backup
                sender.sendMessage("Backup restoration functionality coming soon.");
                break;
            case "list":
                // Logic to list backups
                sender.sendMessage("Backup listing functionality coming soon.");
                break;
            default:
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cInvalid backup command.");
                break;
        }
    }

    private Object parseConfigValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Not an integer
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Not a double
        }
        return value; // It's a string
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