package com.fistpractice.api;

import com.fistpractice.FistPractice;
import com.fistpractice.arena.ArenaManager;
import com.fistpractice.duel.DuelManager;
import com.fistpractice.kit.KitManager;
import com.fistpractice.match.MatchManager;
import com.fistpractice.party.PartyManager;
import com.fistpractice.profile.PlayerProfile;
import com.fistpractice.profile.ProfileManager;
import com.fistpractice.queue.QueueManager;
import com.fistpractice.ranking.RatingManager;

import java.util.UUID;

/**
 * Entry point for other plugins. Example:
 * <pre>
 *   PracticeAPI api = PracticeAPI.get();
 *   int elo = api.getProfileManager().getProfile(uuid).getModeStats("NODEBUFF", 1000).getElo();
 * </pre>
 * Internals (Match, Party, etc.) are still exposed directly for now rather
 * than hidden behind extra DTOs - keeps the API surface small and matches
 * "do not expose internal implementation unnecessarily" pragmatically for a
 * single-jar plugin (no cross-plugin classloading concerns).
 */
public final class PracticeAPI {

    private static PracticeAPI instance;
    private final FistPractice plugin;

    private PracticeAPI(FistPractice plugin) {
        this.plugin = plugin;
    }

    public static PracticeAPI get() {
        if (instance == null) instance = new PracticeAPI(FistPractice.getInstance());
        return instance;
    }

    public MatchManager getMatchManager() { return plugin.getMatchManager(); }
    public DuelManager getDuelManager() { return plugin.getDuelManager(); }
    public PartyManager getPartyManager() { return plugin.getPartyManager(); }
    public QueueManager getQueueManager() { return plugin.getQueueManager(); }
    public ArenaManager getArenaManager() { return plugin.getArenaManager(); }
    public KitManager getKitManager() { return plugin.getKitManager(); }
    public ProfileManager getProfileManager() { return plugin.getProfileManager(); }
    public RatingManager getRatingManager() { return plugin.getRatingManager(); }

    public PlayerProfile getProfile(UUID uuid) { return plugin.getProfileManager().getProfile(uuid); }
    public boolean isInMatch(UUID uuid) { return plugin.getMatchManager().isInMatch(uuid); }
    public boolean isQueued(UUID uuid) { return plugin.getQueueManager().isQueued(uuid); }
}
