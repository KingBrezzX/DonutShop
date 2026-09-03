package com.kingbrezz.donutshop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Immutable category metadata with controlled item mutation for ShopEdit. */
public final class ShopCategory {
    private final String id;
    private final String name;
    private final Material icon;
    private final List<ShopItem> items = new ArrayList<>();

    public ShopCategory(String id, String name, Material icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public void addItem(ShopItem item) {
        removeItem(item.id());
        items.add(item);
        items.sort(Comparator.comparingInt(ShopItem::slot));
    }

    public void removeItem(String id) {
        items.removeIf(item -> item.id().equalsIgnoreCase(id));
    }

    public ShopItem getItem(String id) {
        return items.stream().filter(item -> item.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public ShopItem getItemBySlot(int slot) {
        return items.stream().filter(item -> item.slot() == slot).findFirst().orElse(null);
    }

    public String id() { return id; }
    public String name() { return name; }
    public Material icon() { return icon; }
    public Collection<ShopItem> items() { return List.copyOf(items); }
}
