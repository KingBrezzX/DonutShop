package com.kingbrezz.donutshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Administrative command for reload/version operations. */
public final class DonutShopCommand implements CommandExecutor, TabCompleter {
    private final DonutShop plugin;

    public DonutShopCommand(DonutShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutshop.admin")) {
            plugin.getLanguageManager().send(sender, "messages.no-permission");
            return true;
        }

        if (args.length == 0) {
            plugin.getLanguageManager().send(sender, "messages.admin-help");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("donutshop.admin.reload")) {
                    plugin.getLanguageManager().send(sender, "messages.no-permission");
                    return true;
                }
                boolean success = plugin.reloadPlugin();
                plugin.getLanguageManager().send(sender,
                        success ? "messages.reload-success" : "messages.reload-failed");
            }
            case "version" -> plugin.getLanguageManager().send(sender, "messages.version",
                    Map.of("version", plugin.getPluginMeta().getVersion()));
            default -> plugin.getLanguageManager().send(sender, "messages.admin-help");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String query = args[0].toLowerCase(Locale.ROOT);
        return List.of("reload", "version").stream()
                .filter(value -> value.startsWith(query))
                .toList();
    }
}
