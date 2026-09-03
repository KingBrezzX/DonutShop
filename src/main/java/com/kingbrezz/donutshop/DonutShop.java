package com.kingbrezz.donutshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

/**
 * DonutShop plugin entry point.
 *
 * <p>Vault is intentionally treated as a runtime service rather than assuming
 * that an Economy provider is already registered during onEnable(). This is
 * important on servers where Vault starts before an economy plugin finishes
 * registering its provider.</p>
 */
public final class DonutShop extends JavaPlugin implements Listener {
    private Economy economy;
    private LanguageManager languageManager;
    private ShopManager shopManager;
    private BukkitTask economyRetryTask;
    private boolean ready;
    private boolean shopInitializationFailed;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        languageManager = new LanguageManager(this);
        languageManager.load();

        // Listen for late Vault Economy service registration before attempting
        // the first hook. This removes startup-order sensitivity.
        getServer().getPluginManager().registerEvents(this, this);

        registerCommands();
        registerListeners();

        if (!tryInitializeEconomy()) {
            scheduleEconomyRetry();
            getLogger().warning("No Vault Economy provider is registered yet. "
                    + "DonutShop will remain enabled and retry automatically.");
        }
    }

    @Override
    public void onDisable() {
        if (economyRetryTask != null) {
            economyRetryTask.cancel();
            economyRetryTask = null;
        }

        if (shopManager != null && getConfig().getBoolean("persistence.save-on-disable", true)) {
            shopManager.save();
        }
    }

    @EventHandler
    public void onEconomyServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider() != Economy.class) return;

        // Services may be registered while the server is still processing its
        // startup queue. Move initialization to the next tick for a stable
        // provider lookup and to avoid ordering races.
        getServer().getScheduler().runTask(this, this::tryInitializeEconomy);
    }

    @EventHandler
    public void onEconomyServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider() != Economy.class) return;

        RegisteredServiceProvider<Economy> current = getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (current != null && current.getProvider() != null) {
            // Another provider has already replaced the old one.
            getServer().getScheduler().runTask(this, this::tryInitializeEconomy);
            return;
        }

        ready = false;
        economy = null;
        if (shopManager != null) {
            shopManager.setEconomy(null);
        }
        getLogger().warning("Vault Economy provider was unregistered. "
                + "Purchases are temporarily disabled; DonutShop will retry automatically.");
        scheduleEconomyRetry();
    }

    private boolean tryInitializeEconomy() {
        if (!isEnabled() || shopInitializationFailed) return false;

        Economy provider = findEconomyProvider();
        if (provider == null) {
            ready = false;
            return false;
        }

        if (shopManager == null) {
            ShopManager manager = new ShopManager(this, provider);
            if (!manager.load()) {
                shopInitializationFailed = true;
                getLogger().severe("shop.yml failed validation. DonutShop will remain disabled "
                        + "to protect shop data.");
                getServer().getPluginManager().disablePlugin(this);
                return false;
            }
            shopManager = manager;
        } else {
            shopManager.setEconomy(provider);
        }

        economy = provider;
        ready = true;

        if (economyRetryTask != null) {
            economyRetryTask.cancel();
            economyRetryTask = null;
        }

        getLogger().info("DonutShop " + getPluginMeta().getVersion()
                + " economy ready | provider=" + provider.getName()
                + " | categories=" + shopManager.getCategories().size()
                + " | items=" + shopManager.getItemCount());
        return true;
    }

    private Economy findEconomyProvider() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }

        RegisteredServiceProvider<Economy> registration =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            return null;
        }

        Economy provider = registration.getProvider();
        return provider.isEnabled() ? provider : null;
    }

    private void scheduleEconomyRetry() {
        if (economyRetryTask != null || shopInitializationFailed || !isEnabled()) return;

        // Check once per second. The task is lightweight: it only asks Bukkit's
        // ServicesManager for the current Economy registration.
        economyRetryTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (ready || shopInitializationFailed) {
                if (economyRetryTask != null) {
                    economyRetryTask.cancel();
                    economyRetryTask = null;
                }
                return;
            }
            tryInitializeEconomy();
        }, 1L, 20L);
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("shop"), "shop command missing from plugin.yml")
                .setExecutor(new ShopCommand(this));

        ShopEditCommand shopEditCommand = new ShopEditCommand(this);
        Objects.requireNonNull(getCommand("shopedit"), "shopedit command missing from plugin.yml")
                .setExecutor(shopEditCommand);
        Objects.requireNonNull(getCommand("shopedit")).setTabCompleter(shopEditCommand);

        DonutShopCommand adminCommand = new DonutShopCommand(this);
        Objects.requireNonNull(getCommand("donutshop"), "donutshop command missing from plugin.yml")
                .setExecutor(adminCommand);
        Objects.requireNonNull(getCommand("donutshop")).setTabCompleter(adminCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopEditListener(this), this);
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

    public boolean isReady() {
        return ready && economy != null && shopManager != null;
    }

    /** Reloads configuration, language files and shop data atomically where possible. */
    public boolean reloadPlugin() {
        reloadConfig();
        languageManager.load();

        if (!tryInitializeEconomy()) {
            scheduleEconomyRetry();
            return false;
        }

        return shopManager.load();
    }
}
