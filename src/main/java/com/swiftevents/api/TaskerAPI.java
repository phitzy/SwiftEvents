package com.swiftevents.api;

import com.swiftevents.tasker.EventPreset;

import java.util.Map;

/**
 * API for interacting with the automatic Event Tasker
 */
public interface TaskerAPI {

    /**
     * Check if the event tasker is currently running.
     *
     * @return true if the tasker is running.
     */
    boolean isRunning();

    /**
     * Get the timestamp of the next scheduled event.
     *
     * @return The system time in milliseconds for the next event.
     */
    long getNextEventTime();

    /**
     * Get the time in seconds until the next automatic event.
     *
     * @return Time in seconds, or -1 if no event is scheduled.
     */
    long getTimeUntilNextEvent();

    /**
     * Get a map of all loaded event presets.
     *
     * @return An unmodifiable map of presets by their ID.
     */
    Map<String, EventPreset> getPresets();

    /**
     * Get a specific event preset by its ID.
     *
     * @param id The ID of the preset.
     * @return The preset, or null if not found.
     */
    EventPreset getPreset(String id);

    /**
     * Force the tasker to start a new event immediately.
     * This will select a random preset and start it.
     */
    void forceNextEvent();

    /**
     * Enable or disable an event preset for the tasker.
     *
     * @param presetId The ID of the preset to modify.
     * @param enabled  True to enable, false to disable.
     * @return true if the preset was found and its state was changed.
     */
    boolean setPresetEnabled(String presetId, boolean enabled);

    /**
     * Restarts the event tasker.
     * This reloads presets and schedules the next event.
     */
    void restart();
} 