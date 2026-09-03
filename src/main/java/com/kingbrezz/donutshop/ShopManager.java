package com.kingbrezz.donutshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ShopManager {

    private final DonutShop plugin;
    private final Economy economy;

    private File file;
    private FileConfiguration config;

    private final Map<String, ShopCategory> categories =
            new LinkedHashMap<>();

    public ShopManager(
            DonutShop plugin,
            Economy economy
    ) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void load() {
        categories.clear();

        file = new File(
                plugin.getDataFolder(),
                "shop.yml"
        );

        if (!file.exists()) {
            plugin.saveResource(
                    "shop.yml",
                    false
            );
        }

        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section =
                config.getConfigurationSection(
                        "categories"
                );

        if (section == null) {
            plugin.getLogger().warning(
                    "No shop categories found in shop.yml."
            );
            return;
        }

        for (String id : section.getKeys(false)) {

            String path =
                    "categories." + id;

            String name =
                    config.getString(
                            path + ".name",
                            id
                    );

            String iconName =
                    config.getString(
                            path + ".icon",
                            "CHEST"
                    );

            Material icon =
                    parseMaterial(
                            iconName,
                            Material.CHEST
                    );

            ShopCategory category =
                    new ShopCategory(
                            id,
                            name,
                            icon
                    );

            ConfigurationSection items =
                    config.getConfigurationSection(
                            path + ".items"
                    );

            if (items != null) {

                for (String itemId :
                        items.getKeys(false)) {

                    String itemPath =
                            path
                                    + ".items."
                                    + itemId;

                    Material material =
                            parseMaterial(
                                    config.getString(
                                            itemPath
                                                    + ".material",
                                            "STONE"
                                    ),
                                    Material.STONE
                            );

                    double buyPrice =
                            Math.max(
                                    0.0,
                                    config.getDouble(
                                            itemPath
                                                    + ".buy",
                                            0.0
                                    )
                            );

                    double sellPrice =
                            Math.max(
                                    0.0,
                                    config.getDouble(
                                            itemPath
                                                    + ".sell",
                                            0.0
                                    )
                            );

                    int slot =
                            config.getInt(
                                    itemPath + ".slot",
                                    -1
                            );

                    String displayName =
                            config.getString(
                                    itemPath + ".name",
                                    "&f" + itemId
                            );

                    var lore =
                            config.getStringList(
                                    itemPath + ".lore"
                            );

                    ShopItem shopItem =
                            new ShopItem(
                                    itemId,
                                    material,
                                    displayName,
                                    lore,
                                    buyPrice,
                                    sellPrice,
                                    slot
                            );

                    category.addItem(
                            shopItem
                    );
                }
            }

            categories.put(
                    id.toLowerCase(Locale.ROOT),
                    category
            );
        }

        plugin.getLogger().info(
                "Loaded "
                        + categories.size()
                        + " shop categories."
        );
    }

    public void save() {
        if (config == null || file == null) {
            return;
        }

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Failed to save shop.yml: "
                            + exception.getMessage()
            );
        }
    }

    public void setConfigValue(
            String path,
            Object value
    ) {
        if (config == null ||
                path == null ||
                path.isBlank()) {
            return;
        }

        config.set(path, value);
    }

    public void removeConfigSection(
            String path
    ) {
        if (config == null ||
                path == null ||
                path.isBlank()) {
            return;
        }

        config.set(path, null);
    }

    public boolean buy(
            Player player,
            ShopItem item,
            int amount
    ) {
        if (player == null ||
                item == null ||
                amount <= 0 ||
                !item.canBuy()) {
            return false;
        }

        double total =
                item.buyPrice() * amount;

        if (!Double.isFinite(total) ||
                total <= 0) {
            return false;
        }

        if (!economy.has(
                player,
                total
        )) {
            return false;
        }

        ItemStack stack =
                new ItemStack(
                        item.material(),
                        amount
                );

        HashMap<Integer, ItemStack> remaining =
                player.getInventory()
                        .addItem(stack);

        /*
         * Never withdraw money if the inventory
         * could not accept the complete purchase.
         */
        if (!remaining.isEmpty()) {

            /*
             * Roll back any items that were inserted
             * before Bukkit reported remaining items.
             */
            for (ItemStack leftover :
                    remaining.values()) {

                if (leftover != null &&
                        leftover.getType()
                                != Material.AIR) {

                    removeItems(
                            player,
                            leftover.getType(),
                            leftover.getAmount()
                    );
                }
            }

            return false;
        }

        economy.withdrawPlayer(
                player,
                total
        );

        return true;
    }

    public boolean sell(
            Player player,
            ShopItem item,
            int amount
    ) {
        if (player == null ||
                item == null ||
                amount <= 0 ||
                !item.canSell()) {
            return false;
        }

        int available =
                countItems(
                        player,
                        item.material()
                );

        if (available < amount) {
            return false;
        }

        removeItems(
                player,
                item.material(),
                amount
        );

        double total =
                item.sellPrice() * amount;

        if (!Double.isFinite(total) ||
                total <= 0) {
            return false;
        }

        economy.depositPlayer(
                player,
                total
        );

        return true;
    }

    private int countItems(
            Player player,
            Material material
    ) {
        int amount = 0;

        for (ItemStack stack :
                player.getInventory()
                        .getStorageContents()) {

            if (stack == null ||
                    stack.getType() != material) {
                continue;
            }

            amount += stack.getAmount();
        }

        return amount;
    }

    private void removeItems(
            Player player,
            Material material,
            int amount
    ) {
        int remaining = amount;

        for (
                int slot = 0;
                slot < player.getInventory()
                        .getSize()
                        && remaining > 0;
                slot++
        ) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(slot);

            if (stack == null ||
                    stack.getType() != material) {
                continue;
            }

            int remove =
                    Math.min(
                            remaining,
                            stack.getAmount()
                    );

            int newAmount =
                    stack.getAmount() - remove;

            if (newAmount <= 0) {
                player.getInventory()
                        .setItem(
                                slot,
                                null
                        );
            } else {
                stack.setAmount(
                        newAmount
                );
            }

            remaining -= remove;
        }
    }

    private Material parseMaterial(
            String name,
            Material fallback
    ) {
        if (name == null ||
                name.isBlank()) {
            return fallback;
        }

        try {
            return Material.valueOf(
                    name.toUpperCase(
                            Locale.ROOT
                    )
            );
        } catch (IllegalArgumentException exception) {

            plugin.getLogger().warning(
                    "Unknown material '"
                            + name
                            + "'. Using "
                            + fallback.name()
                            + "."
            );

            return fallback;
        }
    }

    public ShopCategory getCategory(
            String id
    ) {
        if (id == null) {
            return null;
        }

        return categories.get(
                id.toLowerCase(
                        Locale.ROOT
                )
        );
    }

    public Collection<ShopCategory> getCategories() {
        return Collections.unmodifiableCollection(
                categories.values()
        );
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public File getFile() {
        return file;
    }
                                        }
