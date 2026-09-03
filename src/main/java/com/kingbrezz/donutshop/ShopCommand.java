package com.kingbrezz.donutshop;
import org.bukkit.command.*; import org.bukkit.entity.Player; import java.util.*;
public final class ShopCommand implements CommandExecutor,TabCompleter { private final DonutShop plugin; public ShopCommand(DonutShop p){plugin=p;}
 @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){ if(!(s instanceof Player p)){plugin.getLanguageManager().send(s,"messages.player-only");return true;} if(!p.hasPermission("donutshop.use")){plugin.getLanguageManager().send(p,"messages.no-permission");return true;} ShopMenu.openMain(plugin,p); return true; }
 @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){return List.of();}
}
