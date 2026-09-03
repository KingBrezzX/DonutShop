package com.kingbrezz.donutshop;

import org.bukkit.command.*;
import java.util.*;

public final class DonutShopCommand implements CommandExecutor, TabCompleter {
    private final DonutShop plugin;
    public DonutShopCommand(DonutShop plugin){this.plugin=plugin;}
    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a){
        if(!s.hasPermission("donutshop.admin")){ plugin.getLanguageManager().send(s,"messages.no-permission"); return true; }
        if(a.length==0){ help(s); return true; }
        switch(a[0].toLowerCase(Locale.ROOT)){
            case "reload" -> { if(!s.hasPermission("donutshop.admin.reload")){plugin.getLanguageManager().send(s,"messages.no-permission");return true;} plugin.reloadPlugin(); plugin.getLanguageManager().send(s,"messages.reload-success"); }
            case "version" -> { plugin.getLanguageManager().send(s,"messages.version", Map.of("version",plugin.getPluginMeta().getVersion())); }
            default -> help(s);
        } return true;
    }
    private void help(CommandSender s){ plugin.getLanguageManager().send(s,"messages.admin-help"); }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args){
        if(args.length!=1) return List.of(); String q=args[0].toLowerCase(Locale.ROOT);
        return List.of("reload","version").stream().filter(x->x.startsWith(q)).toList();
    }
}
