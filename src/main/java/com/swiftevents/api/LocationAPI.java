package com.swiftevents.api;

import com.swiftevents.locations.PresetLocation;
import org.bukkit.Location;

import java.util.Optional;
import java.util.Set;

/**
 * API for managing preset event locations
 */
public interface LocationAPI {

    /**
     * Get a preset location by its name.
     *
     * @param name The case-insensitive name of the location.
     * @return An Optional containing the PresetLocation if found.
     */
    Optional<PresetLocation> getPresetLocation(String name);

    /**
     * Get the names of all preset locations.
     *
     * @return A set of location names.
     */
    Set<String> getPresetLocationNames();

    /**
     * Add a new preset location.
     *
     * @param name     The name for the new location.
     * @param location The Bukkit location to save.
     * @return true if the location was added, false if a location with that name already exists.
     */
    boolean addPresetLocation(String name, Location location);

    /**
     * Remove a preset location.
     *
     * @param name The case-insensitive name of the location to remove.
     * @return true if the location was found and removed.
     */
    boolean removePresetLocation(String name);

    /**
     * Reload all locations from the configuration.
     */
    void reloadLocations();
} 