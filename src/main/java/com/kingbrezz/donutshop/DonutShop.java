package com.kingbrezz.donutshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * DonutShop plugin entry point.
 *
 * <p>The plugin intentionally exposes a buy-only shop. Vault is a hard
 * runtime dependency because every purchase must be processed through an
 * Economy provider.</p>
 */
public final class DonutShop extends JavaPlugin {
    private Economy economy;
    private LanguageManager languageManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        languageManager = new LanguageManager(this);
        languageManager.load();

        if (!setupEconomy()) {
            getLogger().severe("Vault Economy provider is unavailable. DonutShop cannot start safely.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shopManager = new ShopManager(this, economy);
        if (!shopManager.load()) {
            getLogger().severe("shop.yml failed validation. DonutShop will remain disabled to protect shop data.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommands();
        registerListeners();

        getLogger().info("DonutShop " + getPluginMeta().getVersion()
                + " enabled | categories=" + shopManager.getCategories().size()
                + " | items=" + shopManager.getItemCount()
                + " | economy=" + economy.getName());
    }

    @Override
    public void onDisable() {
        if (shopManager != null && getConfig().getBoolean("persistence.save-on-disable", true)) {
            shopManager.save();
        }
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("shop"), "shop command missing from plugin.yml")
                .setExecutor(new ShopCommand(this));

        ShopEditCommand shopEditCommand = new ShopEditCommand(this);
        Objects.requireNonNull(getCommand("shopedit"), "shopedit command missing from plugin.yml")
                .setExecutor(shopEditCommand);
        getCommand("shopedit").setTabCompleter(shopEditCommand);

        DonutShopCommand adminCommand = new DonutShopCommand(this);
        Objects.requireNonNull(getCommand("donutshop"), "donutshop command missing from plugin.yml")
                .setExecutor(adminCommand);
        getCommand("donutshop").setTabCompleter(adminCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopEditListener(this), this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is not installed.");
            return false;
        }

        RegisteredServiceProvider<Economy> registration =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            getLogger().severe("No Vault Economy provider is registered.");
            return false;
        }

        economy = registration.getProvider();
        return economy.isEnabled();
    }

    public Economy getEconomy() {
        return economy;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    /** Reloads configuration, language files and shop data atomically where possible. */
    public boolean reloadPlugin() {
        reloadConfig();
        languageManager.load();
        return shopManager.load();
    }
}
