package com.swiftevents.api;

import com.swiftevents.chat.ChatManager;
import com.swiftevents.events.Event;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * API for interacting with the SwiftEvents chat and messaging system
 */
public interface ChatAPI {

    /**
     * Send a formatted message with the plugin's prefix to a player or console.
     *
     * @param recipient The recipient of the message.
     * @param message   The message to send (supports MiniMessage format).
     */
    void sendMessage(CommandSender recipient, String message);

    /**
     * Broadcast a formatted message with the plugin's prefix to all online players.
     *
     * @param message The message to broadcast (supports MiniMessage format).
     */
    void broadcastMessage(String message);

    /**
     * Broadcast a formatted message to all participants of an event.
     *
     * @param event   The event.
     * @param message The message to broadcast.
     */
    void broadcastToEvent(Event event, String message);

    /**
     * Announce an event using the configured fancy format.
     *
     * @param event The event to announce.
     * @param type  The type of announcement.
     */
    void announceEvent(Event event, ChatManager.EventAnnouncement type);

    /**
     * Get a raw message string from the messages file.
     *
     * @param key The key of the message.
     * @return The raw message string, or the key if not found.
     */
    String getRawMessage(String key);

    /**
     * Get a formatted message from the messages file, with prefix.
     *
     * @param key          The key of the message.
     * @param replacements The placeholders to replace.
     * @return The formatted message.
     */
    String getFormattedMessage(String key, String... replacements);

    /**
     * Check if chat features are enabled.
     *
     * @return true if enabled.
     */
    boolean isChatEnabled();
} 