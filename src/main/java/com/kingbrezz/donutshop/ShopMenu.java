package com.kingbrezz.donutshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.InventoryView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShopMenu {

    private static final String MAIN_PREFIX = "DONUTSHOP_MAIN";
    private static final String CATEGORY_PREFIX = "DONUTSHOP_CATEGORY";

    private static final Map<UUID, String> OPEN_CATEGORIES =
            new HashMap<>();

    private ShopMenu() {
    }

    public static void openMainMenu(Player player) {
        DonutShop plugin =
                (DonutShop) Bukkit.getPluginManager()
                        .getPlugin("DonutShop");

        if (plugin == null) {
            return;
        }

        openMainMenu(plugin, player);
    }

    public static void openMainMenu(
            DonutShop plugin,
            Player player
    ) {
        int size = plugin.getConfig()
                .getInt("shop.size", 27);

        size = normalizeSize(size);

        String title = color(
                plugin.getConfig().getString(
                        "shop.title",
                        "&8DonutShop"
                )
        );

        Inventory inventory = Bukkit.createInventory(
                null,
                size,
                title
        );

        int slot = 10;

        for (ShopCategory category :
                plugin.getShopManager().getCategories()) {

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

        player.openInventory(inventory);
        OPEN_CATEGORIES.remove(player.getUniqueId());
    }

    public static void openCategory(
            DonutShop plugin,
            Player player,
            ShopCategory category
    ) {
        if (category == null) {
            return;
        }

        int size = 27;

        Inventory inventory = Bukkit.createInventory(
                null,
                size,
                color(category.name())
        );

        for (ShopItem item : category.items()) {
            int slot = item.slot();

            if (slot < 0 || slot >= size) {
                continue;
            }

            inventory.setItem(
                    slot,
                    createShopItem(plugin, item)
            );
        }

        inventory.setItem(
                18,
                createBackItem(plugin)
        );

        player.openInventory(inventory);

        OPEN_CATEGORIES.put(
                player.getUniqueId(),
                category.id()
        );
    }

    private static ItemStack createCategoryItem(
            ShopCategory category
    ) {
        ItemStack item =
                new ItemStack(category.icon());

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                color(category.name())
        );

        meta.setLore(List.of(
                color("&7Click to open this category.")
        ));

        item.setItemMeta(meta);

        return item;
    }

    private static ItemStack createShopItem(
            DonutShop plugin,
            ShopItem shopItem
    ) {
        ItemStack item =
                new ItemStack(shopItem.material());

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                color(shopItem.displayName())
        );

        List<String> lore =
                new ArrayList<>(
                        shopItem.lore()
                );

        if (shopItem.canBuy()) {
            lore.add(
                    color("&aBuy: &f"
                            + formatPrice(
                            plugin,
                            shopItem.buyPrice()
                    ))
            );
        }

        if (shopItem.canSell()) {
            lore.add(
                    color("&cSell: &f"
                            + formatPrice(
                            plugin,
                            shopItem.sellPrice()
                    ))
            );
        }

        lore.add("");
        lore.add(
                color("&7Left Click &f→ Buy")
        );
        lore.add(
                color("&7Right Click &f→ Sell")
        );

        meta.setLore(lore);

        item.setItemMeta(meta);

        return item;
    }

    private static ItemStack createBackItem(
            DonutShop plugin
    ) {
        ItemStack item =
                new ItemStack(
                        Material.RED_STAINED_GLASS_PANE
                );

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                color("&cBack")
        );

        meta.setLore(List.of(
                color("&fClick to return")
        ));

        item.setItemMeta(meta);

        return item;
    }

    public static void handleClick(
            DonutShop plugin,
            Player player,
            int slot,
            ClickType click
    ) {
        String categoryId =
                OPEN_CATEGORIES.get(
                        player.getUniqueId()
                );

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
                        .getCategory(categoryId);

        if (category == null) {
            openMainMenu(plugin, player);
            return;
        }

        if (slot == 18) {
            openMainMenu(plugin, player);
            return;
        }

        ShopItem item =
                category.getItemBySlot(slot);

        if (item == null) {
            return;
        }

        int amount =
                click.isShiftClick()
                        ? plugin.getConfig().getInt(
                        "shop.shift-click-amount",
                        16
                )
                        : plugin.getConfig().getInt(
                        "shop.default-amount",
                        1
                );

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

        if (slot >= 10 && slot <= 16) {
            index = slot - 10;
        } else if (slot >= 19 && slot <= 25) {
            index = slot - 19 + 7;
        } else {
            return;
        }

        List<ShopCategory> categories =
                new ArrayList<>(
                        plugin.getShopManager()
                                .getCategories()
                );

        if (index < 0 || index >= categories.size()) {
            return;
        }

        openCategory(
                plugin,
                player,
                categories.get(index)
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

            playErrorSound(plugin, player);
            return;
        }

        double total =
                item.buyPrice() * amount;

        if (!plugin.getEconomy()
                .has(player, total)) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.not-enough-money"
                    );

            playErrorSound(plugin, player);
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

            playErrorSound(plugin, player);
            return;
        }

        if (!plugin.getShopManager()
                .buy(player, item, amount)) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.purchase-failed"
                    );

            playErrorSound(plugin, player);
            return;
        }

        plugin.getLanguageManager()
                .send(
                        player,
                        "messages.purchase-success",
                        Map.of(
                                "item",
                                color(item.displayName()),
                                "amount",
                                String.valueOf(amount),
                                "price",
                                formatPrice(
                                        plugin,
                                        total
                                )
                        )
                );

        playPurchaseSound(plugin, player);
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

            playErrorSound(plugin, player);
            return;
        }

        if (!plugin.getShopManager()
                .sell(player, item, amount)) {

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.not-enough-items"
                    );

            playErrorSound(plugin, player);
            return;
        }

        double total =
                item.sellPrice() * amount;

        plugin.getLanguageManager()
                .send(
                        player,
                        "messages.sell-success",
                        Map.of(
                                "item",
                                color(item.displayName()),
                                "amount",
                                String.valueOf(amount),
                                "price",
                                formatPrice(
                                        plugin,
                                        total
                                )
                        )
                );

        playPurchaseSound(plugin, player);
    }

    private static boolean hasInventorySpace(
            Player player,
            Material material,
            int amount
    ) {
        int remaining = amount;

        for (ItemStack stack :
                player.getInventory().getStorageContents()) {

            if (stack == null ||
                    stack.getType() == Material.AIR) {

                remaining -= material.getMaxStackSize();

            } else if (stack.getType() == material &&
                    stack.getAmount()
                            < material.getMaxStackSize()) {

                remaining -=
                        material.getMaxStackSize()
                                - stack.getAmount();
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
                .getBoolean("sounds.enabled", true)) {
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
                .getBoolean("sounds.enabled", true)) {
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

        if (soundName == null) {
            return;
        }

        try {
            Sound sound =
                    Sound.valueOf(
                            soundName.toUpperCase()
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

        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning(
                    "Invalid sound: " + soundName
            );
        }
    }

    private static String formatPrice(
            DonutShop plugin,
            double price
    ) {
        int decimals =
                plugin.getConfig()
                        .getInt(
                                "economy.decimals",
                                2
                        );

        StringBuilder pattern =
                new StringBuilder("0");

        if (decimals > 0) {
            pattern.append(".");

            pattern.append(
                    "0".repeat(decimals)
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

        return currency
                + format.format(price);
    }

    public static boolean isShopInventory(
            InventoryView view
    ) {
        if (view == null) {
            return false;
        }

        String title = view.getTitle();

        return title != null &&
                (
                        title.contains("DonutShop") ||
                        OPEN_CATEGORIES.containsValue(
                                findCategoryId(title)
                        )
                );
    }

    private static String findCategoryId(
            String title
    ) {
        for (String id :
                OPEN_CATEGORIES.values()) {

            if (id.equalsIgnoreCase(
                    ChatColor.stripColor(title)
            )) {
                return id;
            }
        }

        return "";
    }

    public static void removeSession(
            Player player
    ) {
        OPEN_CATEGORIES.remove(
                player.getUniqueId()
        );
    }

    private static int normalizeSize(int size) {
        if (size < 9) {
            return 9;
        }

        if (size > 54) {
            return 54;
        }

        return size - (size % 9);
    }

    private static String color(String text) {
        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
  }
