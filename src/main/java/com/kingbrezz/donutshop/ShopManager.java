package com.kingbrezz.donutshop;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
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

    private static final int MAX_TRANSACTION_AMOUNT = 2304;

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

        config =
                YamlConfiguration.loadConfiguration(file);

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

        for (String id :
                section.getKeys(false)) {

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
                            sanitizePrice(
                                    config.getDouble(
                                            itemPath
                                                    + ".buy",
                                            0.0
                                    )
                            );

                    double sellPrice =
                            sanitizePrice(
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
                    id.toLowerCase(
                            Locale.ROOT
                    ),
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
        if (config == null ||
                file == null) {
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

        config.set(
                path,
                value
        );
    }

    public void removeConfigSection(
            String path
    ) {
        if (config == null ||
                path == null ||
                path.isBlank()) {
            return;
        }

        config.set(
                path,
                null
        );
    }

    public boolean buy(
            Player player,
            ShopItem item,
            int amount
    ) {
        if (player == null ||
                !player.isOnline() ||
                item == null ||
                economy == null) {
            return false;
        }

        if (!item.canBuy() ||
                !isValidAmount(amount)) {
            return false;
        }

        double total =
                calculateTotal(
                        item.buyPrice(),
                        amount
                );

        if (!isValidMoney(total)) {
            return false;
        }

        /*
         * Re-check the balance immediately before
         * performing the transaction.
         */
        if (!economy.has(
                player,
                total
        )) {
            return false;
        }

        /*
         * Build and insert the items first.
         *
         * If Bukkit cannot insert everything, nothing
         * is withdrawn.
         */
        if (!addItems(
                player,
                item.material(),
                amount
        )) {
            return false;
        }

        EconomyResponse response =
                economy.withdrawPlayer(
                        player,
                        total
                );

        /*
         * Economy transaction failed after items were
         * inserted. Roll the items back.
         */
        if (response == null ||
                !response.transactionSuccess()) {

            removeItems(
                    player,
                    item.material(),
                    amount
            );

            return false;
        }

        return true;
    }

    public boolean sell(
            Player player,
            ShopItem item,
            int amount
    ) {
        if (player == null ||
                !player.isOnline() ||
                item == null ||
                economy == null) {
            return false;
        }

        if (!item.canSell() ||
                !isValidAmount(amount)) {
            return false;
        }

        double total =
                calculateTotal(
                        item.sellPrice(),
                        amount
                );

        if (!isValidMoney(total)) {
            return false;
        }

        /*
         * Make sure the player owns enough items
         * before touching the inventory.
         */
        if (countItems(
                player,
                item.material()
        ) < amount) {
            return false;
        }

        /*
         * Remove the items first.
         */
        if (!removeItems(
                player,
                item.material(),
                amount
        )) {
            return false;
        }

        EconomyResponse response =
                economy.depositPlayer(
                        player,
                        total
                );

        /*
         * If the economy provider rejects the deposit,
         * restore the exact amount of material.
         */
        if (response == null ||
                !response.transactionSuccess()) {

            if (!addItems(
                    player,
                    item.material(),
                    amount
            )) {
                plugin.getLogger().severe(
                        "CRITICAL: Failed to rollback "
                                + amount
                                + "x "
                                + item.material().name()
                                + " for "
                                + player.getName()
                );
            }

            return false;
        }

        return true;
    }

    private boolean addItems(
            Player player,
            Material material,
            int amount
    ) {
        if (player == null ||
                material == null ||
                amount <= 0) {
            return false;
        }

        int remaining = amount;

        while (remaining > 0) {

            int stackAmount =
                    Math.min(
                            remaining,
                            material.getMaxStackSize()
                    );

            ItemStack stack =
                    new ItemStack(
                            material,
                            stackAmount
                    );

            HashMap<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(stack);

            if (!leftovers.isEmpty()) {

                /*
                 * Roll back everything that was inserted
                 * during this transaction.
                 */
                int inserted =
                        amount - remaining;

                if (inserted > 0) {
                    removeItems(
                            player,
                            material,
                            inserted
                    );
                }

                return false;
            }

            remaining -= stackAmount;
        }

        return true;
    }

    private int countItems(
            Player player,
            Material material
    ) {
        if (player == null ||
                material == null) {
            return 0;
        }

        long amount = 0;

        for (ItemStack stack :
                player.getInventory()
                        .getStorageContents()) {

            if (stack == null ||
                    stack.getType() != material) {
                continue;
            }

            amount += stack.getAmount();

            if (amount >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }

        return (int) amount;
    }

    private boolean removeItems(
            Player player,
            Material material,
            int amount
    ) {
        if (player == null ||
                material == null ||
                amount <= 0) {
            return false;
        }

        if (countItems(
                player,
                material
        ) < amount) {
            return false;
        }

        int remaining = amount;

        for (
                int slot = 0;
                slot < player.getInventory().getSize()
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
                    stack.getAmount()
                            - remove;

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

                player.getInventory()
                        .setItem(
                                slot,
                                stack
                        );
            }

            remaining -= remove;
        }

        return remaining == 0;
    }

    private double calculateTotal(
            double price,
            int amount
    ) {
        if (!Double.isFinite(price) ||
                price <= 0 ||
                !isValidAmount(amount)) {
            return -1;
        }

        double total =
                price * amount;

        if (!Double.isFinite(total) ||
                total <= 0) {
            return -1;
        }

        return total;
    }

    private boolean isValidAmount(
            int amount
    ) {
        return amount > 0 &&
                amount <= MAX_TRANSACTION_AMOUNT;
    }

    private double sanitizePrice(
            double price
    ) {
        if (!Double.isFinite(price) ||
                price < 0) {
            return 0.0;
        }

        return price;
    }

    private boolean isValidMoney(
            double amount
    ) {
        return Double.isFinite(amount) &&
                amount > 0;
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
