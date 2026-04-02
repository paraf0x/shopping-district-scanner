package com.example.shopscanner.listeners;

import com.example.shopscanner.ShopScannerPlugin;
import com.example.shopscanner.managers.ScannerItemManager;
import com.example.shopscanner.managers.ShopManager;
import com.example.shopscanner.scanner.BookGenerator;
import com.example.shopscanner.scanner.ContainerScanner;
import com.example.shopscanner.utils.ZoneUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles all scanner interactions: register, deregister, scan.
 */
public class ScannerInteractListener implements Listener {

    private final ShopScannerPlugin plugin;
    private final ShopManager shopManager;

    public ScannerInteractListener(ShopScannerPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle main hand
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if scanner
        if (!ScannerItemManager.isScanner(item)) {
            return;
        }

        // From here on, we have a scanner - cancel event to prevent normal behavior
        event.setCancelled(true);

        String shopName = ScannerItemManager.getShopName(item);
        if (shopName == null) {
            return;
        }

        // Check permission
        if (!player.hasPermission("shopscanner.use")) {
            sendError(player, "You don't have permission to use the scanner.");
            return;
        }

        // Check if player is in shopping zone
        if (!ZoneUtil.isInShoppingZone(player.getLocation(), plugin)) {
            sendError(player, "You can only use the scanner in the shopping district.");
            return;
        }

        Block block = event.getClickedBlock();
        Action action = event.getAction();

        // Determine action based on click type and block
        if (action == Action.LEFT_CLICK_BLOCK && player.isSneaking() && block != null) {
            // Shift + Left click on block -> Register
            handleRegister(player, block, shopName);
        } else if (action == Action.RIGHT_CLICK_BLOCK && block != null) {
            if (player.isSneaking()) {
                // Shift + Right click
                if (block.getState() instanceof Container && !(block.getState() instanceof Lectern)) {
                    // Shift + Right click on Container (not Lectern) -> Deregister
                    handleDeregister(player, block, shopName);
                }
                // Shift + Right click on Lectern -> do nothing (intentionally)
            } else {
                // Right click (no shift)
                if (block.getState() instanceof Lectern) {
                    // Right click on Lectern -> Scan
                    handleScan(player, block, shopName);
                }
            }
        }
    }

    private void handleRegister(Player player, Block block, String shopName) {
        // Check if block is in shopping zone
        if (!ZoneUtil.isInShoppingZone(block.getLocation(), plugin)) {
            sendError(player, "This container is outside the shopping district.");
            return;
        }

        // Check if block is a container
        if (!(block.getState() instanceof Container)) {
            sendError(player, "This block is not a container.");
            return;
        }

        // Check max container limit
        int maxContainers = plugin.getConfig().getInt("max-containers-per-shop", 54);
        if (shopManager.getContainerCount(shopName) >= maxContainers) {
            sendError(player, "Shop has reached the maximum of " + maxContainers + " containers.");
            return;
        }

        // Check if already registered in this shop
        if (shopManager.isContainerRegistered(shopName, block.getLocation())) {
            sendError(player, "This container is already registered to shop '" + shopName + "'.");
            return;
        }

        // Double chest warning
        checkDoubleChestWarning(player, block, shopName);

        // Register
        if (shopManager.addContainer(shopName, block.getLocation())) {
            sendSuccess(player, "Container registered to shop '" + shopName + "'.");
            playSound(player, "register");
        } else {
            sendError(player, "Failed to register container.");
        }
    }

    private void handleDeregister(Player player, Block block, String shopName) {
        // Check if registered in this shop
        if (!shopManager.isContainerRegistered(shopName, block.getLocation())) {
            sendError(player, "This container is not registered to shop '" + shopName + "'.");
            return;
        }

        // Deregister
        if (shopManager.removeContainer(shopName, block.getLocation())) {
            sendSuccess(player, "Container removed from shop '" + shopName + "'.");
            playSound(player, "deregister");
        } else {
            sendError(player, "Failed to remove container.");
        }
    }

