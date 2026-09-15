package com.fistpractice.kit;

import com.fistpractice.FistPractice;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class KitManager {

    private final FistPractice plugin;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    // uuid -> kitId -> layoutName -> layout
    private final Map<UUID, Map<String, Map<String, KitLayout>>> layouts = new ConcurrentHashMap<>();

    public KitManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public void loadKits() {
        kits.clear();
        File folder = new File(plugin.getDataFolder(), "kits");
        if (!folder.exists()) folder.mkdirs();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.saveResource("kits/default.yml", false);
            files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        }
        if (files == null) return;
        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                Kit kit = new Kit(yaml.getString("id", file.getName().replace(".yml", "")));
                kit.setDisplayName(yaml.getString("display-name", kit.getId()));
                kit.setHealth(yaml.getDouble("health", 20));
                kit.setHunger(yaml.getInt("hunger", 20));
                if (yaml.isList("items")) {
                    for (Object o : yaml.getList("items")) {
                        if (o instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) o;
                            int slot = Integer.parseInt(String.valueOf(map.get("slot")));
                            Material mat = Material.matchMaterial(String.valueOf(map.get("item")));
                            if (mat != null) kit.getItems().put(slot, new ItemStack(mat));
                        }
                    }
                }
                if (yaml.isConfigurationSection("armor")) {
                    for (String key : yaml.getConfigurationSection("armor").getKeys(false)) {
                        Material mat = Material.matchMaterial(yaml.getString("armor." + key, ""));
                        if (mat != null) kit.getArmor().put(key.toLowerCase(), new ItemStack(mat));
                    }
                }
                kits.put(kit.getId().toLowerCase(), kit);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load kit " + file.getName(), e);
            }
        }
    }

    public Kit get(String id) {
        return id == null ? null : kits.get(id.toLowerCase());
    }

    public boolean exists(String id) {
        return get(id) != null;
    }

    public Map<String, Kit> getAll() {
        return kits;
    }

    public void register(Kit kit) {
        kits.put(kit.getId().toLowerCase(), kit);
    }

    /** Applies a kit (or the player's active layout override, if present) to their inventory. */
    public void apply(Player player, String kitId) {
        Kit kit = get(kitId);
        if (kit == null) return;
        PlayerInventory inv = player.getInventory();
        inv.clear();
        kit.getItems().forEach((slot, item) -> inv.setItem(slot, item.clone()));
        ItemStack helmet = kit.getArmor().get("helmet");
        ItemStack chest = kit.getArmor().get("chestplate");
        ItemStack legs = kit.getArmor().get("leggings");
        ItemStack boots = kit.getArmor().get("boots");
        if (helmet != null) inv.setHelmet(helmet.clone());
        if (chest != null) inv.setChestplate(chest.clone());
        if (legs != null) inv.setLeggings(legs.clone());
        if (boots != null) inv.setBoots(boots.clone());
        player.setHealth(Math.min(kit.getHealth(), player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
        player.setFoodLevel(kit.getHunger());
    }

    /** Applies an already-resolved Kit object directly (used by the match engine). */
    public void applyKitFromMatch(Player player, Kit kit) {
        if (kit == null) return;
        PlayerInventory inv = player.getInventory();
        inv.clear();
        kit.getItems().forEach((slot, item) -> inv.setItem(slot, item.clone()));
        ItemStack helmet = kit.getArmor().get("helmet");
        ItemStack chest = kit.getArmor().get("chestplate");
        ItemStack legs = kit.getArmor().get("leggings");
        ItemStack boots = kit.getArmor().get("boots");
        if (helmet != null) inv.setHelmet(helmet.clone());
        if (chest != null) inv.setChestplate(chest.clone());
        if (legs != null) inv.setLeggings(legs.clone());
        if (boots != null) inv.setBoots(boots.clone());
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(kit.getHealth(), maxHealth));
        player.setFoodLevel(kit.getHunger());
    }

    public void saveLayout(UUID uuid, KitLayout layout) {
        layouts.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>())
                .computeIfAbsent(layout.getKitId(), k -> new ConcurrentHashMap<>())
                .put(layout.getLayoutName(), layout);
    }

    public KitLayout getLayout(UUID uuid, String kitId, String layoutName) {
        Map<String, Map<String, KitLayout>> perPlayer = layouts.get(uuid);
        if (perPlayer == null) return null;
        Map<String, KitLayout> perKit = perPlayer.get(kitId);
        return perKit == null ? null : perKit.get(layoutName);
    }

    public void deleteLayout(UUID uuid, String kitId, String layoutName) {
        Map<String, Map<String, KitLayout>> perPlayer = layouts.get(uuid);
        if (perPlayer == null) return;
        Map<String, KitLayout> perKit = perPlayer.get(kitId);
        if (perKit != null) perKit.remove(layoutName);
    }

    public Map<String, KitLayout> getLayouts(UUID uuid, String kitId) {
        Map<String, Map<String, KitLayout>> perPlayer = layouts.get(uuid);
        if (perPlayer == null) return new HashMap<>();
        return perPlayer.getOrDefault(kitId, new HashMap<>());
    }
}
