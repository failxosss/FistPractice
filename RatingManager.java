package com.fistpractice.ranking;

import com.fistpractice.FistPractice;
import com.fistpractice.events.RatingChangeEvent;
import com.fistpractice.profile.ModeStats;
import com.fistpractice.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RatingManager {

    private final FistPractice plugin;
    private EloCalculator calculator;
    private final List<Division> divisions = new ArrayList<>();
    private int startingElo;

    public RatingManager(FistPractice plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        startingElo = plugin.getConfig().getInt("rating.starting-elo", 1000);
        int kProv = plugin.getConfig().getInt("rating.k-factor-provisional", 64);
        int kNorm = plugin.getConfig().getInt("rating.k-factor-normal", 32);
        int placementCount = plugin.getConfig().getInt("rating.provisional-matches", 10);
        this.calculator = new EloCalculator(kProv, kNorm, placementCount);

        divisions.clear();
        List<?> raw = plugin.getConfig().getList("rating.divisions");
        if (raw != null) {
            for (Object o : raw) {
                if (o instanceof ConfigurationSection) {
                    ConfigurationSection sec = (ConfigurationSection) o;
                    divisions.add(new Division(sec.getString("name"), sec.getInt("min-elo"), sec.getInt("sub-ranks", 1)));
                } else if (o instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) o;
                    divisions.add(new Division(
                            String.valueOf(map.get("name")),
                            Integer.parseInt(String.valueOf(map.get("min-elo"))),
                            map.containsKey("sub-ranks") ? Integer.parseInt(String.valueOf(map.get("sub-ranks"))) : 1
                    ));
                }
            }
        }
        divisions.sort((a, b) -> Integer.compare(a.getMinElo(), b.getMinElo()));
    }

    public int getStartingElo() {
        return startingElo;
    }

    public EloCalculator getCalculator() {
        return calculator;
    }

    public Division getDivisionForElo(int elo) {
        Division result = divisions.isEmpty() ? new Division("Unranked", 0, 1) : divisions.get(0);
        for (Division d : divisions) {
            if (elo >= d.getMinElo()) {
                result = d;
            }
        }
        return result;
    }

    public String getFormattedDivision(int elo) {
        Division current = getDivisionForElo(elo);
        int idx = divisions.indexOf(current);
        int nextMin = idx >= 0 && idx + 1 < divisions.size()
                ? divisions.get(idx + 1).getMinElo()
                : current.getMinElo() + 300;
        return current.render(elo, nextMin);
    }

    /**
     * Applies a ranked result to both players for a given mode, firing
     * RatingChangeEvent for each side, and returns the delta applied to the winner.
     */
    public int applyRankedResult(PlayerProfile winner, PlayerProfile loser, String mode) {
        ModeStats winnerStats = winner.getModeStats(mode, startingElo);
        ModeStats loserStats = loser.getModeStats(mode, startingElo);

        int winnerDelta = calculator.calculateDelta(true, winnerStats.getElo(), loserStats.getElo(), winnerStats.getPlacementMatches());
        int loserDelta = calculator.calculateDelta(false, loserStats.getElo(), winnerStats.getElo(), loserStats.getPlacementMatches());

        // Guarantee the winner never loses points and the loser never gains any,
        // which can otherwise happen against a much weaker opponent at the K extremes.
        winnerDelta = Math.max(1, winnerDelta);
        loserDelta = Math.min(-1, loserDelta);

        int winnerBefore = winnerStats.getElo();
        int loserBefore = loserStats.getElo();

        winnerStats.recordResult(true, winnerDelta, 0, 0);
        loserStats.recordResult(false, loserDelta, 0, 0);

        UUID winnerId = winner.getUuid();
        UUID loserId = loser.getUuid();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().callEvent(new RatingChangeEvent(winnerId, mode, winnerBefore, winnerStats.getElo()));
            Bukkit.getPluginManager().callEvent(new RatingChangeEvent(loserId, mode, loserBefore, loserStats.getElo()));
        });

        return winnerDelta;
    }
}
