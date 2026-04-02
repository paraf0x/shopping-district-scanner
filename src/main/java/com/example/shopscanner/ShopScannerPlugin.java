package com.example.shopscanner;

import com.example.shopscanner.managers.ShopManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopScannerPlugin extends JavaPlugin {

    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        shopManager = new ShopManager(this);
        shopManager.load();

        getLogger().info("ShopScanner enabled!");
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.save();
        }
        getLogger().info("ShopScanner disabled!");
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}
