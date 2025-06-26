package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HudAPIManager implements HudAPI {

    private final SwiftEventsPlugin plugin;

    public HudAPIManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendMessage(Player player, String message) {
        plugin.getHUDManager().sendHUDMessage(player, message);
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
        // The default sendHUDMessage has a built-in temporary duration for boss bars.
        // For action bars, we might need a manual clear if we want it to disappear before another message comes.
        // For now, we will just use the default behavior.
        plugin.getHUDManager().sendHUDMessage(player, message);
    }

    @Override
    public boolean isHudEnabled() {
        return plugin.getConfigManager().isHUDEnabled();
    }
} 