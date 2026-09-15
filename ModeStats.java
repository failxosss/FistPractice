package com.fistpractice.profile;

/**
 * Independent rating + stat line for a single game mode (Classic, NoDebuff, ...).
 * Every mode has its own ELO, exactly as required by the ranked system spec.
 */
public class ModeStats {

    private final String mode;
    private int elo;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int currentStreak;
    private int bestStreak;
    private int matchesPlayed;
    private int placementMatches;

    public ModeStats(String mode, int startingElo) {
        this.mode = mode;
        this.elo = startingElo;
    }

    public String getMode() { return mode; }
    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public int getPlacementMatches() { return placementMatches; }

    public boolean isProvisional(int provisionalThreshold) {
        return placementMatches < provisionalThreshold;
    }

    public double getWinRate() {
        int total = wins + losses;
        return total == 0 ? 0.0 : (double) wins / total * 100.0;
    }

    public void recordResult(boolean won, int eloDelta, int killsGained, int deathsGained) {
        matchesPlayed++;
        placementMatches++;
        kills += killsGained;
        deaths += deathsGained;
        elo = Math.max(0, elo + eloDelta);
        if (won) {
            wins++;
            currentStreak = Math.max(0, currentStreak) + 1;
            bestStreak = Math.max(bestStreak, currentStreak);
        } else {
            losses++;
            currentStreak = 0;
        }
    }
}
