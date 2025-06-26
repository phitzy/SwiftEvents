package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.tasker.EventPreset;

import java.util.Collections;
import java.util.Map;

public class TaskerAPIManager implements TaskerAPI {

    private final SwiftEventsPlugin plugin;

    public TaskerAPIManager(SwiftEventsPlugin plugin) {
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
        return Collections.unmodifiableMap(plugin.getEventTasker().getPresets());
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
        plugin.getEventTasker().setPresetEnabled(presetId, enabled);
        // The original method is void, but the API defines a boolean return.
        // We can check if the preset exists to provide a meaningful return.
        return plugin.getEventTasker().getPreset(presetId) != null;
    }

    @Override
    public void restart() {
        plugin.getEventTasker().restart();
    }
} 