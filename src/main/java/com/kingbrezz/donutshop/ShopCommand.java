package com.kingbrezz.donutshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class ShopCommand
        implements CommandExecutor, TabCompleter {

    private final DonutShop plugin;

    public ShopCommand(DonutShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            plugin.getLanguageManager()
                    .send(
                            sender,
                            "messages.player-only"
                    );
            return true;
        }

        if (!player.hasPermission(
                "donutshop.use"
        )) {
            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.no-permission"
                    );
            return true;
        }

        ShopMenu.openMainMenu(
                plugin,
                player
        );

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        return List.of();
    }
        }
