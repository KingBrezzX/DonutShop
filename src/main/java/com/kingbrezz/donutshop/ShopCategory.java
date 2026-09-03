package com.kingbrezz.donutshop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopCategory {

    private final String id;
    private final String name;
    private final Material icon;
    private final List<ShopItem> items = new ArrayList<>();

    public ShopCategory(
            String id,
            String name,
            Material icon
    ) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public void addItem(ShopItem item) {
        if (item == null) {
            return;
        }

        removeItem(item.id());
        items.add(item);
    }

    public void removeItem(String id) {
        if (id == null) {
            return;
        }

        items.removeIf(item ->
                item.id().equalsIgnoreCase(id)
        );
    }

    public ShopItem getItem(String id) {
        if (id == null) {
            return null;
        }

        for (ShopItem item : items) {
            if (item.id().equalsIgnoreCase(id)) {
                return item;
            }
        }

        return null;
    }

    public ShopItem getItemBySlot(int slot) {
        for (ShopItem item : items) {
            if (item.slot() == slot) {
                return item;
            }
        }

        return null;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Material icon() {
        return icon;
    }

    public List<ShopItem> items() {
        return Collections.unmodifiableList(items);
    }
}
