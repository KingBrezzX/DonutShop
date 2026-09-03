package com.kingbrezz.donutshop;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/** Owns validated shop data and all economy transaction logic. */
public final class ShopManager {
    public enum Failure { NONE, BALANCE, INVENTORY, INVALID, ECONOMY }
    public record PurchaseResult(boolean success, Failure reason, double total) {}

    private final DonutShop plugin;
    private Economy economy;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();

    private File shopFile;
    private FileConfiguration shopConfig;

    public ShopManager(DonutShop plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    /**
     * Parses the complete shop into a temporary map and commits it only when
     * strict validation succeeds. This prevents a bad reload from replacing
     * the last known-good in-memory shop.
     */
    public boolean load() {
        File file = new File(plugin.getDataFolder(), plugin.getConfig().getString("persistence.shop-file", "shop.yml"));
        if (!file.exists()) {
            plugin.saveResource("shop.yml", false);
        }

        FileConfiguration next = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = next.getConfigurationSection("categories");
        if (root == null) {
            plugin.getLogger().severe("shop.yml is missing the 'categories' section.");
            return false;
        }

        Map<String, ShopCategory> parsed = new LinkedHashMap<>();
        boolean valid = true;
        int maxSlot = Math.max(9, plugin.getConfig().getInt("global.max-slot", 54));
        double minimumPrice = Math.max(0.000001D, plugin.getConfig().getDouble("economy.minimum-price", 0.01D));
        int categoryMenuSize = normalizeSize(plugin.getConfig().getInt("shop.category-menu.size", 27));
        int backSlot = plugin.getConfig().getInt("shop.category-menu.back-slot", 0);

        for (String rawId : root.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            String path = "categories." + rawId;
            String name = next.getString(path + ".name", rawId);
            Material icon = parseMaterial(next.getString(path + ".icon", "CHEST"));
            ConfigurationSection items = next.getConfigurationSection(path + ".items");

            if (icon == null || items == null || items.getKeys(false).isEmpty()) {
                plugin.getLogger().warning("Invalid category " + path + ": missing icon or items.");
                valid = false;
                continue;
            }

            ShopCategory category = new ShopCategory(id, name, icon);
            Set<Integer> usedSlots = new HashSet<>();

            for (String itemId : items.getKeys(false)) {
                String itemPath = path + ".items." + itemId;
                Material material = parseMaterial(next.getString(itemPath + ".material"));
                double price = next.getDouble(itemPath + ".buy", -1D);
                int slot = next.getInt(itemPath + ".slot", -1);
                String displayName = next.getString(itemPath + ".name", "&f" + itemId);

                boolean itemValid = material != null
                        && slot >= 0
                        && slot < maxSlot
                        && slot < categoryMenuSize
                        && slot != backSlot
                        && usedSlots.add(slot)
                        && Double.isFinite(price)
                        && price >= minimumPrice
                        && price > 0
                        && displayName != null
                        && !displayName.isBlank();

                if (!itemValid) {
                    plugin.getLogger().warning("Invalid shop item: " + itemPath + " (skipping)");
                    valid = false;
                    continue;
                }

                category.addItem(new ShopItem(
                        itemId,
                        material,
                        displayName,
                        next.getStringList(itemPath + ".lore"),
                        price,
                        slot
                ));
            }

            if (category.items().isEmpty()) {
                plugin.getLogger().warning("Category " + id + " contains no valid items.");
                valid = false;
                continue;
            }
            parsed.put(id, category);
        }

        if (parsed.isEmpty()) {
            plugin.getLogger().severe("No valid shop categories were loaded.");
            return false;
        }

        validateMainMenu(parsed);

        boolean strict = plugin.getConfig().getBoolean("global.strict-validation", true);
        if (strict && !valid) {
            plugin.getLogger().severe("Strict shop validation failed. Existing in-memory shop data was retained.");
            return false;
        }

        categories.clear();
        categories.putAll(parsed);
        shopFile = file;
        shopConfig = next;

        if (plugin.getConfig().getBoolean("global.verbose-logging", true)) {
            plugin.getLogger().info("Loaded " + getItemCount() + " purchasable items across "
                    + categories.size() + " categories.");
        }
        return true;
    }

    private void validateMainMenu(Map<String, ShopCategory> parsed) {
        var ids = plugin.getConfig().getStringList("shop.main-menu.categories");
        var slots = plugin.getConfig().getIntegerList("shop.main-menu.category-slots");
        if (ids.size() != slots.size()) {
            plugin.getLogger().warning("shop.main-menu.categories and category-slots must contain the same number of entries.");
        }
        Set<Integer> used = new HashSet<>();
        int size = normalizeSize(plugin.getConfig().getInt("shop.main-menu.size", 27));
        for (int i = 0; i < Math.min(ids.size(), slots.size()); i++) {
            if (!parsed.containsKey(ids.get(i).toLowerCase(Locale.ROOT)) || slots.get(i) < 0 || slots.get(i) >= size || !used.add(slots.get(i))) {
                plugin.getLogger().warning("Invalid main-menu category entry at index " + i + ".");
            }
        }
    }

    private Material parseMaterial(String name) {
        if (name == null || name.isBlank()) return null;
        Material material = Material.matchMaterial(name.trim());
        return material == null || material.isAir() ? null : material;
    }

    private int normalizeSize(int size) {
        size = Math.max(9, Math.min(54, size));
        return size - (size % 9);
    }

    /** Updates the active Vault provider without replacing shop data. */
    public void setEconomy(Economy economy) {
        this.economy = economy;
    }

    public void save() {
        if (shopConfig == null || shopFile == null) return;
        try {
            shopConfig.save(shopFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Unable to save " + shopFile.getName(), exception);
        }
    }

    public Collection<ShopCategory> getCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    public ShopCategory getCategory(String id) {
        return id == null ? null : categories.get(id.toLowerCase(Locale.ROOT));
    }

    public int getItemCount() {
        return categories.values().stream().mapToInt(category -> category.items().size()).sum();
    }

    public FileConfiguration getConfig() {
        return shopConfig;
    }

    public void setConfigValue(String path, Object value) {
        if (shopConfig != null) shopConfig.set(path, value);
    }

    public void removeConfigSection(String path) {
        if (shopConfig != null) shopConfig.set(path, null);
    }

    public PurchaseResult buy(Player player, ShopItem item, int amount) {
        if (player == null || !player.isOnline() || item == null) {
            return new PurchaseResult(false, Failure.INVALID, 0);
        }

        int max = Math.max(1, plugin.getConfig().getInt("shop.max-purchase-amount", 2304));
        if (amount < 1 || amount > max) {
            return new PurchaseResult(false, Failure.INVALID, 0);
        }

        double total = item.buyPrice() * amount;
        if (!Double.isFinite(total) || total <= 0 || total < plugin.getConfig().getDouble("economy.minimum-price", 0.01D)) {
            return new PurchaseResult(false, Failure.INVALID, 0);
        }

        Economy activeEconomy = economy;
        if (activeEconomy == null || !activeEconomy.isEnabled()) {
            return new PurchaseResult(false, Failure.ECONOMY, total);
        }

        if (!activeEconomy.has(player, total)) {
            return new PurchaseResult(false, Failure.BALANCE, total);
        }

        var inventory = player.getInventory();
        ItemStack[] before = Arrays.stream(inventory.getStorageContents())
                .map(stack -> stack == null ? null : stack.clone())
                .toArray(ItemStack[]::new);

        Map<Integer, ItemStack> leftovers = inventory.addItem(new ItemStack(item.material(), amount));
        if (!leftovers.isEmpty()) {
            inventory.setStorageContents(before);
            return new PurchaseResult(false, Failure.INVENTORY, total);
        }

        EconomyResponse response;
        try {
            response = activeEconomy.withdrawPlayer(player, total);
        } catch (RuntimeException exception) {
            inventory.setStorageContents(before);
            plugin.getLogger().log(Level.SEVERE, "Economy provider threw during purchase for " + player.getName(), exception);
            return new PurchaseResult(false, Failure.ECONOMY, total);
        }

        if (response == null || !response.transactionSuccess()) {
            inventory.setStorageContents(before);
            if (response != null) {
                plugin.getLogger().warning("Economy transaction failed for " + player.getName() + ": " + response.errorMessage);
            }
            return new PurchaseResult(false, Failure.ECONOMY, total);
        }

        return new PurchaseResult(true, Failure.NONE, total);
    }
}
