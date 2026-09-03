package com.kingbrezz.donutshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutShop extends JavaPlugin {
    private Economy economy;
    private LanguageManager languageManager;
    private ShopManager shopManager;

    @Override public void onEnable() {
        saveDefaultConfig();
        languageManager = new LanguageManager(this);
        languageManager.load();
        if (!setupEconomy()) {
            getLogger().severe("No Vault economy provider is available. DonutShop will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        shopManager = new ShopManager(this, economy);
        if (!shopManager.load()) {
            getLogger().severe("shop.yml could not be loaded safely. DonutShop will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ShopCommand shop = new ShopCommand(this);
        ShopEditCommand edit = new ShopEditCommand(this);
        DonutShopCommand admin = new DonutShopCommand(this);
        getCommand("shop").setExecutor(shop); getCommand("shop").setTabCompleter(shop);
        getCommand("shopedit").setExecutor(edit); getCommand("shopedit").setTabCompleter(edit);
        getCommand("donutshop").setExecutor(admin); getCommand("donutshop").setTabCompleter(admin);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopEditListener(this), this);
        getLogger().info("DonutShop " + getPluginMeta().getVersion() + " enabled. Categories=" + shopManager.getCategories().size() + ", Items=" + shopManager.getItemCount());
    }

    @Override public void onDisable() {
        if (shopManager != null && getConfig().getBoolean("persistence.save-on-disable", true)) shopManager.save();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) return false;
        economy = registration.getProvider();
        return economy.isEnabled();
    }
    public Economy getEconomy(){ return economy; }
    public LanguageManager getLanguageManager(){ return languageManager; }
    public ShopManager getShopManager(){ return shopManager; }
    public void reloadPlugin(){
        reloadConfig();
        languageManager.load();
        if (!shopManager.load()) getLogger().warning("Reload completed with shop validation warnings; previous valid shop data may have been retained.");
    }
}
