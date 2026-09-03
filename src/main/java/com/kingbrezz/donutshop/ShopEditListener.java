package com.kingbrezz.donutshop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class ShopEditListener implements Listener {

    private final DonutShop plugin;

    public ShopEditListener(DonutShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(
            InventoryClickEvent event
    ) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!ShopEditMenu.isEditorInventory(player)) {
            return;
        }

        /*
         * Completely protect the editor GUI from normal
         * inventory manipulation.
         */
        event.setCancelled(true);

        /*
         * Ignore clicks outside the top inventory.
         */
        if (event.getRawSlot() < 0 ||
                event.getRawSlot()
                        >= event.getView()
                        .getTopInventory()
                        .getSize()) {
            return;
        }

        /*
         * Only process actual top-inventory slots.
         */
        ShopEditMenu.handleClick(
                plugin,
                player,
                event.getRawSlot(),
                event.getClick()
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!ShopEditMenu.isEditorInventory(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!ShopEditMenu.isEditorInventory(player)) {
            return;
        }

        ShopEditMenu.removeSession(player);
    }
                           }
