package com.kingbrezz.donutshop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ShopItem(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        double buyPrice,
        double sellPrice,
        int slot
) {

    public ShopItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Shop item id cannot be empty."
            );
        }

        if (material == null) {
            material = Material.STONE;
        }

        if (displayName == null || displayName.isBlank()) {
            displayName = "&f" + id;
        }

        lore = lore == null
                ? List.of()
                : Collections.unmodifiableList(
                        new ArrayList<>(lore)
                );

        if (buyPrice < 0) {
            buyPrice = 0;
        }

        if (sellPrice < 0) {
            sellPrice = 0;
        }
    }

    public boolean canBuy() {
        return buyPrice > 0;
    }

    public boolean canSell() {
        return sellPrice > 0;
    }

    public boolean hasValidSlot() {
        return slot >= 0;
    }
    }
