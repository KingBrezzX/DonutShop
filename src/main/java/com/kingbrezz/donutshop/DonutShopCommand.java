package com.kingbrezz.donutshop;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class DonutShopCommand
        implements CommandExecutor, TabCompleter {

    private final DonutShop plugin;

    public DonutShopCommand(DonutShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission("donutshop.admin")) {
            plugin.getLanguageManager()
                    .send(sender, "messages.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand =
                args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {

            case "reload" -> {

                if (!sender.hasPermission(
                        "donutshop.admin.reload"
                )) {
                    plugin.getLanguageManager()
                            .send(
                                    sender,
                                    "messages.no-permission"
                            );
                    return true;
                }

                plugin.reloadPlugin();

                plugin.getLanguageManager()
                        .send(
                                sender,
                                "messages.reload-success"
                        );
            }

            case "version" -> {

                String version =
                        plugin.getPluginMeta()
                                .getVersion();

                sender.sendMessage(
                        color(
                                "&8&m--------------------------"
                        )
                );

                sender.sendMessage(
                        color(
                                "&b&lDonutShop"
                        )
                );

                sender.sendMessage(
                        color(
                                "&7Author: &fKingBrezz"
                        )
                );

                sender.sendMessage(
                        color(
                                "&7Version: &f" + version
                        )
                );

                sender.sendMessage(
                        color(
                                "&7Platform: &fPaper/Bukkit"
                        )
                );

                sender.sendMessage(
                        color(
                                "&8&m--------------------------"
                        )
                );
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(
            CommandSender sender
    ) {
        sender.sendMessage(
                color("&8&m--------------------------")
        );

        sender.sendMessage(
                color("&b&lDonutShop &7Administration")
        );

        sender.sendMessage(
                color("&f/donutshop reload &7- Reload configuration")
        );

        sender.sendMessage(
                color("&f/donutshop version &7- Show plugin version")
        );

        sender.sendMessage(
                color("&f/shopedit <category> &7- Edit a shop")
        );

        sender.sendMessage(
                color("&8&m--------------------------")
        );
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

        return List.of(
                        "reload",
                        "version"
                )
                .stream()
                .filter(value ->
                        value.startsWith(input)
                )
                .toList();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}
