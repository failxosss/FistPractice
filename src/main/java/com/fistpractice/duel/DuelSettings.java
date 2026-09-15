package com.fistpractice.duel;

public class DuelSettings {

    private String mode = "NODEBUFF";
    private String kit = "default";
    private String arena = null; // null = random
    private boolean ranked = false;
    private int bestOf = 1;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getKit() { return kit; }
    public void setKit(String kit) { this.kit = kit; }
    public String getArena() { return arena; }
    public void setArena(String arena) { this.arena = arena; }
    public boolean isRanked() { return ranked; }
    public void setRanked(boolean ranked) { this.ranked = ranked; }
    public int getBestOf() { return bestOf; }
    public void setBestOf(int bestOf) { this.bestOf = bestOf; }
}
