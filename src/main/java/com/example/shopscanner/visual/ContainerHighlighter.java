package com.example.shopscanner.visual;

import com.example.shopscanner.ShopScannerPlugin;
import com.example.shopscanner.managers.ScannerItemManager;
import com.example.shopscanner.managers.ShopManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Highlights registered containers with particles when player holds a scanner.
 */
public class ContainerHighlighter implements Listener {

    private final ShopScannerPlugin plugin;
    private final ShopManager shopManager;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, String> activeShops = new HashMap<>();

    // Particle settings
    private static final double RENDER_DISTANCE = 32.0;
    private static final int PARTICLE_COUNT = 8;

    public ContainerHighlighter(ShopScannerPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());

        boolean wasHoldingScanner = ScannerItemManager.isScanner(oldItem);
        boolean nowHoldingScanner = ScannerItemManager.isScanner(newItem);

        if (wasHoldingScanner && !nowHoldingScanner) {
            // Stopped holding scanner
            stopHighlighting(player);
        } else if (nowHoldingScanner && !wasHoldingScanner) {
            // Started holding scanner
            String shopName = ScannerItemManager.getShopName(newItem);
            if (shopName != null) {
                startHighlighting(player, shopName);
            }
        } else if (nowHoldingScanner && wasHoldingScanner) {
            // Still holding a scanner, but might be different shop
            String newShopName = ScannerItemManager.getShopName(newItem);
            String oldShopName = activeShops.get(player.getUniqueId());
            if (newShopName != null && !newShopName.equals(oldShopName)) {
                stopHighlighting(player);
                startHighlighting(player, newShopName);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopHighlighting(event.getPlayer());
    }

    private void startHighlighting(Player player, String shopName) {
        if (!plugin.getConfig().getBoolean("highlighting.enabled", true)) {
            return;
        }

        // Cancel existing task if any
        stopHighlighting(player);

        int interval = plugin.getConfig().getInt("highlighting.update-interval", 10);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopHighlighting(player);
                return;
            }

            // Check if still holding scanner for this shop
            ItemStack held = player.getInventory().getItemInMainHand();
            if (!ScannerItemManager.isScanner(held)) {
                stopHighlighting(player);
                return;
            }

            String currentShop = ScannerItemManager.getShopName(held);
            if (!shopName.equals(currentShop)) {
                stopHighlighting(player);
                return;
            }

            spawnParticles(player, shopName);
        }, 0L, interval);

        activeTasks.put(player.getUniqueId(), task);
        activeShops.put(player.getUniqueId(), shopName);
    }

    private void stopHighlighting(Player player) {
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        activeShops.remove(player.getUniqueId());
    }

    public void stopAll() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        activeShops.clear();
    }

    private void spawnParticles(Player player, String shopName) {
        List<Location> containers = shopManager.getContainers(shopName);
        Location playerLoc = player.getLocation();

        // Get colors from config
        Color containerColor = parseColor(plugin.getConfig().getString("highlighting.container-color", "#00FF00"));
        int particleCount = plugin.getConfig().getInt("highlighting.particle-count", PARTICLE_COUNT);

        Particle.DustOptions containerDust = new Particle.DustOptions(containerColor, 1.0f);

        for (Location containerLoc : containers) {
            if (containerLoc.getWorld() == null || !containerLoc.getWorld().equals(playerLoc.getWorld())) {
                continue;
            }

            double distance = containerLoc.distance(playerLoc);
            if (distance > RENDER_DISTANCE) {
                continue;
            }

            // Spawn ring of particles at block top
            spawnRing(player, containerLoc.clone().add(0.5, 1.1, 0.5), containerDust, particleCount);
        }
    }

    private void spawnRing(Player player, Location center, Particle.DustOptions dust, int count) {
        double radius = 0.5;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            player.spawnParticle(
                    Particle.DUST,
                    x, center.getY(), z,
                    1, // count
                    0, 0, 0, // offset
                    0, // speed
                    dust
            );
        }
    }

    private Color parseColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return Color.GREEN;
        }
        try {
            hex = hex.replace("#", "");
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB(rgb);
        } catch (NumberFormatException e) {
            return Color.GREEN;
        }
    }
}
