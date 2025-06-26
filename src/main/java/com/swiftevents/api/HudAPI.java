package com.swiftevents.api;

import com.swiftevents.events.Event;
import org.bukkit.entity.Player;

/**
 * API for interacting with the SwiftEvents HUD
 */
public interface HudAPI {

    /**
     * Send a HUD message to a player.
     * Respects player's HUD preferences.
     *
     * @param player  The player to send the message to.
     * @param message The message to send.
     */
    void sendMessage(Player player, String message);

    /**
     * Broadcast a HUD message to all participants of an event.
     *
     * @param event   The event.
     * @param message The message to broadcast.
     */
    void broadcastToEvent(Event event, String message);

    /**
     * Send a temporary HUD message (title, boss bar, or action bar).
     *
     * @param player   The player.
     * @param message  The message.
     * @param duration The duration in seconds.
     */
    void sendTemporaryMessage(Player player, String message, int duration);

    /**
     * Check if the HUD system is enabled.
     *
     * @return true if the HUD is enabled, false otherwise.
     */
    boolean isHudEnabled();
} 