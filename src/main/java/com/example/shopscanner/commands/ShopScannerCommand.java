package com.example.shopscanner.commands;

import com.example.shopscanner.ShopScannerPlugin;
import com.example.shopscanner.managers.ScannerItemManager;
import com.example.shopscanner.managers.ShopManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Handles /shopscanner commands: give, reload, list
 */
@SuppressWarnings("UnstableApiUsage")
public class ShopScannerCommand {

    private final ShopScannerPlugin plugin;
    private final ShopManager shopManager;

    public ShopScannerCommand(ShopScannerPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    public void register() {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("shopscanner")
                            .requires(source -> source.getSender().hasPermission("shopscanner.use"))
                            .then(Commands.literal("give")
                                    .requires(source -> source.getSender().hasPermission("shopscanner.give"))
                                    .then(Commands.argument("shopname", StringArgumentType.greedyString())
                                            .executes(this::executeGive)))
                            .then(Commands.literal("reload")
                                    .requires(source -> source.getSender().hasPermission("shopscanner.admin"))
                                    .executes(this::executeReload))
                            .then(Commands.literal("list")
                                    .requires(source -> source.getSender().hasPermission("shopscanner.admin"))
                                    .executes(this::executeList))
                            .executes(this::executeUsage)
                            .build(),
                    "ShopScanner administration",
                    java.util.List.of("scanner", "ss")
            );
        });
    }

    private int executeGive(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String input = StringArgumentType.getString(ctx, "shopname");

        // Parse: might be "shopname" or "shopname playername"
        String[] parts = input.split(" ");
        String shopName;
        Player targetPlayer;

        // Check if last word is an online player
        Player possibleTarget = Bukkit.getPlayer(parts[parts.length - 1]);
        if (possibleTarget != null && parts.length > 1) {
            // Last word is a player
            shopName = String.join(" ", java.util.Arrays.copyOfRange(parts, 0, parts.length - 1));
            targetPlayer = possibleTarget;
        } else {
            // All words are shop name
            shopName = input;
            if (!(sender instanceof Player player)) {
                sendError(sender, "Console must specify a target player.");
                return Command.SINGLE_SUCCESS;
            }
            targetPlayer = player;
        }

        // Create and give scanner
        ItemStack scanner = ScannerItemManager.createScanner(shopName);

        if (targetPlayer.getInventory().firstEmpty() == -1) {
            targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), scanner);
            sendSuccess(sender, "Scanner for '" + shopName + "' dropped at " + targetPlayer.getName() + "'s feet.");
        } else {
            targetPlayer.getInventory().addItem(scanner);
            sendSuccess(sender, "Scanner for '" + shopName + "' given to " + targetPlayer.getName() + ".");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        plugin.reloadConfig();
        shopManager.load();

        sendSuccess(sender, "Configuration and shops reloaded.");
        return Command.SINGLE_SUCCESS;
    }

    private int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        Map<String, Integer> shops = shopManager.listAllShops();

        if (shops.isEmpty()) {
            sendInfo(sender, "No shops registered.");
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(Component.text("[Scanner] ", NamedTextColor.GOLD)
                .append(Component.text("Registered shops:", NamedTextColor.WHITE)));

        for (Map.Entry<String, Integer> entry : shops.entrySet()) {
            sender.sendMessage(Component.text("  - " + entry.getKey(), NamedTextColor.GRAY)
                    .append(Component.text(" (" + entry.getValue() + " containers)", NamedTextColor.DARK_GRAY)));
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeUsage(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        sender.sendMessage(Component.text("[Scanner] ", NamedTextColor.GOLD)
                .append(Component.text("Usage:", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  /shopscanner give <shopname> [player]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /shopscanner reload", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /shopscanner list", NamedTextColor.GRAY));

        return Command.SINGLE_SUCCESS;
    }

    private void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[Scanner] ", NamedTextColor.GOLD)
                .append(Component.text(message, NamedTextColor.GREEN)));
    }

    private void sendError(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[Scanner] ", NamedTextColor.GOLD)
                .append(Component.text(message, NamedTextColor.RED)));
    }

    private void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[Scanner] ", NamedTextColor.GOLD)
                .append(Component.text(message, NamedTextColor.GRAY)));
    }
}
