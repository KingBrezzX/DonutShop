package com.kingbrezz.donutshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShopEditMenu {

    private static final Map<UUID, EditSession> SESSIONS =
            new HashMap<>();

    private static final String PREFIX = "§0DonutShop Editor";

    private ShopEditMenu() {
    }

    public static void open(
            DonutShop plugin,
            Player player,
            ShopCategory category
    ) {
        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                PREFIX + " §8» " + color(category.name())
        );

        for (ShopItem item : category.items()) {
            if (item.slot() < 0 || item.slot() >= 27) {
                continue;
            }

            inventory.setItem(
                    item.slot(),
                    createEditorItem(item)
            );
        }

        inventory.setItem(
                18,
                createBackItem()
        );

        inventory.setItem(
                26,
                createInfoItem()
        );

        SESSIONS.put(
                player.getUniqueId(),
                new EditSession(category.id())
        );

        player.openInventory(inventory);
    }

    public static boolean isEditorInventory(
            Player player
    ) {
        return SESSIONS.containsKey(
                player.getUniqueId()
        );
    }

    public static void handleClick(
            DonutShop plugin,
            Player player,
            int slot,
            ClickType click
    ) {
        EditSession session =
                SESSIONS.get(player.getUniqueId());

        if (session == null) {
            return;
        }

        ShopCategory category =
                plugin.getShopManager()
                        .getCategory(session.categoryId());

        if (category == null) {
            close(player);
            return;
        }

        if (slot == 18) {
            close(player);
            ShopMenu.openMainMenu(plugin, player);
            return;
        }

        if (slot == 26) {
            return;
        }

        if (slot < 0 || slot >= 27) {
            return;
        }

        ItemStack cursor =
                player.getItemOnCursor();

        /*
         * Player has selected an item from their cursor.
         * Place or replace the shop item.
         */
        if (cursor != null &&
                cursor.getType() != Material.AIR) {

            ShopItem created =
                    createShopItemFromCursor(
                            plugin,
                            category,
                            slot,
                            cursor
                    );

            if (created == null) {
                plugin.getLanguageManager()
                        .send(
                                player,
                                "messages.shopedit-invalid-item"
                        );
                return;
            }

            saveItem(
                    plugin,
                    category,
                    created
            );

            player.setItemOnCursor(null);

            reopen(
                    plugin,
                    player,
                    category
            );

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.shopedit-item-saved"
                    );

            return;
        }

        /*
         * Empty cursor + existing slot = remove item.
         */
        ShopItem existing =
                category.getItemBySlot(slot);

        if (existing != null) {

            removeItem(
                    plugin,
                    category,
                    existing
            );

            reopen(
                    plugin,
                    player,
                    category
            );

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.shopedit-item-removed"
                    );
        }
    }

    private static ShopItem createShopItemFromCursor(
            DonutShop plugin,
            ShopCategory category,
            int slot,
            ItemStack cursor
    ) {
        Material material = cursor.getType();

        ItemMeta meta = cursor.getItemMeta();

        String name;

        if (meta != null &&
                meta.hasDisplayName()) {
            name = meta.getDisplayName();
        } else {
            name = "&f" +
                    material.name()
                            .replace('_', ' ');
        }

        List<String> lore = new ArrayList<>();

        if (meta != null &&
                meta.hasLore()) {
            lore.addAll(
                    meta.getLore()
            );
        }

        double price =
                extractPrice(name);

        if (price <= 0) {
            price = 1.0;
        }

        String itemId =
                material.name()
                        .toLowerCase()
                        .replace('_', '-');

        return new ShopItem(
                itemId,
                material,
                name,
                lore,
                price,
                0.0,
                slot
        );
    }

    private static double extractPrice(
            String text
    ) {
        if (text == null) {
            return 0.0;
        }

        String stripped =
                ChatColor.stripColor(text)
                        .trim();

        if (!stripped.startsWith("[PRICE]")) {
            return 0.0;
        }

        String value =
                stripped.substring(
                        "[PRICE]".length()
                ).trim();

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static void saveItem(
            DonutShop plugin,
            ShopCategory category,
            ShopItem item
    ) {
        category.addItem(item);

        String base =
                "categories."
                        + category.id()
                        + ".items."
                        + item.id();

        plugin.getShopManager()
                .setConfigValue(
                        base + ".material",
                        item.material().name()
                );

        plugin.getShopManager()
                .setConfigValue(
                        base + ".name",
                        item.displayName()
                );

        plugin.getShopManager()
                .setConfigValue(
                        base + ".lore",
                        item.lore()
                );

        plugin.getShopManager()
                .setConfigValue(
                        base + ".buy",
                        item.buyPrice()
                );

        plugin.getShopManager()
                .setConfigValue(
                        base + ".sell",
                        item.sellPrice()
                );

        plugin.getShopManager()
                .setConfigValue(
                        base + ".slot",
                        item.slot()
                );

        if (plugin.getConfig().getBoolean(
                "shop-edit.auto-save",
                true
        )) {
            plugin.getShopManager().save();
        }
    }

    private static void removeItem(
            DonutShop plugin,
            ShopCategory category,
            ShopItem item
    ) {
        category.removeItem(item.id());

        String path =
                "categories."
                        + category.id()
                        + ".items."
                        + item.id();

        plugin.getShopManager()
                .removeConfigSection(path);

        if (plugin.getConfig().getBoolean(
                "shop-edit.auto-save",
                true
        )) {
            plugin.getShopManager().save();
        }
    }

    private static void reopen(
            DonutShop plugin,
            Player player,
            ShopCategory category
    ) {
        Bukkit.getScheduler().runTask(
                plugin,
                () -> open(
                        plugin,
                        player,
                        category
                )
        );
    }

    private static ItemStack createEditorItem(
            ShopItem item
    ) {
        ItemStack stack =
                new ItemStack(item.material());

        ItemMeta meta =
                stack.getItemMeta();

        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(
                color(item.displayName())
        );

        List<String> lore =
                new ArrayList<>(
                        item.lore()
                );

        lore.add("");
        lore.add(
                color("&7Buy: &f" + item.buyPrice())
        );
        lore.add(
                color("&7Sell: &f" + item.sellPrice())
        );
        lore.add("");
        lore.add(
                color("&eEditor Item")
        );

        meta.setLore(lore);

        stack.setItemMeta(meta);

        return stack;
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

        meta.setLore(List.of(
                color("&7Return to main menu.")
        ));

        item.setItemMeta(meta);

        return item;
    }

    private static ItemStack createInfoItem() {
        ItemStack item =
                new ItemStack(
                        Material.KNOWLEDGE_BOOK
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                color("&bShopEdit Help")
        );

        meta.setLore(List.of(
                color("&7Select an item from your"),
                color("&7inventory and place it here."),
                "",
                color("&7Use display name:"),
                color("&f[PRICE] 250"),
                "",
                color("&7Empty slot + empty cursor"),
                color("&7removes the shop item.")
        ));

        item.setItemMeta(meta);

        return item;
    }

    public static void close(Player player) {
        SESSIONS.remove(
                player.getUniqueId()
        );
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

    private record EditSession(
            String categoryId
    ) {
    }
}
