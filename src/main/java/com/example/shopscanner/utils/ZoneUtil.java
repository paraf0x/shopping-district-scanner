package com.example.shopscanner.utils;

import com.example.shopscanner.ShopScannerPlugin;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Utility class for shopping district zone and scan radius checks.
 */
public final class ZoneUtil {

    private ZoneUtil() {
    }

    /**
     * Checks if a location is within the configured shopping district zone.
     *
     * @param loc    the location to check
     * @param plugin the plugin instance for config access
     * @return true if within zone, false otherwise
     */
    public static boolean isInShoppingZone(Location loc, ShopScannerPlugin plugin) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }

        World world = loc.getWorld();

        // Only Overworld (NORMAL environment)
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }

        // Check configured world name
        String configWorld = plugin.getConfig().getString("shopping-district.world", "world");
        if (!world.getName().equals(configWorld)) {
            return false;
        }

        // Get zone config
        int centerChunkX = plugin.getConfig().getInt("shopping-district.center-chunk-x", 0);
        int centerChunkZ = plugin.getConfig().getInt("shopping-district.center-chunk-z", 0);
        int radiusChunks = plugin.getConfig().getInt("shopping-district.radius-chunks", 5);

        // Convert block coords to chunk coords
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        // Check if within radius
        return Math.abs(chunkX - centerChunkX) <= radiusChunks
                && Math.abs(chunkZ - centerChunkZ) <= radiusChunks;
    }

    /**
     * Checks if a container is within the 3x3 chunk scan radius around a lectern.
     *
     * @param lectern   the lectern location
     * @param container the container location
     * @return true if within scan radius (±1 chunk), false otherwise
     */
    public static boolean isInScanRadius(Location lectern, Location container) {
        if (lectern == null || container == null) {
            return false;
        }
        if (lectern.getWorld() == null || container.getWorld() == null) {
            return false;
        }
        // Must be same world
        if (!lectern.getWorld().equals(container.getWorld())) {
            return false;
        }

        int lecternChunkX = lectern.getBlockX() >> 4;
        int lecternChunkZ = lectern.getBlockZ() >> 4;
        int containerChunkX = container.getBlockX() >> 4;
        int containerChunkZ = container.getBlockZ() >> 4;

        // 3x3 chunk area = ±1 chunk from lectern
        return Math.abs(lecternChunkX - containerChunkX) <= 1
                && Math.abs(lecternChunkZ - containerChunkZ) <= 1;
    }
}
