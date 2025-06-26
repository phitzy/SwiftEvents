package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.tasker.EventPreset;

import java.util.Map;

public class TaskerAPIImpl implements TaskerAPI {

    private final SwiftEventsPlugin plugin;

    public TaskerAPIImpl(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isRunning() {
        return plugin.getEventTasker().isRunning();
    }

    @Override
    public long getNextEventTime() {
        return plugin.getEventTasker().getNextEventTime();
    }

    @Override
    public long getTimeUntilNextEvent() {
        return plugin.getEventTasker().getTimeUntilNextEvent();
    }

    @Override
    public Map<String, EventPreset> getPresets() {
        return plugin.getEventTasker().getPresets();
    }

    @Override
    public EventPreset getPreset(String id) {
        return plugin.getEventTasker().getPreset(id);
    }

    @Override
    public void forceNextEvent() {
        plugin.getEventTasker().forceNextEvent();
    }

    @Override
    public boolean setPresetEnabled(String presetId, boolean enabled) {
        if (plugin.getEventTasker().getPreset(presetId) != null) {
            plugin.getEventTasker().setPresetEnabled(presetId, enabled);
            return true;
        }
        return false;
    }

    @Override
    public void restart() {
        plugin.getEventTasker().restart();
    }
} 