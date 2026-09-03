package com.kingbrezz.donutshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutShop extends JavaPlugin {

    private Economy economy;
    private ShopManager shopManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        languageManager = new LanguageManager(this);
        languageManager.load();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy was not found.");
            getLogger().severe(
                    "DonutShop requires Vault and a compatible economy provider."
            );

            getServer().getPluginManager()
                    .disablePlugin(this);
            return;
        }

        shopManager = new ShopManager(
                this,
                economy
        );

        shopManager.load();

        ShopCommand shopCommand =
                new ShopCommand(this);

        ShopEditCommand shopEditCommand =
                new ShopEditCommand(this);

        DonutShopCommand adminCommand =
                new DonutShopCommand(this);

        if (getCommand("shop") != null) {
            getCommand("shop")
                    .setExecutor(shopCommand);

            getCommand("shop")
                    .setTabCompleter(shopCommand);
        }

        if (getCommand("shopedit") != null) {
            getCommand("shopedit")
                    .setExecutor(shopEditCommand);

            getCommand("shopedit")
                    .setTabCompleter(shopEditCommand);
        }

        if (getCommand("donutshop") != null) {
            getCommand("donutshop")
                    .setExecutor(adminCommand);

            getCommand("donutshop")
                    .setTabCompleter(adminCommand);
        }

        getServer().getPluginManager()
                .registerEvents(
                        new ShopListener(this),
                        this
                );

        getServer().getPluginManager()
                .registerEvents(
                        new ShopEditListener(this),
                        this
                );

        getLogger().info(
                "DonutShop enabled successfully."
        );
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.save();
        }

        getLogger().info(
                "DonutShop disabled."
        );
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager()
                .getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                getServer().getServicesManager()
                        .getRegistration(
                                Economy.class
                        );

        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();

        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public void reloadPlugin() {
        reloadConfig();

        if (languageManager != null) {
            languageManager.load();
        }

        if (shopManager != null) {
            shopManager.load();
        }
    }
}
