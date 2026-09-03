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

public final class ShopListener implements Listener {
    private final DonutShop plugin;
    public ShopListener(DonutShop plugin){ this.plugin=plugin; }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onClick(InventoryClickEvent event){
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopMenu.Holder holder)) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        event.setCancelled(true);

        if (holder.isMain()) {
            List<String> categories=plugin.getConfig().getStringList("shop.main-menu.categories");
            List<Integer> slots=plugin.getConfig().getIntegerList("shop.main-menu.category-slots");
            int index=slots.indexOf(event.getRawSlot());
            if(index<0 || index>=categories.size()) return;
            ShopCategory category=plugin.getShopManager().getCategory(categories.get(index));
            if(category==null){ plugin.getLanguageManager().send(player,"messages.category-not-found",Map.of("category",categories.get(index))); return; }
            ShopMenu.openCategory(plugin,player,category); return;
        }

        if (event.getRawSlot()==plugin.getConfig().getInt("shop.category-menu.back-slot",0)) {
            ShopMenu.openMain(plugin,player); return;
        }
        ShopCategory category=plugin.getShopManager().getCategory(holder.category());
        if(category==null) return;
        ShopItem item=category.getItemBySlot(event.getRawSlot());
        if(item==null) return;
        int amount=event.isShiftClick()?plugin.getConfig().getInt("shop.shift-click-amount",16):plugin.getConfig().getInt("shop.default-amount",1);
        if(amount<1) amount=1;
        ShopManager.PurchaseResult result=plugin.getShopManager().buy(player,item,amount);
        if(result.success()){
            plugin.getLanguageManager().send(player,"messages.purchase-success",Map.of("amount",String.valueOf(amount),"item",item.displayName().replace("&f",""),"price",format(result.total())));
            sound(player,"sounds.purchase");
        }else{
            String key=switch(result.reason()){case BALANCE -> "messages.not-enough-money";case INVENTORY -> "messages.inventory-full";case INVALID -> "messages.cannot-buy";default -> "messages.purchase-failed";};
            plugin.getLanguageManager().send(player,key); sound(player,"sounds.error");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDrag(InventoryDragEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof ShopMenu.Holder holder){
            for(int raw:event.getRawSlots()) if(raw<event.getView().getTopInventory().getSize()){event.setCancelled(true);return;}
        }
    }

    private void sound(Player player,String path){
        if(!plugin.getConfig().getBoolean("sounds.enabled",true)) return;
        try{Sound sound=Sound.valueOf(plugin.getConfig().getString(path+".sound","ENTITY_EXPERIENCE_ORB_PICKUP"));player.playSound(player.getLocation(),sound,(float)plugin.getConfig().getDouble(path+".volume",1),(float)plugin.getConfig().getDouble(path+".pitch",1));}catch(IllegalArgumentException ignored){plugin.getLogger().warning("Invalid sound configured at "+path);}
    }
    private String format(double value){return String.format(Locale.US,"%.2f",value);}
}
