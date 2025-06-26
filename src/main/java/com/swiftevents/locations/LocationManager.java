package com.swiftevents.locations;

import com.swiftevents.SwiftEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LocationManager {

    private final SwiftEventsPlugin plugin;
    private final Map<String, PresetLocation> presetLocations = new HashMap<>();

    public LocationManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        loadLocations();
    }

    public void loadLocations() {
        presetLocations.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection locationsSection = config.getConfigurationSection("locations");
        if (locationsSection == null) {
            return;
        }

        for (String key : locationsSection.getKeys(false)) {
            String worldName = locationsSection.getString(key + ".world");
            if (worldName == null) {
                plugin.getLogger().warning("[Locations] World is not defined for location: " + key);
                continue;
            }
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("[Locations] World '" + worldName + "' not found for location: " + key);
                continue;
            }
            double x = locationsSection.getDouble(key + ".x");
            double y = locationsSection.getDouble(key + ".y");
            double z = locationsSection.getDouble(key + ".z");
            float yaw = (float) locationsSection.getDouble(key + ".yaw");
            float pitch = (float) locationsSection.getDouble(key + ".pitch");

            Location location = new Location(world, x, y, z, yaw, pitch);
            presetLocations.put(key.toLowerCase(), new PresetLocation(key, location));
        }
    }

    public void saveLocations() {
        FileConfiguration config = plugin.getConfig();
        config.set("locations", null); // Clear existing locations
        ConfigurationSection locationsSection = config.createSection("locations");

        for (PresetLocation presetLocation : presetLocations.values()) {
            String key = presetLocation.name();
            locationsSection.set(key + ".world", presetLocation.location().getWorld().getName());
            locationsSection.set(key + ".x", presetLocation.location().getX());
            locationsSection.set(key + ".y", presetLocation.location().getY());
            locationsSection.set(key + ".z", presetLocation.location().getZ());
            locationsSection.set(key + ".yaw", presetLocation.location().getYaw());
            locationsSection.set(key + ".pitch", presetLocation.location().getPitch());
        }
        plugin.saveConfig();
    }

    public Optional<PresetLocation> getPresetLocation(String name) {
        return Optional.ofNullable(presetLocations.get(name.toLowerCase()));
    }

    public Set<String> getPresetLocationNames() {
        return presetLocations.values().stream()
                .map(PresetLocation::name)
                .collect(Collectors.toSet());
    }

    public boolean addPresetLocation(String name, Location location) {
        if (presetLocations.containsKey(name.toLowerCase())) {
            return false; // Already exists
        }
        presetLocations.put(name.toLowerCase(), new PresetLocation(name, location));
        saveLocations();
        return true;
    }

    public boolean removePresetLocation(String name) {
        if (presetLocations.remove(name.toLowerCase()) != null) {
            saveLocations();
            return true;
        }
        return false;
    }
} 