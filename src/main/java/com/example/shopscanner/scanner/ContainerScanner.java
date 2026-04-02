package com.example.shopscanner.scanner;

import com.example.shopscanner.utils.ItemNameUtil;
import com.example.shopscanner.utils.ZoneUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans container inventories and counts items.
 */
public final class ContainerScanner {

    private ContainerScanner() {
    }

    /**
     * Status of a container scan.
     */
    public enum ContainerStatus {
        OK,                    // Successfully scanned
        EMPTY,                 // Container is empty
        CHUNK_NOT_LOADED,      // Chunk not loaded
        NOT_A_CONTAINER,       // Block is no longer a container -> auto-deregister
        OUT_OF_SCAN_RADIUS     // Outside 3x3 around lectern
    }

    /**
     * Result of scanning a single container.
     */
    public record ContainerResult(
            Location location,
            String containerTypeName,
            ContainerStatus status,
            Map<Material, Integer> items
    ) {
    }

    /**
     * Scans all containers and returns results.
     *
     * @param containerLocations list of container locations to scan
     * @param lecternLocation    the lectern location (for scan radius check)
     * @return list of scan results
     */
    public static List<ContainerResult> scanContainers(
            List<Location> containerLocations,
            Location lecternLocation
    ) {
        List<ContainerResult> results = new ArrayList<>();

        for (Location loc : containerLocations) {
            if (loc == null) {
                continue;
            }
            results.add(scanSingleContainer(loc, lecternLocation));
        }

        return results;
    }

    private static ContainerResult scanSingleContainer(Location loc, Location lecternLocation) {
        // Check scan radius (3x3 chunks around lectern)
        if (!ZoneUtil.isInScanRadius(lecternLocation, loc)) {
            return new ContainerResult(
                    loc,
                    getContainerTypeName(loc),
                    ContainerStatus.OUT_OF_SCAN_RADIUS,
                    null
            );
        }

        // Check if chunk is loaded
        if (!isChunkLoaded(loc)) {
            return new ContainerResult(
                    loc,
                    getContainerTypeName(loc),
                    ContainerStatus.CHUNK_NOT_LOADED,
                    null
            );
        }

        // Get block and check if it's a container
        Block block = loc.getBlock();
        if (!(block.getState() instanceof Container container)) {
            return new ContainerResult(
                    loc,
                    null,
                    ContainerStatus.NOT_A_CONTAINER,
                    null
            );
        }

        // Get container type name
        String typeName = ItemNameUtil.formatMaterialName(block.getType());

        // Scan inventory
        Inventory inventory = container.getInventory();
        Map<Material, Integer> items = countItems(inventory);

        // Determine status
        ContainerStatus status = items.isEmpty() ? ContainerStatus.EMPTY : ContainerStatus.OK;

        return new ContainerResult(loc, typeName, status, items);
    }

    private static boolean isChunkLoaded(Location loc) {
        if (loc.getWorld() == null) {
            return false;
        }
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        return loc.getWorld().isChunkLoaded(chunkX, chunkZ);
    }

    private static String getContainerTypeName(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        if (!isChunkLoaded(loc)) {
            return null;
        }
        Block block = loc.getBlock();
        if (block.getState() instanceof Container) {
            return ItemNameUtil.formatMaterialName(block.getType());
        }
        return null;
    }

    private static Map<Material, Integer> countItems(Inventory inventory) {
        Map<Material, Integer> items = new HashMap<>();

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            items.merge(item.getType(), item.getAmount(), Integer::sum);
        }

        return items;
    }
}
