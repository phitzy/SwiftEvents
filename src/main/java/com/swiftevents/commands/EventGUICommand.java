package com.swiftevents.commands;

import com.swiftevents.SwiftEventsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventGUICommand implements CommandExecutor {
    
    private final SwiftEventsPlugin plugin;
    
    public EventGUICommand(SwiftEventsPlugin plugin) {
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
        
        plugin.getGUIManager().openEventsGUI(player);
        return true;
    }
} 