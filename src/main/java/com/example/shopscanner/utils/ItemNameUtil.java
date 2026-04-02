package com.example.shopscanner.utils;

import org.bukkit.Material;

/**
 * Utility class for formatting Material names to human-readable form.
 */
public final class ItemNameUtil {

    private ItemNameUtil() {
    }

    /**
     * Formats a Material name to title case with spaces.
     * Example: DIAMOND_BLOCK → "Diamond Block"
     *
     * @param material the material to format
     * @return formatted name, or empty string if material is null
     */
    public static String formatMaterialName(Material material) {
        if (material == null) {
            return "";
        }
        return formatMaterialName(material.name());
    }

    /**
     * Formats a material name string to title case with spaces.
     * Example: "DIAMOND_BLOCK" → "Diamond Block"
     *
     * @param name the material name to format
     * @return formatted name, or empty string if name is null or empty
     */
    public static String formatMaterialName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String[] words = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            result.append(capitalize(words[i]));
        }
        return result.toString();
    }

    private static String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
