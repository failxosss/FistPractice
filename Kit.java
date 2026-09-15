package com.fistpractice.kit;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Kit {

    private final String id;
    private String displayName;
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Map<String, ItemStack> armor = new HashMap<>();
    private double health = 20;
    private int hunger = 20;

    public Kit(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName == null ? id : displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Map<Integer, ItemStack> getItems() { return items; }
    public Map<String, ItemStack> getArmor() { return armor; }
    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }
    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = hunger; }

    public Kit copy() {
        Kit clone = new Kit(id);
        clone.displayName = displayName;
        clone.health = health;
        clone.hunger = hunger;
        for (Map.Entry<Integer, ItemStack> e : items.entrySet()) clone.items.put(e.getKey(), e.getValue().clone());
        for (Map.Entry<String, ItemStack> e : armor.entrySet()) clone.armor.put(e.getKey(), e.getValue().clone());
        return clone;
    }
}
