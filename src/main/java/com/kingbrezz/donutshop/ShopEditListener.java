package com.kingbrezz.donutshop;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class ShopEditListener
        implements Listener {

    private final DonutShop plugin;

    public ShopEditListener(DonutShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(
            InventoryClickEvent event
    ) {
        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        if (!ShopEditMenu.isEditorInventory(player)) {
            return;
        }

        int topSize =
                event.getView()
                        .getTopInventory()
                        .getSize();

        /*
         * Click inside the editor GUI.
         */
        if (event.getRawSlot() >= 0 &&
                event.getRawSlot() < topSize) {

            event.setCancelled(true);

            ShopEditMenu.handleClick(
                    plugin,
                    player,
                    event.getRawSlot(),
                    event.getClick()
            );

            return;
        }

        /*
         * Click inside the player's inventory.
         *
         * The item is copied to the cursor instead
         * of being removed from the player's inventory.
         */
        if (event.getRawSlot() >= topSize) {

            ItemStack clicked =
                    event.getCurrentItem();

            if (clicked == null ||
                    clicked.getType() == Material.AIR) {
                return;
            }

            if (!event.isLeftClick() &&
                    !event.isRightClick()) {
                return;
            }

            event.setCancelled(true);

            ItemStack selected =
                    clicked.clone();

            selected.setAmount(1);

            player.setItemOnCursor(
                    selected
            );

            plugin.getLanguageManager()
                    .send(
                            player,
                            "messages.shopedit-item-selected"
                    );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {
        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        if (!ShopEditMenu.isEditorInventory(player)) {
            return;
        }

        int topSize =
                event.getView()
                        .getTopInventory()
                        .getSize();

        /*
         * Prevent dragging items into or around
         * the editor GUI.
         */
        for (int rawSlot :
                event.getRawSlots()) {

            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {
        if (!(event.getPlayer()
                instanceof Player player)) {
            return;
        }

        if (!ShopEditMenu.isEditorInventory(player)) {
            return;
        }

        ShopEditMenu.removeSession(
                player
        );
    }
                }
