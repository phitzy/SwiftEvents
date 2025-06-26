package com.swiftevents.api;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.locations.PresetLocation;
import org.bukkit.Location;

import java.util.Optional;
import java.util.Set;

public class LocationAPIManager implements LocationAPI {

    private final SwiftEventsPlugin plugin;

    public LocationAPIManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<PresetLocation> getPresetLocation(String name) {
        return plugin.getLocationManager().getPresetLocation(name);
    }

    @Override
    public Set<String> getPresetLocationNames() {
        return plugin.getLocationManager().getPresetLocationNames();
    }

    @Override
    public boolean addPresetLocation(String name, Location location) {
        return plugin.getLocationManager().addPresetLocation(name, location);
    }

    @Override
    public boolean removePresetLocation(String name) {
        return plugin.getLocationManager().removePresetLocation(name);
    }

    @Override
    public void reloadLocations() {
        plugin.getLocationManager().loadLocations();
    }
} 