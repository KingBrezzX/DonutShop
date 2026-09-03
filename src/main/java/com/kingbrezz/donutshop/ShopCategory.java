package com.kingbrezz.donutshop;
import org.bukkit.Material; import java.util.*;
public final class ShopCategory {
 private final String id,name; private final Material icon; private final List<ShopItem> items=new ArrayList<>();
 public ShopCategory(String id,String name,Material icon){this.id=id;this.name=name;this.icon=icon;}
 public void addItem(ShopItem item){removeItem(item.id()); items.add(item); items.sort(Comparator.comparingInt(ShopItem::slot));}
 public void removeItem(String id){items.removeIf(i->i.id().equalsIgnoreCase(id));}
 public ShopItem getItem(String id){return items.stream().filter(i->i.id().equalsIgnoreCase(id)).findFirst().orElse(null);}
 public ShopItem getItemBySlot(int slot){return items.stream().filter(i->i.slot()==slot).findFirst().orElse(null);}
 public String id(){return id;} public String name(){return name;} public Material icon(){return icon;} public List<ShopItem> items(){return Collections.unmodifiableList(items);}
}
