package com.kingbrezz.donutshop;

import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Session-based ShopEdit UI. It never removes the source item from a player's inventory. */
public final class ShopEditMenu {
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "\\[PRICE\\]\\s*([0-9]+(?:[.,][0-9]+)?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private ShopEditMenu() {}

    public static final class Holder implements InventoryHolder {
        private final String category;
        private final UUID sessionId;
        private Inventory inventory;

        private Holder(String category, UUID sessionId) {
            this.category = category;
            this.sessionId = sessionId;
        }

        private void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        public String category() { return category; }
        public UUID sessionId() { return sessionId; }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class Session {
        private final String category;
        private final UUID id;
        private ItemStack selected;

        private Session(String category, UUID id) {
            this.category = category;
            this.id = id;
        }
    }

    public static void open(DonutShop plugin, Player player, ShopCategory category) {
        close(player);

        int size = normalizeSize(plugin.getConfig().getInt("shop-edit.size", 54));
        UUID sessionId = UUID.randomUUID();
        Holder holder = new Holder(category.id(), sessionId);
        String title = plugin.getConfig().getString("shop-edit.title", "&8&lShopEdit &7• &f{category}")
                .replace("{category}", category.id());
        Inventory inventory = Bukkit.createInventory(holder, size, component(title));
        holder.bind(inventory);
        SESSIONS.put(player.getUniqueId(), new Session(category.id(), sessionId));

        for (ShopItem item : category.items()) {
            if (isEditableSlot(plugin, item.slot(), size)) {
                inventory.setItem(item.slot(), ShopMenu.display(plugin, item));
            }
        }
        inventory.setItem(4, helpItem(plugin));
        player.openInventory(inventory);
    }

    /** Handles clicks in the player's inventory while ShopEdit is open. */
    public static void handlePlayerClick(DonutShop plugin, Player player, int rawSlot, ClickType click, int topSize) {
        Session session = SESSIONS.get(player.getUniqueId());
        if (session == null || rawSlot < topSize) return;

        int playerSlot = rawSlot - topSize;
        if (playerSlot < 0 || playerSlot >= player.getInventory().getSize()) return;

        ItemStack source = player.getInventory().getItem(playerSlot);
        if (source == null || source.getType().isAir()) {
            plugin.getLanguageManager().send(player, "messages.shopedit-invalid-item");
            return;
        }

        ItemStack selected = source.clone();
        int selectionAmount = Math.max(1, plugin.getConfig().getInt("shop-edit.selection.amount", 1));
        selected.setAmount(Math.min(selectionAmount, source.getAmount()));
        session.selected = selected;
        plugin.getLanguageManager().send(player, "messages.shopedit-item-selected");
    }

    public static void handleTopClick(DonutShop plugin, Player player, Holder holder, int slot, ClickType click) {
        Session session = SESSIONS.get(player.getUniqueId());
        if (session == null || !session.id.equals(holder.sessionId())) return;

        if (slot == 4) {
            plugin.getLanguageManager().send(player, "messages.shopedit-help");
            return;
        }

        int size = holder.getInventory().getSize();
        if (!isEditableSlot(plugin, slot, size)) {
            plugin.getLanguageManager().send(player, "messages.shopedit-reserved-slot");
            return;
        }

        ShopCategory category = plugin.getShopManager().getCategory(holder.category());
        if (category == null) return;

        if (click.isRightClick() && !click.isShiftClick()) {
            removeItem(plugin, player, category, slot);
            return;
        }

        if (session.selected == null || session.selected.getType().isAir()) {
            plugin.getLanguageManager().send(player, "messages.shopedit-invalid-item");
            return;
        }

        placeItem(plugin, player, category, slot, session.selected);
    }

    private static void placeItem(DonutShop plugin, Player player, ShopCategory category, int slot, ItemStack selected) {
        ShopItem old = category.getItemBySlot(slot);
        String id = old == null
                ? selected.getType().name().toLowerCase(Locale.ROOT) + "_" + slot
                : old.id();

        if (old != null) {
            plugin.getShopManager().removeConfigSection("categories." + category.id() + ".items." + old.id());
            category.removeItem(old.id());
        }

        double fallback = Math.max(
                plugin.getConfig().getDouble("economy.minimum-price", 0.01D),
                plugin.getConfig().getDouble("shop-edit.default-price", 100.00D)
        );
        double price = parsePrice(selected, fallback);
        String displayName = selected.hasItemMeta() && selected.getItemMeta().hasDisplayName()
                ? LEGACY.serialize(selected.getItemMeta().displayName())
                : "&f" + pretty(selected.getType());
        List<String> lore = selected.hasItemMeta() && selected.getItemMeta().hasLore()
                ? selected.getItemMeta().lore().stream().map(LEGACY::serialize).collect(java.util.stream.Collectors.toCollection(ArrayList::new))
                : new ArrayList<>();

        if (plugin.getConfig().getBoolean("shop-edit.selection.strip-price-tags", false)) {
            displayName = stripPriceTag(displayName);
            lore.replaceAll(ShopEditMenu::stripPriceTag);
            lore.removeIf(String::isBlank);
        }

        ShopItem item = new ShopItem(id, selected.getType(), displayName, lore, price, slot);
        category.addItem(item);
        writeItem(plugin, category, item);
        refresh(plugin, player, category);
        plugin.getLanguageManager().send(player, "messages.shopedit-item-saved", Map.of(
                "item", PLAIN.serialize(component(displayName)),
                "price", format(price)
        ));
        autoSave(plugin);
    }

