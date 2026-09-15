package com.fistpractice.history;

import java.util.UUID;

public class MatchHistoryEntry {

    private final UUID player;
    private final UUID opponent;
    private final String mode;
    private final String kit;
    private final String arena;
    private final boolean ranked;
    private final String result; // WIN | LOSS
    private final long durationMs;
    private int eloChange;
    private final long playedAt = System.currentTimeMillis();

    public MatchHistoryEntry(UUID player, UUID opponent, String mode, String kit, String arena,
                              boolean ranked, String result, long durationMs) {
        this.player = player;
        this.opponent = opponent;
        this.mode = mode;
        this.kit = kit;
        this.arena = arena;
        this.ranked = ranked;
        this.result = result;
        this.durationMs = durationMs;
    }

    public UUID getPlayer() { return player; }
    public UUID getOpponent() { return opponent; }
    public String getMode() { return mode; }
    public String getKit() { return kit; }
    public String getArena() { return arena; }
    public boolean isRanked() { return ranked; }
    public String getResult() { return result; }
    public long getDurationMs() { return durationMs; }
    public int getEloChange() { return eloChange; }
    public void setEloChange(int eloChange) { this.eloChange = eloChange; }
    public long getPlayedAt() { return playedAt; }
}
