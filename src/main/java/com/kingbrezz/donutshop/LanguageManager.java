package com.kingbrezz.donutshop;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.util.*;

public final class LanguageManager {
    public static final List<String> SUPPORTED=List.of("id","en","zh","vi","de");
    private final DonutShop plugin; private final Map<String,FileConfiguration> languages=new LinkedHashMap<>();
    public LanguageManager(DonutShop plugin){this.plugin=plugin;}
    public void load(){
        languages.clear(); File dir=new File(plugin.getDataFolder(),"lang"); if(!dir.exists() && !dir.mkdirs()) plugin.getLogger().warning("Could not create lang directory.");
        for(String id:SUPPORTED){File f=new File(dir,id+".yml"); if(!f.exists()) plugin.saveResource("lang/"+id+".yml",false); languages.put(id,YamlConfiguration.loadConfiguration(f));}
        FileConfiguration base=languages.get("en"); for(String id:SUPPORTED){ for(String key:base.getKeys(true)){ if(base.isString(key) && !languages.get(id).isString(key)) plugin.getLogger().warning("Missing language key: "+id+" -> "+key); } }
    }
    public String get(String path){return get(path,Map.of());}
    public String get(String path, Map<String,String> placeholders){
        String id=plugin.getConfig().getString("language","id").toLowerCase(Locale.ROOT); FileConfiguration cfg=languages.getOrDefault(id,languages.get("id"));
        String value=cfg==null?path:cfg.getString(path,path); for(var e:placeholders.entrySet()) value=value.replace("{" + e.getKey() + "}",e.getValue());
        return ChatColor.translateAlternateColorCodes('&',value);
    }
    public void send(CommandSender sender,String path){sender.sendMessage(get(path));}
    public void send(CommandSender sender,String path,Map<String,String> placeholders){sender.sendMessage(get(path,placeholders));}
    public boolean isSupported(String id){return id!=null && SUPPORTED.contains(id.toLowerCase(Locale.ROOT));}
}
