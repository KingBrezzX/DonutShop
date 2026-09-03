package com.kingbrezz.donutshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ShopMenu {

    private static final String MAIN_TITLE =
            "&8DonutShop";

    private static final String CATEGORY_PREFIX =
            "DONUTSHOP_CATEGORY:";

    private static final Map<UUID, String> OPEN_CATEGORIES =
            new HashMap<>();

    private ShopMenu() {
    }

    public static void openMainMenu(
            DonutShop plugin,
            Player player
    ) {
        if (plugin == null || player == null ||
                !player.isOnline()) {
            return;
        }

        int size = normalizeSize(
                plugin.getConfig()
                        .getInt(
                                "shop.size",
                                27
                        )
        );

        String title = color(
                plugin.getConfig()
                        .getString(
                                "shop.title",
                                MAIN_TITLE
                        )
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        size,
                        title
                );

        int slot = 10;

        for (ShopCategory category :
                plugin.getShopManager()
                        .getCategories()) {

            if (slot >= size) {
                break;
            }

            inventory.setItem(
                    slot,
                    createCategoryItem(
                            category
                    )
            );

            slot++;

            if (slot == 17) {
                slot = 19;
            }
        }

        OPEN_CATEGORIES.remove(
                player.getUniqueId()
        );

        player.openInventory(
                inventory
        );
    }

    public static void openCategory(
            DonutShop plugin,
            Player player,
            ShopCategory category
    ) {
        if (plugin == null ||
                player == null ||
                category == null ||
                !player.isOnline()) {
            return;
        }

        final int size = 27;

        String title =
                CATEGORY_PREFIX
                        + category.id();

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        size,
                        title
                );

        for (ShopItem item :
                category.items()) {

            if (item == null ||
                    !item.hasValidSlot()) {
                continue;
            }

            int slot = item.slot();

            if (slot < 0 ||
                    slot >= size) {
                continue;
            }

            inventory.setItem(
                    slot,
                    createShopItem(
                            plugin,
                            item
                    )
            );
        }

        inventory.setItem(
                18,
                createBackItem()
        );

        OPEN_CATEGORIES.put(
                player.getUniqueId(),
                category.id()
        );

        player.openInventory(
                inventory
        );
    }

    private static ItemStack createCategoryItem(
            ShopCategory category
    ) {
        ItemStack item =
                new ItemStack(
                        category.icon()
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                color(category.name())
        );

        meta.setLore(
                List.of(
                        color(
                                "&7Click to open this category."
                        )
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    private static ItemStack createShopItem(
            DonutShop plugin,
            ShopItem shopItem
    ) {
        ItemStack item =
                new ItemStack(
                        shopItem.material()
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                color(
                        shopItem.displayName()
                )
        );

        List<String> lore =
                new ArrayList<>(
                        shopItem.lore()
                );

        if (shopItem.canBuy()) {
            lore.add(
                    color(
                            "&aBuy: &f"
                                    + formatPrice(
                                    plugin,
                                    shopItem.buyPrice()
                            )
                    )
            );
        }

        if (shopItem.canSell()) {
            lore.add(
                    color(
                            "&cSell: &f"
                                    + formatPrice(
                                    plugin,
                                    shopItem.sellPrice()
                            )
                    )
            );
        }

        lore.add("");

        if (shopItem.canBuy()) {
            lore.add(
                    color(
                            "&7Left Click &f→ Buy"
                    )
            );
        }

        if (shopItem.canSell()) {
            lore.add(
                    color(
                            "&7Right Click &f→ Sell"
                    )
            );

        }

        meta.setLore(lore);

        item.setItemMeta(meta);

        return item;
    }

    private static ItemStack createBackItem() {
        ItemStack item =
                new ItemStack(
                        Material.RED_STAINED_GLASS_PANE
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                color("&cBack")
        );

        meta.setLore(
                List.of(
                        color(
                                "&fClick to return"
                        )
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    public static void handleClick(
            DonutShop plugin,
            Player player,
            int slot,
            ClickType click
    ) {
        if (plugin == null ||
                player == null ||
                click == null) {
            return;
        }

        String categoryId =
                OPEN_CATEGORIES.get(
                        player.getUniqueId()
                );

        /*
         * Main category menu.
         */
        if (categoryId == null) {
            handleMainMenuClick(
                    plugin,
                    player,
                    slot
            );
            return;
        }

        ShopCategory category =
                plugin.getShopManager()
                        .getCategory(
                                categoryId
                        );

        if (category == null) {
            openMainMenu(
                    plugin,
                    player
            );
            return;
        }

        /*
         * Back button.
         */
        if (slot == 18) {
            openMainMenu(
                    plugin,
                    player
            );
            return;
        }

        ShopItem item =
                category.getItemBySlot(
                        slot
                );

        if (item == null) {
            return;
        }

        int amount =
                getTransactionAmount(
                        plugin,
                        click
                );

        if (amount <= 0) {
            return;
        }

        if (click.isLeftClick()) {

            buy(
                    plugin,
                    player,
                    item,
                    amount
            );

        } else if (click.isRightClick()) {

            sell(
                    plugin,
                    player,
                    item,
                    amount
            );
        }
    }

    private static void handleMainMenuClick(
            DonutShop plugin,
            Player player,
            int slot
    ) {
        int index;

        if (slot >= 10 &&
                slot <= 16) {

            index = slot - 10;

        } else if (slot >= 19 &&
                slot <= 25) {

            index = slot - 19 + 7;

        } else {
            return;
        }

        List<ShopCategory> categories =
                new ArrayList<>(
                        plugin.getShopManager()
                                .getCategories()
                );

        if (index < 0 ||
                index >= categories.size()) {
            return;
        }

        openCategory(
                plugin,
                player,
                categories.get(index)
        );
    }

    private static int getTransactionAmount(
            DonutShop plugin,
            ClickType click
    ) {
        int configured;

        if (click.isShiftClick()) {

            configured =
                    plugin.getConfig()
                            .getInt(
                                    "shop.shift-click-amount",
                                    16
                            );

        } else {

            configured =
                    plugin.getConfig()
                            .getInt(
                                    "shop.default-amount",
                                    1
                            );
        }

        return Math.max(
                1,
                Math.min(
                        configured,
                        2304
                )
        );
    }

    private static void buy(
            DonutShop plugin,
            Player player,
            ShopItem item,
            int amount
    ) {
        if (!item.canBuy()) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.cannot-buy"
                    );

            playErrorSound(
                    plugin,
                    player
            );

            return;
        }

        double total =
                item.buyPrice()
                        * amount;

        if (!Double.isFinite(total) ||
                total <= 0) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.purchase-failed"
                    );

            playErrorSound(
                    plugin,
                    player
            );

            return;
        }

        if (!plugin.getEconomy()
                .has(
                        player,
                        total
                )) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.not-enough-money"
                    );

            playErrorSound(
                    plugin,
                    player
            );

            return;
        }

        if (!hasInventorySpace(
                player,
                item.material(),
                amount
        )) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.inventory-full"
                    );

            playErrorSound(
                    plugin,
                    player
            );

            return;
        }

        if (!plugin.getShopManager()
                .buy(
                        player,
                        item,
                        amount
                )) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.purchase-failed"
                    );

            playErrorSound(
                    plugin,
                    player
            );

            return;
        }

        plugin.getLanguageManager()
                .send(
                        player,
                        "messages.purchase-success",
                        Map.of(
                                "item",
                                color(
                                        item.displayName()
                                ),
                                "amount",
                                String.valueOf(
                                        amount
                                ),
                                "price",
                                formatPrice(
                                        plugin,
                                        total
                                )
                        )
                );

        playPurchaseSound(
                plugin,
                player
        );
    }

    private static void sell(
            DonutShop plugin,
            Player player,
            ShopItem item,
            int amount
    ) {
        if (!item.canSell()) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.cannot-sell"
                    );

            playErrorSound(
                    plugin,
                    player
            );

            return;
        }

        double total =
                item.sellPrice()
                        * amount;

        if (!Double.isFinite(total) ||
                total <= 0) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.purchase-failed"
                    );

            playErrorSound(
                    plugin,
                    player
            );

            return;
        }

        if (!plugin.getShopManager()
                .sell(
                        player,
                        item,
                        amount
                )) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.not-enough-items"
                    );

            playErrorSound(
                    plugin,
                    player
            );

            return;
        }

        plugin.getLanguageManager()
                .send(
                        player,
                        "messages.sell-success",
                        Map.of(
                                "item",
                                color(
                                        item.displayName()
                                ),
                                "amount",
                                String.valueOf(
                                        amount
                                ),
                                "price",
                                formatPrice(
                                        plugin,
                                        total
                                )
                        )
                );

        playPurchaseSound(
                plugin,
                player
        );
    }

    private static boolean hasInventorySpace(
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
        int maxStack =
                Math.max(
                        1,
                        material.getMaxStackSize()
                );

        for (ItemStack stack :
                player.getInventory()
                        .getStorageContents()) {

            if (stack == null ||
                    stack.getType() == Material.AIR) {

                remaining -= maxStack;

            } else if (stack.getType() ==
                    material) {

                remaining -=
                        Math.max(
                                0,
                                maxStack
                                        - stack.getAmount()
                        );
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private static void playPurchaseSound(
            DonutShop plugin,
            Player player
    ) {
        if (!plugin.getConfig()
                .getBoolean(
                        "sounds.enabled",
                        true
                )) {
            return;
        }

        playSound(
                plugin,
                player,
                "sounds.purchase"
        );
    }

    private static void playErrorSound(
            DonutShop plugin,
            Player player
    ) {
        if (!plugin.getConfig()
                .getBoolean(
                        "sounds.enabled",
                        true
                )) {
            return;
        }

        playSound(
                plugin,
                player,
                "sounds.error"
        );
    }

    private static void playSound(
            DonutShop plugin,
            Player player,
            String path
    ) {
        String soundName =
                plugin.getConfig()
                        .getString(
                                path + ".sound"
                        );

        if (soundName == null ||
                soundName.isBlank()) {
            return;
        }

        try {
            Sound sound =
                    Sound.valueOf(
                            soundName.toUpperCase(
                                    Locale.ROOT
                            )
                    );

            float volume =
                    (float) plugin.getConfig()
                            .getDouble(
                                    path + ".volume",
                                    1.0
                            );

            float pitch =
                    (float) plugin.getConfig()
                            .getDouble(
                                    path + ".pitch",
                                    1.0
                            );

            player.playSound(
                    player.getLocation(),
                    sound,
                    volume,
                    pitch
            );

        } catch (IllegalArgumentException exception) {

            plugin.getLogger().warning(
                    "Invalid sound '"
                            + soundName
                            + "' in "
                            + path
            );
        }
    }

    private static String formatPrice(
            DonutShop plugin,
            double price
    ) {
        int decimals =
                Math.max(
                        0,
                        Math.min(
                                6,
                                plugin.getConfig()
                                        .getInt(
                                                "economy.decimals",
                                                2
                                        )
                        )
                );

        StringBuilder pattern =
                new StringBuilder("0");

        if (decimals > 0) {

            pattern.append(".");

            pattern.append(
                    "0".repeat(
                            decimals
                    )
            );
        }

        DecimalFormat format =
                new DecimalFormat(
                        pattern.toString()
                );

        String currency =
                plugin.getConfig()
                        .getString(
                                "economy.currency-name",
                                "$"
                        );

        if (currency == null) {
            currency = "$";
        }

        return currency
                + format.format(price);
    }

    public static boolean isShopInventory(
            InventoryView view
    ) {
        if (view == null) {
            return false;
        }

        String title =
                view.getTitle();

        if (title == null) {
            return false;
        }

        String stripped =
                ChatColor.stripColor(
                        title
                );

        if (stripped == null) {
            return false;
        }

        if (stripped.equalsIgnoreCase(
                "DonutShop"
        )) {
            return true;
        }

        return stripped.startsWith(
                CATEGORY_PREFIX
        );
    }

    public static void removeSession(
            Player player
    ) {
        if (player == null) {
            return;
        }

        OPEN_CATEGORIES.remove(
                player.getUniqueId()
        );
    }

    private static int normalizeSize(
            int size
    ) {
        if (size < 9) {
            return 9;
        }

        if (size > 54) {
            return 54;
        }

        return size -
                (size % 9);
    }

    private static String color(
            String text
    ) {
        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
            }
