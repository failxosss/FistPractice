package com.fistpractice.queue;

import java.util.List;
import java.util.UUID;

public class QueueEntry {

    private final List<UUID> members; // 1 for solo queue, >1 for a party queue
    private final String mode;
    private final boolean ranked;
    private final int averageElo;
    private final long queuedAt = System.currentTimeMillis();

    public QueueEntry(List<UUID> members, String mode, boolean ranked, int averageElo) {
        this.members = members;
        this.mode = mode;
        this.ranked = ranked;
        this.averageElo = averageElo;
    }

    public List<UUID> getMembers() { return members; }
    public String getMode() { return mode; }
    public boolean isRanked() { return ranked; }
    public int getAverageElo() { return averageElo; }
    public long getQueuedAt() { return queuedAt; }

    public int getWaitSeconds() {
        return (int) ((System.currentTimeMillis() - queuedAt) / 1000);
    }

    /** Expanding ELO search range, widening the longer this entry waits. */
    public int getSearchRange(int initial, boolean expansionEnabled, int intervalSeconds, int amount) {
        if (!expansionEnabled) return initial;
        int steps = getWaitSeconds() / Math.max(1, intervalSeconds);
        return initial + steps * amount;
    }
}
