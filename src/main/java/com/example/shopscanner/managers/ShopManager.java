package com.example.shopscanner.managers;

import com.example.shopscanner.ShopScannerPlugin;
import com.example.shopscanner.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages shop-to-container mappings with YAML persistence.
 */
public class ShopManager {

    private final ShopScannerPlugin plugin;
    private final File shopsFile;
    private Map<String, List<String>> shops;

    public ShopManager(ShopScannerPlugin plugin) {
        this.plugin = plugin;
        this.shopsFile = new File(plugin.getDataFolder(), "shops.yml");
        this.shops = new HashMap<>();
    }

    /**
     * Loads shops from YAML file.
     */
    public void load() {
        shops.clear();
        if (!shopsFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(shopsFile);
        if (!config.contains("shops")) {
            return;
        }

        for (String shopName : config.getConfigurationSection("shops").getKeys(false)) {
            List<String> containers = config.getStringList("shops." + shopName + ".containers");
            if (!containers.isEmpty()) {
                shops.put(shopName, new ArrayList<>(containers));
            }
        }

        plugin.getLogger().info("Loaded " + shops.size() + " shops");
    }

    /**
     * Saves shops to YAML file.
     */
    public void save() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, List<String>> entry : shops.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                config.set("shops." + entry.getKey() + ".containers", entry.getValue());
            }
        }

        try {
            config.save(shopsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shops.yml: " + e.getMessage());
        }
    }

    /**
     * Adds a container to a shop.
     *
     * @param shopName the shop name
     * @param loc      the container location
     * @return true if added, false if already exists
     */
    public boolean addContainer(String shopName, Location loc) {
        String serialized = LocationUtil.serialize(loc);
        if (serialized == null) {
            return false;
        }

        List<String> containers = shops.computeIfAbsent(shopName, k -> new ArrayList<>());
        if (containers.contains(serialized)) {
            return false;
        }

        containers.add(serialized);
        save();
        return true;
    }

    /**
     * Removes a container from a shop.
     *
     * @param shopName the shop name
     * @param loc      the container location
     * @return true if removed, false if not found
     */
    public boolean removeContainer(String shopName, Location loc) {
        String serialized = LocationUtil.serialize(loc);
        if (serialized == null) {
            return false;
        }

        List<String> containers = shops.get(shopName);
        if (containers == null) {
            return false;
        }

        boolean removed = containers.remove(serialized);
        if (removed) {
            // Clean up empty shops
            if (containers.isEmpty()) {
                shops.remove(shopName);
            }
            save();
        }
        return removed;
    }

    /**
     * Removes a container from ALL shops (for auto-deregister on block break).
     *
     * @param loc the container location
     */
    public void removeContainerFromAllShops(Location loc) {
        String serialized = LocationUtil.serialize(loc);
        if (serialized == null) {
            return;
        }

        boolean changed = false;
        List<String> emptyShops = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : shops.entrySet()) {
            if (entry.getValue().remove(serialized)) {
                changed = true;
                if (entry.getValue().isEmpty()) {
                    emptyShops.add(entry.getKey());
                }
            }
        }

        // Clean up empty shops
        for (String shopName : emptyShops) {
            shops.remove(shopName);
        }

        if (changed) {
            save();
        }
    }

    /**
     * Gets all container locations for a shop.
     *
     * @param shopName the shop name
     * @return list of locations (may contain nulls if world doesn't exist)
     */
    public List<Location> getContainers(String shopName) {
        List<String> serialized = shops.get(shopName);
        if (serialized == null) {
            return new ArrayList<>();
        }
        return serialized.stream()
                .map(LocationUtil::deserialize)
                .collect(Collectors.toList());
    }

    /**
     * Gets raw container strings for a shop.
     *
     * @param shopName the shop name
     * @return list of serialized location strings
     */
    public List<String> getContainerStrings(String shopName) {
        return shops.getOrDefault(shopName, new ArrayList<>());
    }

    /**
     * Checks if a shop exists.
     *
     * @param shopName the shop name
     * @return true if shop exists
     */
    public boolean hasShop(String shopName) {
        return shops.containsKey(shopName) && !shops.get(shopName).isEmpty();
    }

    /**
     * Gets the container count for a shop.
     *
     * @param shopName the shop name
     * @return number of containers
     */
    public int getContainerCount(String shopName) {
        List<String> containers = shops.get(shopName);
        return containers == null ? 0 : containers.size();
    }

    /**
     * Lists all shops with their container counts.
     *
     * @return map of shop name to container count
     */
    public Map<String, Integer> listAllShops() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : shops.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.put(entry.getKey(), entry.getValue().size());
            }
        }
        return result;
    }

    /**
     * Finds all shops containing a specific container.
     *
     * @param loc the container location
     * @return list of shop names
     */
    public List<String> findShopsContaining(Location loc) {
        String serialized = LocationUtil.serialize(loc);
        if (serialized == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : shops.entrySet()) {
            if (entry.getValue().contains(serialized)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Checks if a container is registered in a specific shop.
     *
     * @param shopName the shop name
     * @param loc      the container location
     * @return true if registered
     */
    public boolean isContainerRegistered(String shopName, Location loc) {
        String serialized = LocationUtil.serialize(loc);
        if (serialized == null) {
            return false;
        }
        List<String> containers = shops.get(shopName);
        return containers != null && containers.contains(serialized);
    }
}
