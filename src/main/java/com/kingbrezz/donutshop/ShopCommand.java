package com.kingbrezz.donutshop;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                    .send(sender, "messages.player-only");
            return true;
        }

        if (!player.hasPermission("donutshop.use")) {
            plugin.getLanguageManager()
                    .send(player, "messages.no-permission");
            return true;
        }

        new ShopMenu(plugin).openMainMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        return new ArrayList<>();
    }
}
