package com.swiftevents.commands;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.swiftevents.hud.HUDManager;
import com.swiftevents.permissions.Permissions;
import com.swiftevents.tasker.EventTasker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SwiftEventCommand implements CommandExecutor, TabCompleter {

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

        if (!player.hasPermission(Permissions.USER_BASE)) {
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
                if (!player.hasPermission(Permissions.USER_TP)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("no_permission"));
                    return true;
                }
                handleTeleport(player, args);
                break;
            case "hud":
                if (!player.hasPermission("swiftevents.hud.toggle")) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("no_permission"));
                    return true;
                }
                handleHud(player, args);
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
        if (!sender.hasPermission(Permissions.ADMIN_BASE)) {
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
            case "location":
                handleLocationCommand(sender, Arrays.copyOfRange(args, 1, args.length));
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
        if (!player.hasPermission(Permissions.USER_JOIN)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!player.hasPermission(Permissions.USER_LEAVE)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!player.hasPermission(Permissions.USER_LIST)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!player.hasPermission(Permissions.USER_STATS)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!player.hasPermission(Permissions.USER_TP)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cUsage: /swiftevent teleport <event_name_or_id>");
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
                    "§cFailed to teleport to event. You can only teleport to running events.");
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
        if (sender instanceof Player && !sender.hasPermission(Permissions.USER_HELP)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        sender.sendMessage("§6§lSwiftEvents Help");
        sender.sendMessage("§7" + "─".repeat(40));
        sender.sendMessage("§e/swiftevent help §7- Shows this help message.");
        sender.sendMessage("§e/swiftevent list §7- Lists all available events.");
        sender.sendMessage("§e/swiftevent info <event> §7- Shows info about an event.");
        sender.sendMessage("§e/swiftevent teleport <event> §7- Teleports you to an event.");
        sender.sendMessage("§7" + "─".repeat(40));
    }

    // Admin command handlers from EventAdminCommand
    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_CREATE)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!sender.hasPermission(Permissions.ADMIN_DELETE)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!sender.hasPermission(Permissions.ADMIN_START)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!sender.hasPermission(Permissions.ADMIN_STOP)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!sender.hasPermission(Permissions.ADMIN_BASE)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
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
        if (!sender.hasPermission(Permissions.ADMIN_BASE)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        if (sender instanceof Player player) {
            plugin.getAdminGUIManager().openAdminGUI(player);
        } else {
            showAdminHelp(sender);
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(Permissions.RELOAD)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        plugin.getConfigManager().reloadConfig();
        plugin.getEventTasker().restart();
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aConfiguration reloaded and event tasker restarted.");
    }

    private void handleTasker(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showTaskerHelp(sender);
            return;
        }

        String taskerCmd = args[1].toLowerCase();
        EventTasker tasker = plugin.getEventTasker();

        switch (taskerCmd) {
            case "start":
                if (!sender.hasPermission(Permissions.TASKER_START)) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("no_permission"));
                    return;
                }
                if (tasker.isRunning()) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent tasker is already running.");
                } else {
                    tasker.start();
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aEvent tasker started.");
                }
                break;
            case "stop":
                if (!sender.hasPermission(Permissions.TASKER_STOP)) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("no_permission"));
                    return;
                }
                if (!tasker.isRunning()) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent tasker is not running.");
                } else {
                    tasker.stop();
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aEvent tasker stopped.");
                }
                break;
            case "restart":
                if (!sender.hasPermission(Permissions.TASKER_RESTART)) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("no_permission"));
                    return;
                }
                tasker.restart();
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aEvent tasker restarted.");
                break;
            case "next":
                if (!sender.hasPermission(Permissions.TASKER_NEXT)) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("no_permission"));
                    return;
                }
                if (!tasker.isRunning()) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cEvent tasker is not running. Start it first.");
                } else {
                    tasker.forceNextEvent();
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aForcing next automatic event to start...");
                }
                break;
            case "status":
                if (!sender.hasPermission(Permissions.TASKER_STATUS)) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("no_permission"));
                    return;
                }
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Event Tasker Status:");
                sender.sendMessage("  §7Running: " + (tasker.isRunning() ? "§aYes" : "§cNo"));
                if (tasker.isRunning()) {
                    long timeUntilNext = tasker.getTimeUntilNextEvent();
                    sender.sendMessage("  §7Next event in: §f" + formatTime(timeUntilNext / 1000));
                }
                sender.sendMessage("  §7Loaded Presets: §f" + tasker.getPresets().size());
                break;
            default:
                showTaskerHelp(sender);
                break;
        }
    }

    private void showTaskerHelp(CommandSender sender) {
        sender.sendMessage("§6§lEvent Tasker Help");
        sender.sendMessage("§7" + "─".repeat(40));
        sender.sendMessage("§e/swiftevent admin tasker start §7- Starts the tasker.");
        sender.sendMessage("§e/swiftevent admin tasker stop §7- Stops the tasker.");
        sender.sendMessage("§e/swiftevent admin tasker restart §7- Restarts the tasker.");
        sender.sendMessage("§e/swiftevent admin tasker next §7- Forces the next scheduled event.");
        sender.sendMessage("§e/swiftevent admin tasker status §7- Shows the tasker status.");
        sender.sendMessage("§7" + "─".repeat(40));
    }

    private void showAdminHelp(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN_BASE)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        sender.sendMessage("§c§lSwiftEvents Admin Help");
        sender.sendMessage("§7" + "─".repeat(40));
        sender.sendMessage("§e/swiftevent admin create <name> <type> <duration> [desc] §7- Creates an event.");
        sender.sendMessage("§e/swiftevent admin delete <name> - Delete an event.");
        sender.sendMessage("§e/swiftevent admin start <name> - Start an event.");
        sender.sendMessage("§e/swiftevent admin stop <name> - Stop an event.");
        sender.sendMessage("§e/swiftevent admin list - List all events.");
        sender.sendMessage("§e/swiftevent admin gui - Open the admin GUI.");
        sender.sendMessage("§e/swiftevent admin reload - Reload the plugin configuration.");
        sender.sendMessage("§e/swiftevents admin tasker <start|stop|status> - Control the event tasker.");
        sender.sendMessage("§e/swiftevents admin config <get|set> <key> [value] - Manage plugin config.");
        sender.sendMessage("§e/swiftevents admin backup <create|list> - Manage backups.");
        sender.sendMessage("§e/swiftevents admin location <set|remove|list|tp> [name] - Manage preset locations.");
        sender.sendMessage("§7" + "─".repeat(40));
    }

    private void handleConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_BASE)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /swiftevent admin config <key> [new_value]");
            return;
        }

        String key = args[1];
        if (args.length == 3) {
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
        if (!sender.hasPermission(Permissions.ADMIN_BASE)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /swiftevent admin backup <create|restore|list> [name]");
            return;
        }

        String backupAction = args[1].toLowerCase();

        if (backupAction.equals("create")) {
            plugin.getDatabaseManager().backupData().thenAccept(success -> {
                if (success) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aManual backup created."));
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cFailed to create backup. Check console for details."));
                }
            });
        } else if (backupAction.equals("restore")) {
            if (args.length < 3) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /swiftevent admin backup restore <backup_name>");
                return;
            }
            // Logic to restore a backup
            sender.sendMessage("Backup restoration functionality not yet implemented.");
        } else if (backupAction.equals("list")) {
            sender.sendMessage("Backup listing functionality coming soon.");
        } else {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cInvalid backup command.");
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

    private void handleLocationCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showLocationHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "set":
                handleLocationSet(sender, args);
                break;
            case "remove":
                handleLocationRemove(sender, args);
                break;
            case "list":
                handleLocationList(sender);
                break;
            case "teleport":
                handleLocationTeleport(sender, args);
                break;
            default:
                showLocationHelp(sender);
                break;
        }
    }

    private void handleLocationSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_SETPOS)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /swiftevent admin location set <name>");
            return;
        }
        String name = args[1];
        Location location = player.getLocation();
        if (plugin.getLocationManager().addPresetLocation(name, location)) {
            sender.sendMessage("§aPreset location '" + name + "' set to your current position.");
        } else {
            sender.sendMessage("§cLocation with name '" + name + "' already exists.");
        }
    }

    private void handleLocationRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_SETPOS)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /swiftevent admin location remove <name>");
            return;
        }
        String name = args[1];
        if (plugin.getLocationManager().removePresetLocation(name)) {
            sender.sendMessage("§aPreset location '" + name + "' removed.");
        } else {
            sender.sendMessage("§cLocation with name '" + name + "' not found.");
        }
    }

    private void handleLocationList(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN_SETPOS)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        Set<String> locations = plugin.getLocationManager().getPresetLocationNames();
        if (locations.isEmpty()) {
            sender.sendMessage("§cNo preset locations found.");
            return;
        }
        sender.sendMessage("§e==== Preset Locations ====");
        locations.forEach(name -> sender.sendMessage("§a- " + name));
        sender.sendMessage("§e==========================");
    }
    
    private void handleLocationTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_SETPOS)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /swiftevent admin location tp <name>");
            return;
        }
        String name = args[1];
        plugin.getLocationManager().getPresetLocation(name).ifPresentOrElse(
            presetLocation -> {
                player.teleport(presetLocation.location());
                player.sendMessage("§aTeleported to location '" + name + "'.");
            },
            () -> player.sendMessage("§cLocation with name '" + name + "' not found.")
        );
    }

    private void showLocationHelp(CommandSender sender) {
        sender.sendMessage("§6§lLocation Management Help");
        sender.sendMessage("§7" + "─".repeat(40));
        sender.sendMessage("§e/swiftevent admin location set <name> §7- Sets a preset location.");
        sender.sendMessage("§e/swiftevent admin location remove <name> §7- Removes a preset location.");
        sender.sendMessage("§e/swiftevent admin location list §7- Lists all preset locations.");
        sender.sendMessage("§e/swiftevent admin location teleport <name> §7- Teleports to a preset location.");
        sender.sendMessage("§7" + "─".repeat(40));
    }

    private void handleHud(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§cUsage: /swiftevent hud <sidebar|bossbar|none>");
            return;
        }

        String arg = args[1].toLowerCase();
        switch (arg) {
            case "sidebar":
                plugin.getHUDManager().toggleSidebar(player);
                break;
            case "bossbar":
                plugin.getHUDManager().toggleBossBar(player);
                // Additional feedback for bossbar
                List<Event> activeEvents = plugin.getEventManager().getActiveEvents();
                if (activeEvents.isEmpty()) {
                    player.sendMessage("§7Note: Bossbar will show event information when events are active.");
                }
                break;
            case "none":
                plugin.getHUDManager().setPlayerHUDPreference(player, HUDManager.HUDPreference.NONE);
                player.sendMessage("§aHUD disabled.");
                break;
            default:
                player.sendMessage("§cInvalid argument. Usage: /swiftevent hud <sidebar|bossbar|none>");
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("join", "leave", "list", "info", "teleport", "gui", "help"));
            if (sender.hasPermission(Permissions.ADMIN_BASE)) {
                completions.add("admin");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission(Permissions.ADMIN_BASE)) {
                return Collections.emptyList();
            }
            if (args.length == 2) {
                List<String> adminCompletions = new ArrayList<>(Arrays.asList(
                        "create", "delete", "start", "stop", "list", "gui", "reload", "tasker", "config", "backup", "location", "help"
                ));
                return adminCompletions.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            // Tab completion for admin sub-commands
            String adminSubCommand = args[1].toLowerCase();
            if (adminSubCommand.equals("tasker") && args.length == 3) {
                return Arrays.asList("start", "stop", "restart", "next", "status").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            // Add more tab completions for other admin commands if needed (e.g., event names for delete/start/stop)
        }

        return Collections.emptyList();
    }
}