package com.kingbrezz.donutshop;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopEditCommand
        implements CommandExecutor, TabCompleter {

    private final DonutShop plugin;

    public ShopEditCommand(DonutShop plugin) {
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

        if (!player.hasPermission(
                "donutshop.admin.shopedit"
        )) {
            plugin.getLanguageManager()
                    .send(player, "messages.no-permission");
            return true;
        }

        if (!plugin.getConfig().getBoolean(
                "shop-edit.enabled",
                true
        )) {
            plugin.getLanguageManager()
                    .send(player, "messages.shopedit-disabled");
            return true;
        }

        if (args.length == 0) {
            plugin.getLanguageManager()
                    .send(player, "messages.shopedit-usage");
            return true;
        }

        String categoryId =
                args[0].toLowerCase(Locale.ROOT);

        ShopCategory category =
                plugin.getShopManager()
                        .getCategory(categoryId);

        if (category == null) {
            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.category-not-found",
                            "category",
                            categoryId
                    );
            return true;
        }

        ShopEditMenu.open(
                plugin,
                player,
                category
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
        if (args.length != 1) {
            return List.of();
        }

        String input =
                args[0].toLowerCase(Locale.ROOT);

        List<String> result =
                new ArrayList<>();

        for (ShopCategory category :
                plugin.getShopManager().getCategories()) {

            String id = category.id();

            if (id.toLowerCase(Locale.ROOT)
                    .startsWith(input)) {

                result.add(id);
            }
        }

        return result;
    }
}
