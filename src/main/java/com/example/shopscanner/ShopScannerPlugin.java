package com.example.shopscanner;

import com.example.shopscanner.commands.ShopScannerCommand;
import com.example.shopscanner.listeners.ContainerBreakListener;
import com.example.shopscanner.listeners.ScannerInteractListener;
import com.example.shopscanner.managers.ShopManager;
import com.example.shopscanner.visual.ContainerHighlighter;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopScannerPlugin extends JavaPlugin {

    private ShopManager shopManager;
    private ContainerHighlighter highlighter;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        shopManager = new ShopManager(this);
        shopManager.load();

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new ScannerInteractListener(this, shopManager), this);
        getServer().getPluginManager().registerEvents(
                new ContainerBreakListener(this, shopManager), this);

        // Register highlighter
        highlighter = new ContainerHighlighter(this, shopManager);
        getServer().getPluginManager().registerEvents(highlighter, this);

        // Register commands
        new ShopScannerCommand(this, shopManager).register();

        getLogger().info("ShopScanner enabled!");
    }

    @Override
    public void onDisable() {
        if (highlighter != null) {
            highlighter.stopAll();
        }
        if (shopManager != null) {
            shopManager.save();
        }
        getLogger().info("ShopScanner disabled!");
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}