    private void handleScan(Player player, Block block, String shopName) {
        // Check if lectern is in shopping zone
        if (!ZoneUtil.isInShoppingZone(block.getLocation(), plugin)) {
            sendError(player, "This lectern is outside the shopping district.");
            return;
        }

        // Check if lectern has a book to overwrite
        Lectern lectern = (Lectern) block.getState();
        ItemStack existingBook = lectern.getInventory().getItem(0);
        if (existingBook == null || existingBook.getType() == org.bukkit.Material.AIR) {
            sendError(player, "Place a book in the lectern first.");
            return;
        }

        // Safety check: only allow replacing empty Book and Quill or same shop's scan result
        if (existingBook.getType() == org.bukkit.Material.WRITABLE_BOOK) {
            // Empty book and quill - OK to replace
        } else if (existingBook.getType() == org.bukkit.Material.WRITTEN_BOOK) {
            // Check if it's a scan result from this shop (title matches shop name)
            org.bukkit.inventory.meta.BookMeta bookMeta = (org.bukkit.inventory.meta.BookMeta) existingBook.getItemMeta();
            String bookTitle = bookMeta.getTitle();
            if (bookTitle == null || !bookTitle.equals(shopName)) {
                sendError(player, "Cannot overwrite: book is not from this shop.");
                return;
            }
            // Same shop's book - OK to replace
        } else {
            sendError(player, "Place a Book and Quill in the lectern.");
            return;
        }

        // Check if shop has containers
        if (!shopManager.hasShop(shopName)) {
            sendError(player, "Shop '" + shopName + "' has no registered containers.");
            return;
        }

        // Get containers
        List<Location> containers = shopManager.getContainers(shopName);
        if (containers.isEmpty()) {
            sendError(player, "Shop '" + shopName + "' has no registered containers.");
            return;
        }

        // Scan containers
        List<ContainerScanner.ContainerResult> results = ContainerScanner.scanContainers(
                containers, block.getLocation());

        // Auto-deregister containers that no longer exist
        for (ContainerScanner.ContainerResult result : results) {
            if (result.status() == ContainerScanner.ContainerStatus.NOT_A_CONTAINER) {
                shopManager.removeContainer(shopName, result.location());
            }
        }

        // Generate book
        ItemStack book = BookGenerator.generateBook(shopName, results);

        // Get fresh lectern state and replace book
        Lectern freshLectern = (Lectern) block.getState();
        freshLectern.getSnapshotInventory().setItem(0, book);
        freshLectern.update(true, false);

        sendSuccess(player, "Scanned " + results.size() + " containers for shop '" + shopName + "'.");
        playSound(player, "scan-complete");
    }

    private void checkDoubleChestWarning(Player player, Block block, String shopName) {
        if (!(block.getState() instanceof Chest chest)) {
            return;
        }

        org.bukkit.block.data.type.Chest chestData = (org.bukkit.block.data.type.Chest) block.getBlockData();
        if (chestData.getType() == Type.SINGLE) {
            return;
        }

        // Find the other half
        Location otherHalf = getOtherChestHalf(block, chestData);
        if (otherHalf != null && shopManager.isContainerRegistered(shopName, otherHalf)) {
            sendWarning(player, "Warning: The other half of this double chest is already registered. Items may be counted twice.");
        }
    }

    private Location getOtherChestHalf(Block block, org.bukkit.block.data.type.Chest chestData) {
        org.bukkit.block.data.type.Chest.Type type = chestData.getType();
        if (type == Type.SINGLE) {
            return null;
        }

        org.bukkit.block.BlockFace facing = chestData.getFacing();
        org.bukkit.block.BlockFace otherFace;

        if (type == Type.LEFT) {
            otherFace = getRightFace(facing);
        } else {
            otherFace = getLeftFace(facing);
        }

        return block.getRelative(otherFace).getLocation();
    }

    private org.bukkit.block.BlockFace getRightFace(org.bukkit.block.BlockFace facing) {
        return switch (facing) {
            case NORTH -> org.bukkit.block.BlockFace.EAST;
            case EAST -> org.bukkit.block.BlockFace.SOUTH;
            case SOUTH -> org.bukkit.block.BlockFace.WEST;
            case WEST -> org.bukkit.block.BlockFace.NORTH;
            default -> org.bukkit.block.BlockFace.NORTH;
        };
    }

    private org.bukkit.block.BlockFace getLeftFace(org.bukkit.block.BlockFace facing) {
        return switch (facing) {
            case NORTH -> org.bukkit.block.BlockFace.WEST;
            case EAST -> org.bukkit.block.BlockFace.NORTH;
            case SOUTH -> org.bukkit.block.BlockFace.EAST;
            case WEST -> org.bukkit.block.BlockFace.SOUTH;
            default -> org.bukkit.block.BlockFace.NORTH;
        };
    }

    private void sendSuccess(Player player, String message) {
        String prefix = plugin.getConfig().getString("prefix", "§6[Scanner]§r ");
        player.sendMessage(Component.text(prefix).append(Component.text(message, NamedTextColor.GREEN)));
    }

    private void sendWarning(Player player, String message) {
        String prefix = plugin.getConfig().getString("prefix", "§6[Scanner]§r ");
        player.sendMessage(Component.text(prefix).append(Component.text(message, NamedTextColor.YELLOW)));
    }

    private void sendError(Player player, String message) {
        String prefix = plugin.getConfig().getString("prefix", "§6[Scanner]§r ");
        player.sendMessage(Component.text(prefix).append(Component.text(message, NamedTextColor.RED)));
        playSound(player, "error");
    }

    private void playSound(Player player, String soundKey) {
        String soundName = plugin.getConfig().getString("sounds." + soundKey);
        if (soundName == null || soundName.isEmpty()) {
            return;
        }
        try {
            // Use sound ID directly (e.g., block.chest.locked)
            // Also supports legacy format BLOCK_CHEST_LOCKED -> block.chest.locked
            String soundId = soundName.toLowerCase().replace("_", ".");
            player.playSound(player.getLocation(), soundId, 1.0f, 1.0f);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound in config: " + soundName);
        }
    }
}
