package com.fistpractice.utilities;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class PlayerStateSnapshot {

    private final Location location;
    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final ItemStack offHand;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final List<PotionEffect> potionEffects;
    private final float exp;
    private final int level;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;
    private final int fireTicks;
    private final Vector velocity;

    private PlayerStateSnapshot(Player p) {
        this.location = p.getLocation().clone();
        this.inventoryContents = p.getInventory().getContents().clone();
        this.armorContents = p.getInventory().getArmorContents().clone();
        this.offHand = p.getInventory().getItemInOffHand().clone();
        this.health = p.getHealth();
        this.foodLevel = p.getFoodLevel();
        this.saturation = p.getSaturation();
        this.potionEffects = new ArrayList<>(p.getActivePotionEffects());
        this.exp = p.getExp();
        this.level = p.getLevel();
        this.gameMode = p.getGameMode();
        this.allowFlight = p.getAllowFlight();
        this.flying = p.isFlying();
        this.fireTicks = p.getFireTicks();
        this.velocity = p.getVelocity().clone();
    }

    public static PlayerStateSnapshot capture(Player player) {
        return new PlayerStateSnapshot(player);
    }

    public void restore(Player p) {
        p.getInventory().setContents(inventoryContents);
        p.getInventory().setArmorContents(armorContents);
        p.getInventory().setItemInOffHand(offHand);
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : potionEffects) {
            p.addPotionEffect(effect);
        }
        p.setGameMode(gameMode);
        p.setHealth(Math.min(health, p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
        p.setFoodLevel(foodLevel);
        p.setSaturation(saturation);
        p.setExp(exp);
        p.setLevel(level);
        p.setAllowFlight(allowFlight);
        p.setFlying(flying);
        p.setFireTicks(fireTicks);
        p.setVelocity(velocity);
        p.teleport(location);
    }

    public Location getLocation() { return location; }
}
