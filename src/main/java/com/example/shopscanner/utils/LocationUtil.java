package com.example.shopscanner.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Utility class for serializing and deserializing Bukkit Locations.
 * Format: "worldName;x;y;z" (block coordinates, integers)
 */
public final class LocationUtil {

    private static final String SEPARATOR = ";";

    private LocationUtil() {
    }

    /**
     * Serializes a Location to string format "worldName;x;y;z".
     *
     * @param loc the location to serialize
     * @return serialized string, or null if location or world is null
     */
    public static String serialize(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return loc.getWorld().getName() + SEPARATOR
                + loc.getBlockX() + SEPARATOR
                + loc.getBlockY() + SEPARATOR
                + loc.getBlockZ();
    }

    /**
     * Deserializes a string to a Location.
     *
     * @param str the serialized string in format "worldName;x;y;z"
     * @return the Location, or null if parsing fails or world doesn't exist
     */
    public static Location deserialize(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        String[] parts = str.split(SEPARATOR);
        if (parts.length != 4) {
            return null;
        }
        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                return null;
            }
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
