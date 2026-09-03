package com.kingbrezz.donutshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Factory for all player-facing shop inventories. */
public final class ShopMenu {
    private static final String MAIN_ID = "__main__";

    private ShopMenu() {}

    public static final class Holder implements InventoryHolder {
        private final String category;
        private Inventory inventory;

        private Holder(String category) {
            this.category = category;
        }

        private void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        public String category() {
            return category;
        }

        public boolean isMain() {
            return MAIN_ID.equals(category);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static void openMain(DonutShop plugin, Player player) {
        int size = normalizeSize(plugin.getConfig().getInt("shop.main-menu.size", 27));
        Holder holder = new Holder(MAIN_ID);
        Inventory inventory = Bukkit.createInventory(holder, size,
                color(plugin.getConfig().getString("shop.main-menu.title", "&8&lDonutShop")));
        holder.bind(inventory);

        List<String> categories = plugin.getConfig().getStringList("shop.main-menu.categories");
        List<Integer> slots = plugin.getConfig().getIntegerList("shop.main-menu.category-slots");
        for (int index = 0; index < Math.min(categories.size(), slots.size()); index++) {
            ShopCategory category = plugin.getShopManager().getCategory(categories.get(index));
            int slot = slots.get(index);
            if (category != null && slot >= 0 && slot < size) {
                inventory.setItem(slot, createCategoryIcon(category));
            }
        }
        player.openInventory(inventory);
    }

    public static void openCategory(DonutShop plugin, Player player, ShopCategory category) {
        int size = normalizeSize(plugin.getConfig().getInt("shop.category-menu.size", 27));
        Holder holder = new Holder(category.id());
        Inventory inventory = Bukkit.createInventory(holder, size, color(category.name()));
        holder.bind(inventory);

        for (ShopItem item : category.items()) {
            if (item.slot() >= 0 && item.slot() < size) {
                inventory.setItem(item.slot(), display(plugin, item));
            }
        }

        int backSlot = plugin.getConfig().getInt("shop.category-menu.back-slot", 0);
        if (backSlot >= 0 && backSlot < size) {
            inventory.setItem(backSlot, createBackButton(plugin));
        }
        player.openInventory(inventory);
    }

    static ItemStack display(DonutShop plugin, ShopItem item) {
        ItemStack stack = new ItemStack(item.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.setDisplayName(color(item.displayName()));
        List<String> lore = new ArrayList<>();
        for (String line : item.lore()) {
            lore.add(color(line));
        }
        lore.add("");
        lore.add(color(plugin.getConfig().getString("shop.item-lore.buy", "&7Price: &a{price}")
                .replace("{price}", format(item.buyPrice()))));
        lore.add(color(plugin.getConfig().getString("shop.item-lore.click", "&eLeft-click &7to buy &8• &eShift-click &7for bulk")));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack createCategoryIcon(ShopCategory category) {
        ItemStack stack = new ItemStack(category.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(category.name()));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack createBackButton(DonutShop plugin) {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(plugin.getConfig().getString("shop.category-menu.back-name", "&c&lBack")));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static int normalizeSize(int size) {
        size = Math.max(9, Math.min(54, size));
        return size - size % 9;
    }

    static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private static String format(double value) {
        int decimals = 2;
        return String.format(Locale.US, "%." + decimals + "f", value);
    }
}
