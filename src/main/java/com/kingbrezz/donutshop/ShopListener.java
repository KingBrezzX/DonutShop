package com.kingbrezz.donutshop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class ShopListener implements Listener {

    private final DonutShop plugin;

    public ShopListener(DonutShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!ShopMenu.isShopInventory(event.getView())) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ShopMenu.handleClick(
                plugin,
                player,
                event.getRawSlot(),
                event.getClick()
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!ShopMenu.isShopInventory(event.getView())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        ShopMenu.removeSession(player);
    }
  }
