package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.chat.ChatManager;
import com.swiftevents.events.Event;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChatAPIImpl implements ChatAPI {

    private final SwiftEventsPlugin plugin;
    private final MiniMessage miniMessage;

    public ChatAPIImpl(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void sendMessage(CommandSender recipient, String message) {
        String prefix = plugin.getConfigManager().getPrefix();
        recipient.sendMessage(miniMessage.deserialize(prefix + message));
    }

    @Override
    public void broadcastMessage(String message) {
        String prefix = plugin.getConfigManager().getPrefix();
        Bukkit.getServer().broadcast(miniMessage.deserialize(prefix + message));
    }

    @Override
    public void broadcastToEvent(Event event, String message) {
        String prefix = plugin.getConfigManager().getPrefix();
        for (UUID participantId : event.getParticipants()) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                player.sendMessage(miniMessage.deserialize(prefix + message));
            }
        }
    }

    @Override
    public void announceEvent(Event event, ChatManager.EventAnnouncement type) {
        plugin.getChatManager().announceEvent(event, type);
    }

    @Override
    public String getRawMessage(String key) {
        return plugin.getConfigManager().getMessage(key);
    }

    @Override
    public String getFormattedMessage(String key, String... replacements) {
        String message = plugin.getConfigManager().getMessage(key);
        if (replacements.length % 2 != 0) {
            return message;
        }

        StringBuilder sb = new StringBuilder(message);
        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            int index = sb.indexOf(placeholder);
            while (index != -1) {
                sb.replace(index, index + placeholder.length(), value);
                index = sb.indexOf(placeholder, index + value.length());
            }
        }
        return sb.toString();
    }

    @Override
    public boolean isChatEnabled() {
        return plugin.getConfigManager().isChatEnabled();
    }
} 