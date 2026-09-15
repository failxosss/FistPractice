package com.fistpractice.kit;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A player's personal rearrangement of a base Kit (item positions swapped,
 * hotbar preference, etc). Multiple named layouts per kit are supported
 * (e.g. "NoDebuff Layout 1", "NoDebuff Layout 2").
 */
public class KitLayout {

    private final String kitId;
    private final String layoutName;
    private final Map<Integer, ItemStack> slots = new HashMap<>();

    public KitLayout(String kitId, String layoutName) {
        this.kitId = kitId;
        this.layoutName = layoutName;
    }

    public String getKitId() { return kitId; }
    public String getLayoutName() { return layoutName; }
    public Map<Integer, ItemStack> getSlots() { return slots; }
}
