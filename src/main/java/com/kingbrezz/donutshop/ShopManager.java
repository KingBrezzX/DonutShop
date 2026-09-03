package com.kingbrezz.donutshop;

import net.milkbowl.vault.economy.*;
import org.bukkit.Material; import org.bukkit.configuration.*; import org.bukkit.configuration.file.*; import org.bukkit.entity.Player; import org.bukkit.inventory.*;
import java.io.*; import java.util.*;

public final class ShopManager {
 public enum Failure { NONE, BALANCE, INVENTORY, INVALID, ECONOMY }
 public record PurchaseResult(boolean success, Failure reason, double total) {}
 private final DonutShop plugin; private final Economy economy; private File file; private FileConfiguration config; private final Map<String,ShopCategory> categories=new LinkedHashMap<>();
 public ShopManager(DonutShop p,Economy e){plugin=p;economy=e;}
 public boolean load(){
  File f=new File(plugin.getDataFolder(),"shop.yml"); if(!f.exists()) plugin.saveResource("shop.yml",false); FileConfiguration next=YamlConfiguration.loadConfiguration(f); ConfigurationSection root=next.getConfigurationSection("categories");
  if(root==null){plugin.getLogger().severe("shop.yml is missing categories."); return false;} Map<String,ShopCategory> parsed=new LinkedHashMap<>(); boolean valid=true;
  for(String id:root.getKeys(false)){String path="categories."+id; String name=next.getString(path+".name",id); Material icon=material(next.getString(path+".icon","CHEST"),Material.CHEST); ShopCategory cat=new ShopCategory(id.toLowerCase(Locale.ROOT),name,icon); ConfigurationSection items=next.getConfigurationSection(path+".items"); Set<Integer> slots=new HashSet<>();
   if(items!=null) for(String itemId:items.getKeys(false)){String ip=path+".items."+itemId; Material mat=material(next.getString(ip+".material","STONE"),null); double price=next.getDouble(ip+".buy",0); int slot=next.getInt(ip+".slot",-1); if(mat==null||slot<0||slot>=next.getInt("global.max-slot",54)||!slots.add(slot)||!Double.isFinite(price)||price<=0){plugin.getLogger().warning("Invalid shop item "+ip+"; skipping.");valid=false;continue;} cat.addItem(new ShopItem(itemId,mat,next.getString(ip+".name","&f"+itemId),next.getStringList(ip+".lore"),price,slot));}
   parsed.put(cat.id(),cat);
  }
  if(parsed.isEmpty()){plugin.getLogger().severe("No valid shop categories were loaded."); return false;}
  boolean strict=plugin.getConfig().getBoolean("global.strict-validation",true); if(strict && !valid){plugin.getLogger().severe("Shop validation failed in strict mode. Fix shop.yml before enabling the plugin."); return false;}
  categories.clear(); categories.putAll(parsed); file=f; config=next; plugin.getLogger().info("Loaded "+getItemCount()+" purchasable shop items across "+categories.size()+" categories."); return true;
 }
 private Material material(String s,Material fallback){try{Material m=Material.matchMaterial(s==null?"":s);return m!=null&&!m.isAir()?m:fallback;}catch(Exception e){return fallback;}}
 public void save(){if(config==null||file==null)return;try{config.save(file);}catch(IOException e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Could not save shop.yml",e);}}
 public Collection<ShopCategory> getCategories(){return Collections.unmodifiableCollection(categories.values());}
 public ShopCategory getCategory(String id){return id==null?null:categories.get(id.toLowerCase(Locale.ROOT));}
 public int getItemCount(){return categories.values().stream().mapToInt(c->c.items().size()).sum();}
 public FileConfiguration getConfig(){return config;}
 public void setConfigValue(String path,Object value){config.set(path,value);}
 public void removeConfigSection(String path){config.set(path,null);}
 public PurchaseResult buy(Player p,ShopItem item,int amount){
  if(p==null||!p.isOnline()||item==null)return new PurchaseResult(false,Failure.INVALID,0);
  int max=plugin.getConfig().getInt("shop.max-purchase-amount",2304);
  if(amount<1||amount>max)return new PurchaseResult(false,Failure.INVALID,0);
  double total=item.buyPrice()*amount;
  if(!Double.isFinite(total)||total<=0)return new PurchaseResult(false,Failure.INVALID,0);
  if(!economy.has(p,total))return new PurchaseResult(false,Failure.BALANCE,total);
  ItemStack[] before=Arrays.stream(p.getInventory().getContents()).map(x->x==null?null:x.clone()).toArray(ItemStack[]::new);
  Map<Integer,ItemStack> leftovers=p.getInventory().addItem(new ItemStack(item.material(),amount));
  if(!leftovers.isEmpty()){p.getInventory().setContents(before);return new PurchaseResult(false,Failure.INVENTORY,total);}
  EconomyResponse response=economy.withdrawPlayer(p,total);
  if(response==null||!response.transactionSuccess()){p.getInventory().setContents(before);return new PurchaseResult(false,Failure.ECONOMY,total);}
  return new PurchaseResult(true,Failure.NONE,total);
 }
}
