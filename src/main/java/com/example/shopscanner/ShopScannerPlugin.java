package com.example.shopscanner;

import org.bukkit.plugin.java.JavaPlugin;

public class ShopScannerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("ShopScanner aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ShopScanner deaktiviert!");
    }
}
