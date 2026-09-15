package com.fistpractice.arena;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.atomic.AtomicBoolean;

public class Arena {

    private final String name;
    private String worldName;
    private Location spawn1;
    private Location spawn2;
    private Location spectatorSpawn;
    private Location regionMin;
    private Location regionMax;
    private boolean enabled = true;
    private final AtomicBoolean inUse = new AtomicBoolean(false);
    private String modeId; // which game mode this arena is associated with (optional filter)

    public Arena(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public Location getSpawn1() { return spawn1; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public Location getSpawn2() { return spawn2; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public void setSpectatorSpawn(Location spectatorSpawn) { this.spectatorSpawn = spectatorSpawn; }
    public Location getRegionMin() { return regionMin; }
    public void setRegionMin(Location regionMin) { this.regionMin = regionMin; }
    public Location getRegionMax() { return regionMax; }
    public void setRegionMax(Location regionMax) { this.regionMax = regionMax; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getModeId() { return modeId; }
    public void setModeId(String modeId) { this.modeId = modeId; }

    public boolean tryReserve() {
        return inUse.compareAndSet(false, true);
    }

    public void release() {
        inUse.set(false);
    }

    public boolean isInUse() {
        return inUse.get();
    }

    public boolean isFullyConfigured() {
        return spawn1 != null && spawn2 != null;
    }

    public World getWorld() {
        return spawn1 != null ? spawn1.getWorld() : null;
    }
}
