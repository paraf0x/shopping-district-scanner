package com.example.shopscanner.listeners;

import com.example.shopscanner.ShopScannerPlugin;
import com.example.shopscanner.managers.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

/**
 * Auto-deregisters containers when they are destroyed.
 */
public class ContainerBreakListener implements Listener {

    private final ShopScannerPlugin plugin;
    private final ShopManager shopManager;

    public ContainerBreakListener(ShopScannerPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Only check if it's a container type
        if (!(block.getState() instanceof Container)) {
            return;
        }

        Location location = block.getLocation();
        List<String> shops = shopManager.findShopsContaining(location);

        if (shops.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        String prefix = plugin.getConfig().getString("prefix", "§6[Scanner]§r ");

        for (String shopName : shops) {
            shopManager.removeContainer(shopName, location);
            player.sendMessage(Component.text(prefix)
                    .append(Component.text("Container removed from shop '" + shopName + "'.", NamedTextColor.YELLOW)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        for (Block block : blocks) {
            // Only check if it's a container type
            if (!(block.getState() instanceof Container)) {
                continue;
            }

            Location location = block.getLocation();
            List<String> shops = shopManager.findShopsContaining(location);

            for (String shopName : shops) {
                shopManager.removeContainer(shopName, location);
                plugin.getLogger().info("Container at " + formatLocation(location)
                        + " removed from shop '" + shopName + "' (explosion)");
            }
        }
    }

    private String formatLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}
