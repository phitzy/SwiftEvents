package com.swiftevents.api;

import com.swiftevents.gui.EventFilter;
import com.swiftevents.gui.EventSort;
import org.bukkit.entity.Player;

/**
 * API for interacting with the SwiftEvents GUI
 */
public interface GuiAPI {

    /**
     * Open the main events GUI for a player
     * @param player The player to open the GUI for
     */
    void openEventsGUI(Player player);

    /**
     * Open the events GUI for a player with a specific filter and sort order
     * @param player The player to open the GUI for
     * @param filter The filter to apply
     * @param sort The sort order to apply
     */
    void openEventsGUI(Player player, EventFilter filter, EventSort sort);

    /**
     * Open the admin GUI for a player
     * @param player The player to open the admin GUI for
     */
    void openAdminGUI(Player player);

    /**
     * Open the event creation GUI for a player
     * @param player The player to open the GUI for
     */
    void openCreateEventGUI(Player player);

    /**
     * Open the statistics GUI for a player
     * @param player The player
     */
    void openStatisticsGUI(Player player);

    /**
     * Check if the GUI system is enabled in the config
     * @return True if enabled
     */
    boolean isGuiEnabled();
} 