    private static void removeItem(DonutShop plugin, Player player, ShopCategory category, int slot) {
        ShopItem old = category.getItemBySlot(slot);
        if (old == null) return;

        plugin.getShopManager().removeConfigSection("categories." + category.id() + ".items." + old.id());
        category.removeItem(old.id());
        refresh(plugin, player, category);
        plugin.getLanguageManager().send(player, "messages.shopedit-item-removed");
        autoSave(plugin);
    }

    private static void writeItem(DonutShop plugin, ShopCategory category, ShopItem item) {
        String path = "categories." + category.id() + ".items." + item.id();
        plugin.getShopManager().setConfigValue(path + ".material", item.material().name());
        plugin.getShopManager().setConfigValue(path + ".name", item.displayName());
        plugin.getShopManager().setConfigValue(path + ".lore", item.lore());
        plugin.getShopManager().setConfigValue(path + ".buy", item.buyPrice());
        plugin.getShopManager().setConfigValue(path + ".slot", item.slot());
    }

    private static double parsePrice(ItemStack item, double fallback) {
        if (!item.hasItemMeta()) return fallback;
        ItemMeta meta = item.getItemMeta();
        List<String> lines = new ArrayList<>();
        if (meta.hasDisplayName()) lines.add(LEGACY.serialize(meta.displayName()));
        if (meta.hasLore()) lines.addAll(meta.lore().stream().map(LEGACY::serialize).toList());

        for (String line : lines) {
            Matcher matcher = PRICE_PATTERN.matcher(PLAIN.serialize(component(line)));
            if (!matcher.find()) continue;
            try {
                double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
                if (Double.isFinite(value) && value > 0) return value;
            } catch (NumberFormatException ignored) {
                // Invalid editor tag intentionally falls back to configured default.
            }
        }
        return fallback;
    }

    private static String stripPriceTag(String text) {
        return PRICE_PATTERN.matcher(text == null ? "" : text).replaceAll("").trim();
    }

    private static boolean isEditableSlot(DonutShop plugin, int slot, int inventorySize) {
        int min = plugin.getConfig().getInt("shop-edit.allowed-slots.min", 9);
        int max = plugin.getConfig().getInt("shop-edit.allowed-slots.max", 44);
        if (slot < 0 || slot >= inventorySize || slot < min || slot > max) return false;
        return !plugin.getConfig().getIntegerList("shop-edit.reserved-slots").contains(slot);
    }

    private static void refresh(DonutShop plugin, Player player, ShopCategory category) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (!(inventory.getHolder() instanceof Holder)) return;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot != 4 && isEditableSlot(plugin, slot, inventory.getSize())) {
                inventory.setItem(slot, null);
            }
        }
        for (ShopItem item : category.items()) {
            if (isEditableSlot(plugin, item.slot(), inventory.getSize())) {
                inventory.setItem(item.slot(), ShopMenu.display(plugin, item));
            }
        }
        inventory.setItem(4, helpItem(plugin));
    }

    private static ItemStack helpItem(DonutShop plugin) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(component(plugin.getConfig().getString("shop-edit.help.title", "&b&lShopEdit Help")));
            meta.lore(List.of(
                    component(plugin.getConfig().getString("shop-edit.help.select", "&7Click an item in your inventory to select it.")),
                    component(plugin.getConfig().getString("shop-edit.help.place", "&7Left-click a valid shop slot to place it.")),
                    component(plugin.getConfig().getString("shop-edit.help.remove", "&7Right-click a shop slot to remove it.")),
                    component(plugin.getConfig().getString("shop-edit.help.price", "&7Add &f[PRICE] 250 &7to the item's name/lore."))
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String pretty(Material material) {
        return Arrays.stream(material.name().toLowerCase(Locale.ROOT).split("_"))
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(material.name());
    }

    private static void autoSave(DonutShop plugin) {
        if (plugin.getConfig().getBoolean("shop-edit.auto-save", true)) {
            plugin.getShopManager().save();
        }
    }

    public static void close(Player player) {
        SESSIONS.remove(player.getUniqueId());
    }

    private static int normalizeSize(int size) {
        size = Math.max(27, Math.min(54, size));
        return size - size % 9;
    }

    private static Component component(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
