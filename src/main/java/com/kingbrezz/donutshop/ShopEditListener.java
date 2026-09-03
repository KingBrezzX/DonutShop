package com.kingbrezz.donutshop;
import org.bukkit.entity.Player; import org.bukkit.event.*; import org.bukkit.event.inventory.*; import org.bukkit.event.player.PlayerQuitEvent;
public final class ShopEditListener implements Listener {private final DonutShop plugin;public ShopEditListener(DonutShop p){plugin=p;}
 @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p))return;if(!(e.getView().getTopInventory().getHolder() instanceof ShopEditMenu.Holder h))return;e.setCancelled(true);if(e.getRawSlot()>=0&&e.getRawSlot()<e.getView().getTopInventory().getSize())ShopEditMenu.handleTopClick(plugin,p,h,e.getRawSlot(),e.getClick());else if(e.getRawSlot()>=e.getView().getTopInventory().getSize())ShopEditMenu.handlePlayerClick(plugin,p,e.getRawSlot(),e.getClick());}
 @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof ShopEditMenu.Holder h){for(int s:e.getRawSlots())if(s<e.getView().getTopInventory().getSize()){e.setCancelled(true);return;}}}
 @EventHandler public void close(InventoryCloseEvent e){if(e.getPlayer() instanceof Player p && e.getInventory().getHolder() instanceof ShopEditMenu.Holder)ShopEditMenu.close(p);}
 @EventHandler public void quit(PlayerQuitEvent e){ShopEditMenu.close(e.getPlayer());}
}
