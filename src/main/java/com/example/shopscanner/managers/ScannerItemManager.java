package com.example.shopscanner.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Utility methods for creating and validating scanner items.
 * A scanner is a BONE with display name starting with "Scanner ".
 */
public final class ScannerItemManager {

    private static final String SCANNER_PREFIX = "Scanner ";
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private ScannerItemManager() {
    }

    /**
     * Checks if an ItemStack is a valid scanner.
     *
     * @param item the item to check
     * @return true if it's a valid scanner
     */
    public static boolean isScanner(ItemStack item) {
        if (item == null || item.getType() != Material.BONE) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        String displayName = PLAIN_SERIALIZER.serialize(meta.displayName());
        if (!displayName.startsWith(SCANNER_PREFIX)) {
            return false;
        }
        // Must have text after "Scanner "
        String shopName = displayName.substring(SCANNER_PREFIX.length());
        return !shopName.isEmpty();
    }

    /**
     * Extracts the shop name from a scanner item.
     *
     * @param item the scanner item
     * @return the shop name, or null if not a valid scanner
     */
    public static String getShopName(ItemStack item) {
        if (!isScanner(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String displayName = PLAIN_SERIALIZER.serialize(meta.displayName());
        return displayName.substring(SCANNER_PREFIX.length());
    }

    /**
     * Creates a scanner item for a shop.
     *
     * @param shopName the shop name
     * @return the scanner ItemStack
     */
    public static ItemStack createScanner(String shopName) {
        ItemStack item = new ItemStack(Material.BONE, 1);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(SCANNER_PREFIX + shopName));
        meta.lore(List.of(
                Component.text("Shop: " + shopName, NamedTextColor.GRAY)
        ));

        item.setItemMeta(meta);
        return item;
    }
}
