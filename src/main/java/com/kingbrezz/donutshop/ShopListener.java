package com.kingbrezz.donutshop;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Protects player shop GUIs and dispatches purchases. */
public final class ShopListener implements Listener {
    private final DonutShop plugin;

    public ShopListener(DonutShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopMenu.Holder holder)) return;

        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topSize) return;

        event.setCancelled(true);
        if (holder.isMain()) {
            handleMainClick(player, event.getRawSlot());
            return;
        }

        handleCategoryClick(player, holder, event.getRawSlot(), event.isShiftClick());
    }

    private void handleMainClick(Player player, int rawSlot) {
        List<String> categories = plugin.getConfig().getStringList("shop.main-menu.categories");
        List<Integer> slots = plugin.getConfig().getIntegerList("shop.main-menu.category-slots");
        int index = slots.indexOf(rawSlot);
        if (index < 0 || index >= categories.size()) return;

        String id = categories.get(index);
        ShopCategory category = plugin.getShopManager().getCategory(id);
        if (category == null) {
            plugin.getLanguageManager().send(player, "messages.category-not-found", Map.of("category", id));
            return;
        }
        ShopMenu.openCategory(plugin, player, category);
    }

    private void handleCategoryClick(Player player, ShopMenu.Holder holder, int rawSlot, boolean shiftClick) {
        int backSlot = plugin.getConfig().getInt("shop.category-menu.back-slot", 0);
        if (rawSlot == backSlot) {
            ShopMenu.openMain(plugin, player);
            return;
        }

        ShopCategory category = plugin.getShopManager().getCategory(holder.category());
        if (category == null) return;

        ShopItem item = category.getItemBySlot(rawSlot);
        if (item == null) return;

        int amount = shiftClick
                ? plugin.getConfig().getInt("shop.shift-click-amount", 16)
                : plugin.getConfig().getInt("shop.default-amount", 1);
        amount = Math.max(1, amount);

        ShopManager.PurchaseResult result = plugin.getShopManager().buy(player, item, amount);
        if (result.success()) {
            String displayName = org.bukkit.ChatColor.stripColor(ShopMenu.color(item.displayName()));
            plugin.getLanguageManager().send(player, "messages.purchase-success", Map.of(
                    "amount", String.valueOf(amount),
                    "item", displayName,
                    "price", format(result.total())
            ));
            playSound(player, "sounds.purchase");
            return;
        }

        String messageKey = switch (result.reason()) {
            case BALANCE -> "messages.not-enough-money";
            case INVENTORY -> "messages.inventory-full";
            case INVALID -> "messages.cannot-buy";
            case ECONOMY, NONE -> "messages.purchase-failed";
        };
        plugin.getLanguageManager().send(player, messageKey);
        playSound(player, "sounds.error");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopMenu.Holder)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    private void playSound(Player player, String path) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) return;
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString(path + ".sound", "ENTITY_EXPERIENCE_ORB_PICKUP"));
            float volume = (float) plugin.getConfig().getDouble(path + ".volume", 1.0D);
            float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 1.0D);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid sound configured at " + path);
        }
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
