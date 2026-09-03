package com.kingbrezz.donutshop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Strictly controls the ShopEdit inventory to prevent item movement/duplication. */
public final class ShopEditListener implements Listener {
    private final DonutShop plugin;

    public ShopEditListener(DonutShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopEditMenu.Holder holder)) return;

        event.setCancelled(true);
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() >= 0 && event.getRawSlot() < topSize) {
            ShopEditMenu.handleTopClick(plugin, player, holder, event.getRawSlot(), event.getClick());
        } else if (event.getRawSlot() >= topSize) {
            ShopEditMenu.handlePlayerClick(plugin, player, event.getRawSlot(), event.getClick(), topSize);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopEditMenu.Holder)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player
                && event.getInventory().getHolder() instanceof ShopEditMenu.Holder) {
            ShopEditMenu.close(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ShopEditMenu.close(event.getPlayer());
    }
}
