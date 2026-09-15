package com.fistpractice.profile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerProfile {

    private final UUID uuid;
    private String name;
    private long firstJoin;
    private long lastSeen;
    private final Map<String, ModeStats> modeStats = new ConcurrentHashMap<>();

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = this.firstJoin;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getFirstJoin() { return firstJoin; }
    public void setFirstJoin(long firstJoin) { this.firstJoin = firstJoin; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public ModeStats getModeStats(String mode, int startingElo) {
        return modeStats.computeIfAbsent(mode.toUpperCase(), m -> new ModeStats(m, startingElo));
    }

    public Map<String, ModeStats> getAllModeStats() {
        return modeStats;
    }

    public int getTotalWins() {
        return modeStats.values().stream().mapToInt(ModeStats::getWins).sum();
    }

    public int getTotalLosses() {
        return modeStats.values().stream().mapToInt(ModeStats::getLosses).sum();
    }

    public int getTotalMatches() {
        return modeStats.values().stream().mapToInt(ModeStats::getMatchesPlayed).sum();
    }
}
