package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HudAPIImpl implements HudAPI {

    private final SwiftEventsPlugin plugin;

    public HudAPIImpl(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendMessage(Player player, String message) {
        plugin.getHUDManager().sendActionBarMessage(player, message);
    }

    @Override
    public boolean isHudEnabled() {
        return plugin.getConfigManager().isHUDEnabled();
    }

    @Override
    public void broadcastToEvent(Event event, String message) {
        if (!isHudEnabled()) return;
        for (UUID participantId : event.getParticipants()) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                sendMessage(player, message);
            }
        }
    }

    @Override
    public void sendTemporaryMessage(Player player, String message, int duration) {
        // The HUDManager handles displaying messages; we'll use the action bar.
        // A more complex implementation could involve custom tasks for duration.
        plugin.getHUDManager().sendActionBarMessage(player, message);
    }
} 