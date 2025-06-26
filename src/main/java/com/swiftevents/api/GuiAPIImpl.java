package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.gui.EventFilter;
import com.swiftevents.gui.EventSort;
import org.bukkit.entity.Player;

public class GuiAPIImpl implements GuiAPI {

    private final SwiftEventsPlugin plugin;

    public GuiAPIImpl(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void openEventsGUI(Player player) {
        plugin.getGUIManager().openEventsGUI(player, 0, EventFilter.ALL, EventSort.STATUS);
    }

    @Override
    public void openEventsGUI(Player player, EventFilter filter, EventSort sort) {
        plugin.getGUIManager().openEventsGUI(player, 0, filter, sort);
    }

    @Override
    public void openAdminGUI(Player player) {
        plugin.getAdminGUIManager().openAdminGUI(player);
    }

    @Override
    public void openCreateEventGUI(Player player) {
        plugin.getAdminGUIManager().openEventCreationWizard(player);
    }

    @Override
    public void openStatisticsGUI(Player player) {
        plugin.getStatisticsGUIManager().openEventStatisticsGUI(player);
    }

    @Override
    public boolean isGuiEnabled() {
        return plugin.getConfigManager().isGUIEnabled();
    }
} 