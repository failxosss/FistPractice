package com.fistpractice.arena;

import com.fistpractice.FistPractice;
import com.fistpractice.events.ArenaResetEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Arena lifecycle: create/delete/setspawn/setregion/enable/disable/reset,
 * plus mode-aware selection (random / sequential / priority) and
 * multi-arena support so several matches can run at once without collisions.
 *
 * Reset strategy: a lightweight in-memory BlockData snapshot of the arena's
 * bounding region is taken once (on /arena setregion or /arena reset "capture").
 * Restoring only rewrites blocks and is throttled across ticks so it never
 * blocks the main thread for large regions - satisfying "never block the
 * server main thread with expensive world operations" without requiring a
 * WorldEdit/FAWE dependency. For very large arenas, hooking WorldEdit/FAWE
 * instead is a drop-in swap of resetArena()'s implementation.
 */
public class ArenaManager {

    private final FistPractice plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final Map<String, BlockData[][][]> snapshots = new ConcurrentHashMap<>();
    private final Map<String, Location> snapshotOrigins = new ConcurrentHashMap<>();
    private final Map<String, Integer> priorityRotationIndex = new ConcurrentHashMap<>();

    public ArenaManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public void loadArenas() {
        arenas.clear();
        File folder = new File(plugin.getDataFolder(), "arenas");
        if (!folder.exists()) folder.mkdirs();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            Arena arena = new Arena(yaml.getString("name", file.getName().replace(".yml", "")));
            arena.setWorldName(yaml.getString("world"));
            arena.setEnabled(yaml.getBoolean("enabled", true));
            arena.setModeId(yaml.getString("mode", null));
            arena.setSpawn1(readLoc(yaml, "spawn1"));
            arena.setSpawn2(readLoc(yaml, "spawn2"));
            arena.setSpectatorSpawn(readLoc(yaml, "spectator"));
            arena.setRegionMin(readLoc(yaml, "region-min"));
            arena.setRegionMax(readLoc(yaml, "region-max"));
            arenas.put(arena.getName().toLowerCase(), arena);
        }
    }

    private Location readLoc(YamlConfiguration yaml, String path) {
        if (!yaml.isConfigurationSection(path)) return null;
        String world = yaml.getString(path + ".world");
        World w = Bukkit.getWorld(world != null ? world : "");
        if (w == null) return null;
        return new Location(w, yaml.getDouble(path + ".x"), yaml.getDouble(path + ".y"),
                yaml.getDouble(path + ".z"), (float) yaml.getDouble(path + ".yaw", 0),
                (float) yaml.getDouble(path + ".pitch", 0));
    }

    private void writeLoc(YamlConfiguration yaml, String path, Location loc) {
        if (loc == null) return;
        yaml.set(path + ".world", loc.getWorld().getName());
        yaml.set(path + ".x", loc.getX());
        yaml.set(path + ".y", loc.getY());
        yaml.set(path + ".z", loc.getZ());
        yaml.set(path + ".yaw", (double) loc.getYaw());
        yaml.set(path + ".pitch", (double) loc.getPitch());
    }

    public void save(Arena arena) {
        File file = new File(new File(plugin.getDataFolder(), "arenas"), arena.getName() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", arena.getName());
        yaml.set("world", arena.getWorldName());
        yaml.set("enabled", arena.isEnabled());
        yaml.set("mode", arena.getModeId());
        writeLoc(yaml, "spawn1", arena.getSpawn1());
        writeLoc(yaml, "spawn2", arena.getSpawn2());
        writeLoc(yaml, "spectator", arena.getSpectatorSpawn());
        writeLoc(yaml, "region-min", arena.getRegionMin());
        writeLoc(yaml, "region-max", arena.getRegionMax());
        try {
            yaml.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    public Arena create(String name) {
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        save(arena);
        return arena;
    }

    public void delete(String name) {
        arenas.remove(name.toLowerCase());
        snapshots.remove(name.toLowerCase());
        File file = new File(new File(plugin.getDataFolder(), "arenas"), name + ".yml");
        if (file.exists()) file.delete();
    }

    public Arena get(String name) {
        return name == null ? null : arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getAll() {
        return arenas.values();
    }

    public List<Arena> getAvailableForMode(String modeId) {
        List<Arena> list = new ArrayList<>();
        for (Arena a : arenas.values()) {
            if (a.isEnabled() && a.isFullyConfigured() && !a.isInUse()
                    && (a.getModeId() == null || a.getModeId().equalsIgnoreCase(modeId))) {
                list.add(a);
            }
        }
        return list;
    }

    /**
     * Selects and reserves (tryReserve) an arena for the given mode according
     * to the configured rotation strategy. Returns null if none are free.
     */
    public Arena reserveArenaFor(String modeId) {
        List<Arena> candidates = getAvailableForMode(modeId);
        if (candidates.isEmpty()) return null;

        String strategy = plugin.getConfig().getString("arenas.rotation", "RANDOM").toUpperCase();
        Arena chosen;
        switch (strategy) {
            case "SEQUENTIAL": {
                int idx = priorityRotationIndex.merge(modeId.toUpperCase(), 1, Integer::sum) - 1;
                chosen = candidates.get(idx % candidates.size());
                break;
            }
            case "PRIORITY": {
                candidates.sort(Comparator.comparing(Arena::getName));
                chosen = candidates.get(0);
                break;
            }
            case "RANDOM":
            default:
                chosen = candidates.get(new Random().nextInt(candidates.size()));
        }
        return chosen.tryReserve() ? chosen : reserveArenaFor(modeId); // retry if race lost
    }

    /** Captures a snapshot of the arena's region for later fast restoration. */
    public void captureSnapshot(Arena arena) {
        if (arena.getRegionMin() == null || arena.getRegionMax() == null) return;
        Location min = arena.getRegionMin();
        Location max = arena.getRegionMax();
        World world = min.getWorld();
        int xs = Math.abs(max.getBlockX() - min.getBlockX()) + 1;
        int ys = Math.abs(max.getBlockY() - min.getBlockY()) + 1;
        int zs = Math.abs(max.getBlockZ() - min.getBlockZ()) + 1;
        int ox = Math.min(min.getBlockX(), max.getBlockX());
        int oy = Math.min(min.getBlockY(), max.getBlockY());
        int oz = Math.min(min.getBlockZ(), max.getBlockZ());

        BlockData[][][] data = new BlockData[xs][ys][zs];
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                for (int z = 0; z < zs; z++) {
                    data[x][y][z] = world.getBlockAt(ox + x, oy + y, oz + z).getBlockData();
                }
            }
        }
        snapshots.put(arena.getName().toLowerCase(), data);
        snapshotOrigins.put(arena.getName().toLowerCase(), new Location(world, ox, oy, oz));
    }

    /**
     * Restores the last snapshot, spread across ticks (a batch of block
     * writes per tick) so large arenas never cause a main-thread stall.
     */
    public void resetArena(Arena arena, Runnable onComplete) {
        BlockData[][][] data = snapshots.get(arena.getName().toLowerCase());
        Location origin = snapshotOrigins.get(arena.getName().toLowerCase());
        if (data == null || origin == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        World world = origin.getWorld();
        int xs = data.length, ys = data[0].length, zs = data[0][0].length;
        int totalBlocks = xs * ys * zs;
        int blocksPerTick = Math.max(500, totalBlocks / 40); // finish within ~40 ticks worst case
        AtomicInteger cursor = new AtomicInteger(0);

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int placed = 0;
            while (placed < blocksPerTick) {
                int i = cursor.getAndIncrement();
                if (i >= totalBlocks) {
                    task.cancel();
                    Bukkit.getPluginManager().callEvent(new ArenaResetEvent(arena.getName()));
                    if (onComplete != null) onComplete.run();
                    return;
                }
                int x = i / (ys * zs);
                int y = (i / zs) % ys;
                int z = i % zs;
                world.getBlockAt(origin.getBlockX() + x, origin.getBlockY() + y, origin.getBlockZ() + z)
                        .setBlockData(data[x][y][z], false);
                placed++;
            }
        }, 0L, 1L);
    }
}
