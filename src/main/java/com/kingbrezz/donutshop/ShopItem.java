package com.kingbrezz.donutshop;
import org.bukkit.Material; import java.util.*;
public record ShopItem(String id,Material material,String displayName,List<String> lore,double buyPrice,int slot){
 public ShopItem { if(id==null||id.isBlank()) throw new IllegalArgumentException("Item id cannot be blank"); if(material==null||material.isAir()) throw new IllegalArgumentException("Invalid material for "+id); if(displayName==null||displayName.isBlank()) displayName="&f"+id; lore=lore==null?List.of():List.copyOf(lore); if(!Double.isFinite(buyPrice)||buyPrice<=0) throw new IllegalArgumentException("Invalid buy price for "+id); if(slot<0) throw new IllegalArgumentException("Invalid slot for "+id); }
}
