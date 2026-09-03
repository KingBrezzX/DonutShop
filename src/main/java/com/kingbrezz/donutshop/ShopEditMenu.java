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

    private static final String TITLE_PREFIX =
            "§0DonutShop Editor §8» ";

    private static final Map<UUID, EditSession> SESSIONS =
            new HashMap<>();

    private ShopEditMenu() {
    }

    public static void open(
            DonutShop plugin,
            Player player,
            ShopCategory category
    ) {
        if (category == null) {
            return;
        }

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        TITLE_PREFIX
                                + color(
                                category.name()
                        )
                );

        for (ShopItem item :
                category.items()) {

            int slot = item.slot();

            if (slot < 0 || slot >= 27) {
                continue;
            }

            inventory.setItem(
                    slot,
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
                new EditSession(
                        category.id()
                )
        );

        player.openInventory(
                inventory
        );
    }

    public static boolean isEditorInventory(
            Player player
    ) {
        return player != null &&
                SESSIONS.containsKey(
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
                SESSIONS.get(
                        player.getUniqueId()
                );

        if (session == null) {
            return;
        }

        ShopCategory category =
                plugin.getShopManager()
                        .getCategory(
                                session.categoryId()
                        );

        if (category == null) {
            close(player);
            return;
        }

        /*
         * Back button.
         */
        if (slot == 18) {
            close(player);
            ShopMenu.openMainMenu(
                    plugin,
                    player
            );
            return;
        }

        /*
         * Information button.
         */
        if (slot == 26) {
            return;
        }

        /*
         * Reserved GUI slots.
         */
        if (slot < 0 ||
                slot >= 18) {
            return;
        }

        ItemStack cursor =
                player.getItemOnCursor();

        /*
         * Cursor contains an item:
         * create / replace shop item.
         */
        if (cursor != null &&
                cursor.getType() != Material.AIR) {

            ShopItem newItem =
                    createFromCursor(
                            cursor,
                            slot
                    );

            if (newItem == null) {
                plugin.getLanguageManager()
                        .send(
                                player,
                                "messages.shopedit-invalid-item"
                        );
                return;
            }

            ShopItem oldItem =
                    category.getItemBySlot(
                            slot
                    );

            if (oldItem != null &&
                    !oldItem.id().equalsIgnoreCase(
                            newItem.id()
                    )) {

                removeConfigItem(
                        plugin,
                        category,
                        oldItem
                );

                category.removeItem(
                        oldItem.id()
                );
            }

            saveItem(
                    plugin,
                    category,
                    newItem
            );

            player.setItemOnCursor(
                    null
            );

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
         * Empty cursor + occupied slot:
         * remove the shop item.
         */
        ShopItem existing =
                category.getItemBySlot(
                        slot
                );

        if (existing == null) {
            return;
        }

        removeConfigItem(
                plugin,
                category,
                existing
        );

        category.removeItem(
                existing.id()
        );

        if (plugin.getConfig()
                .getBoolean(
                        "shop-edit.auto-save",
                        true
                )) {

            plugin.getShopManager()
                    .save();
        }

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

    private static ShopItem createFromCursor(
            ItemStack cursor,
            int slot
    ) {
        if (cursor == null ||
                cursor.getType() == Material.AIR) {
            return null;
        }

        Material material =
                cursor.getType();

        ItemMeta meta =
                cursor.getItemMeta();

        String displayName;

        if (meta != null &&
                meta.hasDisplayName()) {

            displayName =
                    meta.getDisplayName();

        } else {

            displayName =
                    "&f"
                            + material.name()
                            .replace(
                                    '_',
                                    ' '
                            );
        }

        List<String> lore =
                new ArrayList<>();

        if (meta != null &&
                meta.hasLore() &&
                meta.getLore() != null) {

            lore.addAll(
                    meta.getLore()
            );
        }

        double price =
                extractPrice(
                        displayName
                );

        /*
         * Original ShopEdit behavior:
         * [PRICE] 250 sets an explicit price.
         */
        if (price <= 0) {
            price = 1.0;
        }

        String itemId =
                material.name()
                        .toLowerCase()
                        .replace(
                                '_',
                                '-'
                        );

        return new ShopItem(
                itemId,
                material,
                displayName,
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
                ChatColor.stripColor(
                        text
                ).trim();

        if (!stripped
                .toUpperCase()
                .startsWith("[PRICE]")) {
            return 0.0;
        }

        String value =
                stripped.substring(
                        "[PRICE]".length()
                ).trim();

        try {
            double price =
                    Double.parseDouble(
                            value
                    );

            if (!Double.isFinite(price) ||
                    price <= 0) {
                return 0.0;
            }

            return price;

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

        ShopManager manager =
                plugin.getShopManager();

        manager.setConfigValue(
                base + ".material",
                item.material().name()
        );

        manager.setConfigValue(
                base + ".name",
                item.displayName()
        );

        manager.setConfigValue(
                base + ".lore",
                item.lore()
        );

        manager.setConfigValue(
                base + ".buy",
                item.buyPrice()
        );

        manager.setConfigValue(
                base + ".sell",
                item.sellPrice()
        );

        manager.setConfigValue(
                base + ".slot",
                item.slot()
        );

        if (plugin.getConfig()
                .getBoolean(
                        "shop-edit.auto-save",
                        true
                )) {

            manager.save();
        }
    }

    private static void removeConfigItem(
            DonutShop plugin,
            ShopCategory category,
            ShopItem item
    ) {
        String path =
                "categories."
                        + category.id()
                        + ".items."
                        + item.id();

        plugin.getShopManager()
                .removeConfigSection(
                        path
                );
    }

    private static void reopen(
            DonutShop plugin,
            Player player,
            ShopCategory category
    ) {
        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            if (!player.isOnline()) {
                                return;
                            }

                            open(
                                    plugin,
                                    player,
                                    category
                            );
                        }
                );
    }

    private static ItemStack createEditorItem(
            ShopItem item
    ) {
        ItemStack stack =
                new ItemStack(
                        item.material()
                );

        ItemMeta meta =
                stack.getItemMeta();

        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(
                color(
                        item.displayName()
                )
        );

        List<String> lore =
                new ArrayList<>(
                        item.lore()
                );

        lore.add("");
        lore.add(
                color(
                        "&7Buy: &f"
                                + item.buyPrice()
                )
        );
        lore.add(
                color(
                        "&7Sell: &f"
                                + item.sellPrice()
                )
        );
        lore.add("");
        lore.add(
                color(
                        "&eShop Editor Item"
                )
        );

        meta.setLore(
                lore
        );

        stack.setItemMeta(
                meta
        );

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

        meta.setLore(
                List.of(
                        color(
                                "&7Return to main menu."
                        )
                )
        );

        item.setItemMeta(
                meta
        );

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
                color(
                        "&bShopEdit Help"
                )
        );

        meta.setLore(
                List.of(
                        color(
                                "&7Select an item from your inventory."
                        ),
                        color(
                                "&7Then place it into a shop slot."
                        ),
                        "",
                        color(
                                "&7Custom price format:"
                        ),
                        color(
                                "&f[PRICE] 250"
                        ),
                        "",
                        color(
                                "&7Empty cursor + filled slot:"
                        ),
                        color(
                                "&cremoves the item"
                        )
                )
        );

        item.setItemMeta(
                meta
        );

        return item;
    }

    public static void close(
            Player player
    ) {
        if (player == null) {
            return;
        }

        SESSIONS.remove(
                player.getUniqueId()
        );
    }

    public static void removeSession(
            Player player
    ) {
        close(player);
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

    private record EditSession(
            String categoryId
    ) {
    }
        }
