package com.fistpractice.configuration;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Every game mode (Classic, NoDebuff, Boxing, Sumo, Bridge, ...) is data,
 * never a Java subclass - admins define new modes purely via YAML
 * (see /modes/*.yml). This satisfies "game modes must NOT be hard-coded".
 */
public class GameMode {

    private final String id;
    private String displayName;
    private boolean ranked;
    private double health;
    private int hunger;
    private int saturation;
    private boolean naturalRegeneration;
    private boolean fallDamage;
    private boolean fireDamage;
    private boolean allowBlockPlace;
    private boolean allowBlockBreak;
    private boolean allowEnderPearls;
    private boolean allowPotions;
    private boolean allowGoldenApples;
    private int minHeight = -64;
    private int maxHeight = 320;
    private int worldBorderRadius;
    private int timeLimitSeconds;
    private String knockbackProfile = "MODERN";
    private final Map<Integer, ItemStack> inventory = new HashMap<>();
    private final Map<String, ItemStack> armor = new HashMap<>();

    public GameMode(String id) {
        this.id = id.toUpperCase();
    }

    public static GameMode fromYaml(YamlConfiguration yaml) {
        GameMode mode = new GameMode(yaml.getString("id", "MODE"));
        mode.displayName = yaml.getString("display-name", mode.id);
        mode.ranked = yaml.getBoolean("ranked", true);
        mode.health = yaml.getDouble("health", 20);
        mode.hunger = yaml.getInt("hunger", 20);
        mode.saturation = yaml.getInt("saturation", 20);
        mode.naturalRegeneration = yaml.getBoolean("natural-regeneration", true);
        mode.fallDamage = yaml.getBoolean("fall-damage", true);
        mode.fireDamage = yaml.getBoolean("fire-damage", true);
        mode.allowBlockPlace = yaml.getBoolean("allow-block-place", false);
        mode.allowBlockBreak = yaml.getBoolean("allow-block-break", false);
        mode.allowEnderPearls = yaml.getBoolean("allow-ender-pearls", false);
        mode.allowPotions = yaml.getBoolean("allow-potions", false);
        mode.allowGoldenApples = yaml.getBoolean("allow-golden-apples", false);
        mode.minHeight = yaml.getInt("build-height.min", -64);
        mode.maxHeight = yaml.getInt("build-height.max", 320);
        mode.worldBorderRadius = yaml.getInt("world-border-radius", 0);
        mode.timeLimitSeconds = yaml.getInt("time-limit-seconds", 0);
        mode.knockbackProfile = yaml.getString("knockback-profile", "MODERN");

        if (yaml.isList("inventory")) {
            for (Object o : yaml.getList("inventory")) {
                if (o instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) o;
                    int slot = Integer.parseInt(String.valueOf(map.get("slot")));
                    Material mat = Material.matchMaterial(String.valueOf(map.get("item")));
                    if (mat == null) continue;
                    int amount = map.containsKey("amount") ? Integer.parseInt(String.valueOf(map.get("amount"))) : 1;
                    ItemStack stack = new ItemStack(mat, amount);
                    if (map.get("enchantments") instanceof Map) {
                        ItemMeta meta = stack.getItemMeta();
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) map.get("enchantments")).entrySet()) {
                            Enchantment ench = Enchantment.getByName(String.valueOf(entry.getKey()));
                            if (ench != null && meta != null) {
                                meta.addEnchant(ench, Integer.parseInt(String.valueOf(entry.getValue())), true);
                            }
                        }
                        stack.setItemMeta(meta);
                    }
                    mode.inventory.put(slot, stack);
                }
            }
        }

        if (yaml.isConfigurationSection("armor")) {
            for (String key : yaml.getConfigurationSection("armor").getKeys(false)) {
                Material mat = Material.matchMaterial(yaml.getString("armor." + key, ""));
                if (mat != null) mode.armor.put(key.toLowerCase(), new ItemStack(mat));
            }
        }
        return mode;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public boolean isRanked() { return ranked; }
    public double getHealth() { return health; }
    public int getHunger() { return hunger; }
    public int getSaturation() { return saturation; }
    public boolean isNaturalRegeneration() { return naturalRegeneration; }
    public boolean isFallDamage() { return fallDamage; }
    public boolean isFireDamage() { return fireDamage; }
    public boolean isAllowBlockPlace() { return allowBlockPlace; }
    public boolean isAllowBlockBreak() { return allowBlockBreak; }
    public boolean isAllowEnderPearls() { return allowEnderPearls; }
    public boolean isAllowPotions() { return allowPotions; }
    public boolean isAllowGoldenApples() { return allowGoldenApples; }
    public int getMinHeight() { return minHeight; }
    public int getMaxHeight() { return maxHeight; }
    public int getWorldBorderRadius() { return worldBorderRadius; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public String getKnockbackProfile() { return knockbackProfile; }
    public Map<Integer, ItemStack> getInventory() { return inventory; }
    public Map<String, ItemStack> getArmor() { return armor; }
}
