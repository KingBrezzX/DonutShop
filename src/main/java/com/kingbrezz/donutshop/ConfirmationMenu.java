package com.kingbrezz.donutshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;

/** Quantity confirmation GUI shown before a purchase is committed. */
public final class ConfirmationMenu {
    public static final String TITLE_DEFAULT = "&8Confirmation Menu";
    private static final int SIZE = 27;

    // Layout mirrors the reference UI: red controls on the left, selected item
    // in the center, and green quantity controls on the right.
    private static final int MIN_64 = 9;
    private static final int MIN_10 = 10;
    private static final int MIN_1 = 11;
    private static final int RESET = 12;
    private static final int CONFIRM = 13;
    private static final int PLUS_1 = 15;
    private static final int PLUS_10 = 16;
    private static final int PLUS_64 = 17;

    private ConfirmationMenu() {}

    public static final class Holder implements InventoryHolder {
        private final ShopItem item;
        private final ShopCategory category;
        private int quantity;
        private Inventory inventory;

        private Holder(ShopCategory category, ShopItem item, int quantity) {
            this.category = category;
            this.item = item;
            this.quantity = quantity;
        }

        private void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        public ShopItem item() {
            return item;
        }

        public ShopCategory category() {
            return category;
        }

        public int quantity() {
            return quantity;
        }

        public void quantity(int quantity) {
            this.quantity = quantity;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static void open(DonutShop plugin, Player player, ShopCategory category, ShopItem item, int quantity) {
        int max = maxPurchaseAmount(plugin);
        int safeQuantity = Math.max(1, Math.min(quantity, max));
        Holder holder = new Holder(category, item, safeQuantity);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, ShopMenu.component(
                plugin.getConfig().getString("shop.confirmation.title", TITLE_DEFAULT)));
        holder.bind(inventory);
        render(plugin, holder);
        player.openInventory(inventory);
    }

    public static void render(DonutShop plugin, Holder holder) {
        Inventory inventory = holder.getInventory();
        if (inventory == null) return;
        inventory.clear();

        Material filler = material(plugin, "shop.confirmation.filler", Material.GRAY_STAINED_GLASS_PANE);
        ItemStack fillerStack = button(filler, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, fillerStack.clone());
        }

        inventory.setItem(MIN_64, button(
                material(plugin, "shop.confirmation.decrease-material", Material.RED_STAINED_GLASS_PANE),
                plugin.getConfig().getString("shop.confirmation.decrease-64-name", "&cRemove 64"),
                lore(plugin, "shop.confirmation.decrease-lore", "&7Current quantity: &f{quantity}", "&eClick to adjust the quantity"),
                holder.quantity()));
        inventory.setItem(MIN_10, button(
                material(plugin, "shop.confirmation.decrease-material", Material.RED_STAINED_GLASS_PANE),
                plugin.getConfig().getString("shop.confirmation.decrease-10-name", "&cRemove 10"),
                lore(plugin, "shop.confirmation.decrease-lore", "&7Current quantity: &f{quantity}", "&eClick to adjust the quantity"),
                holder.quantity()));
        inventory.setItem(MIN_1, button(
                material(plugin, "shop.confirmation.decrease-material", Material.RED_STAINED_GLASS_PANE),
                plugin.getConfig().getString("shop.confirmation.decrease-1-name", "&cRemove 1"),
                lore(plugin, "shop.confirmation.decrease-lore", "&7Current quantity: &f{quantity}", "&eClick to adjust the quantity"),
                holder.quantity()));
        inventory.setItem(RESET, button(
                material(plugin, "shop.confirmation.decrease-material", Material.RED_STAINED_GLASS_PANE),
                plugin.getConfig().getString("shop.confirmation.reset-name", "&cSet To 1"),
                lore(plugin, "shop.confirmation.decrease-lore", "&7Current quantity: &f{quantity}", "&eClick to adjust the quantity"),
                holder.quantity()));

        ItemStack selected = new ItemStack(holder.item().material(), Math.min(64, holder.quantity()));
        ItemMeta selectedMeta = selected.getItemMeta();
        if (selectedMeta != null) {
            selectedMeta.displayName(ShopMenu.component(plugin.getConfig().getString(
                    "shop.confirmation.confirm-name", "&aConfirm")));
            selectedMeta.lore(List.of(
                    ShopMenu.component(plugin.getConfig().getString("shop.confirmation.confirm-lore", "&fClick To Buy")),
                    ShopMenu.component("&7Quantity: &f" + holder.quantity()),
                    ShopMenu.component("&7Total: &f" + format(holder.item().buyPrice() * holder.quantity()))
            ));
            selected.setItemMeta(selectedMeta);
        }
        inventory.setItem(CONFIRM, selected);

        Material increase = material(plugin, "shop.confirmation.increase-material", Material.LIME_STAINED_GLASS_PANE);
        inventory.setItem(PLUS_1, button(increase,
                plugin.getConfig().getString("shop.confirmation.plus-1-name", "&aAdd 1"),
                lore(plugin, "shop.confirmation.increase-lore", "&7Current quantity: &f{quantity}", "&eClick to adjust the quantity"),
                holder.quantity()));
        inventory.setItem(PLUS_10, button(increase,
                plugin.getConfig().getString("shop.confirmation.plus-10-name", "&aAdd 10"),
                lore(plugin, "shop.confirmation.increase-lore", "&7Current quantity: &f{quantity}", "&eClick to adjust the quantity"),
                holder.quantity()));
        inventory.setItem(PLUS_64, button(increase,
                plugin.getConfig().getString("shop.confirmation.plus-64-name", "&aSet To 64"),
                lore(plugin, "shop.confirmation.increase-lore", "&7Current quantity: &f{quantity}", "&eClick to adjust the quantity"),
                holder.quantity()));
    }

    private static ItemStack button(Material material, String name, List<String> lore, int quantity) {
        List<String> replaced = lore.stream().map(line -> line.replace("{quantity}", String.valueOf(quantity))).toList();
        return button(material, name, replaced);
    }

    private static ItemStack button(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(ShopMenu.component(name));
            if (!lore.isEmpty()) {
                meta.lore(lore.stream().map(ShopMenu::component).toList());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static List<String> lore(DonutShop plugin, String path, String first, String second) {
        List<String> configured = plugin.getConfig().getStringList(path);
        if (!configured.isEmpty()) return configured;
        return List.of(first, second);
    }

    private static Material material(DonutShop plugin, String path, Material fallback) {
        String value = plugin.getConfig().getString(path, fallback.name());
        Material parsed = Material.matchMaterial(value == null ? fallback.name() : value);
        return parsed == null || parsed.isAir() ? fallback : parsed;
    }

    public static int maxPurchaseAmount(DonutShop plugin) {
        return Math.max(1, plugin.getConfig().getInt("shop.max-purchase-amount", 2304));
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
