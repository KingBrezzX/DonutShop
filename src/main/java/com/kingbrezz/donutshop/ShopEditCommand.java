package com.kingbrezz.donutshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Opens the protected ShopEdit session for administrators. */
public final class ShopEditCommand implements CommandExecutor, TabCompleter {
    private final DonutShop plugin;

    public ShopEditCommand(DonutShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLanguageManager().send(sender, "messages.player-only");
            return true;
        }
        if (!plugin.isReady()) {
            plugin.getLanguageManager().send(player, "messages.economy-unavailable");
            return true;
        }
        if (!player.hasPermission("donutshop.admin.shopedit")) {
            plugin.getLanguageManager().send(player, "messages.no-permission");
            return true;
        }
        if (!plugin.getConfig().getBoolean("shop-edit.enabled", true)) {
            plugin.getLanguageManager().send(player, "messages.shopedit-disabled");
            return true;
        }
        if (args.length != 1) {
            plugin.getLanguageManager().send(player, "messages.shopedit-usage");
            return true;
        }

        ShopCategory category = plugin.getShopManager().getCategory(args[0]);
        if (category == null) {
            plugin.getLanguageManager().send(player, "messages.category-not-found",
                    Map.of("category", args[0]));
            return true;
        }
        ShopEditMenu.open(plugin, player, category);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String query = args[0].toLowerCase(Locale.ROOT);
        if (!plugin.isReady()) return List.of();
        return plugin.getShopManager().getCategories().stream()
                .map(ShopCategory::id)
                .filter(id -> id.startsWith(query))
                .toList();
    }
}
