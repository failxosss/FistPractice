package com.fistpractice.match;

import com.fistpractice.FistPractice;
import com.fistpractice.arena.Arena;
import com.fistpractice.configuration.GameMode;
import com.fistpractice.events.MatchEndEvent;
import com.fistpractice.events.MatchStartEvent;
import com.fistpractice.history.MatchHistoryEntry;
import com.fistpractice.kit.Kit;
import com.fistpractice.profile.PlayerProfile;
import com.fistpractice.utilities.MessageUtil;
import com.fistpractice.utilities.PlayerStateSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THE single match engine (spec section 24). Player duels, party duels, team
 * duels and tournament matches all instantiate a {@link Match} and are driven
 * by this class - there is no parallel/duplicate implementation anywhere else.
 */
public class MatchManager {

    private final FistPractice plugin;
    private final Map<UUID, Match> matches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToMatch = new ConcurrentHashMap<>();
    private final Map<UUID, Long> disconnectedSince = new ConcurrentHashMap<>();

    public MatchManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public Match getMatch(UUID matchId) {
        return matches.get(matchId);
    }

    public Match getMatchOf(UUID playerId) {
        UUID id = playerToMatch.get(playerId);
        return id == null ? null : matches.get(id);
    }

    public boolean isInMatch(UUID playerId) {
        return playerToMatch.containsKey(playerId);
    }

    public Collection<Match> getActiveMatches() {
        return matches.values();
    }

    /**
     * Step 1-9 of the match flow: validate, reserve arena, save state,
     * teleport, apply kit, apply rules, freeze, countdown, start.
     */
    public Match startMatch(MatchType type, GameMode mode, Kit kit, List<MatchSide> sides,
                             int bestOf, boolean ranked, boolean friendlyFire) {
        Arena arena = plugin.getArenaManager().reserveArenaFor(mode.getId());
        if (arena == null) {
            for (MatchSide s : sides) notifyAll(s, "match.no-arena-available");
            return null;
        }

        Match match = new Match(type, mode, kit, arena, sides, bestOf, ranked, friendlyFire);
        matches.put(match.getId(), match);
        for (UUID uuid : match.getAllPlayers()) playerToMatch.put(uuid, match.getId());

        // Snapshot the arena once so every round after the first can be reset instantly.
        plugin.getArenaManager().captureSnapshot(arena);

        // Save every participant's current state before touching anything.
        for (UUID uuid : match.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) match.getSavedStates().put(uuid, PlayerStateSnapshot.capture(p));
        }

        beginRound(match, true);
        Bukkit.getPluginManager().callEvent(new MatchStartEvent(match));
        return match;
    }

    private void beginRound(Match match, boolean firstRound) {
        match.setState(MatchState.STARTING);
        Arena arena = match.getArena();

        int sideIndex = 0;
        for (MatchSide side : match.getSides()) {
            org.bukkit.Location spawn = sideIndex == 0 ? arena.getSpawn1() : arena.getSpawn2();
            for (UUID uuid : side.getMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                p.teleport(spawn);
                plugin.getKitManager().applyKitFromMatch(p, match.getKit());
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                p.setInvulnerable(true);
            }
            sideIndex++;
        }
        match.resetForNewRound();
        runCountdown(match);
    }

    private void runCountdown(Match match) {
        match.setState(MatchState.COUNTDOWN);
        int seconds = plugin.getConfig().getInt("match.countdown-seconds", 5);
        new BukkitRunnable() {
            int remaining = seconds;
            @Override
            public void run() {
                if (!matches.containsKey(match.getId())) { cancel(); return; }
                if (remaining <= 0) {
                    for (UUID uuid : match.getAllPlayers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.setInvulnerable(false);
                            MessageUtil.actionBar(p, "<green><bold>FIGHT!");
                        }
                    }
                    match.setState(MatchState.IN_PROGRESS);
                    match.markRoundStart();
                    cancel();
                    return;
                }
                for (UUID uuid : match.getAllPlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) MessageUtil.actionBar(p, "<yellow>" + remaining);
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /** Called by CombatListener when a participant dies or is otherwise eliminated. */
    public void eliminate(Match match, UUID uuid) {
        if (match.getState() != MatchState.IN_PROGRESS) return;
        MatchSide side = match.getSideOf(uuid);
        if (side == null) return;
        side.eliminate(uuid);

        MatchSide roundWinner = match.getRoundWinner();
        if (roundWinner != null) {
            handleRoundEnd(match, roundWinner);
        }
    }

    private void handleRoundEnd(Match match, MatchSide roundWinner) {
        match.setState(MatchState.ROUND_ENDED);
        match.getRoundWins().merge(roundWinner.getName(), 1, Integer::sum);

        for (UUID uuid : match.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                MessageUtil.actionBar(p, "<gold>" + roundWinner.getName() + " wins the round!");
            }
        }

        MatchSide matchWinner = match.getMatchWinner();
        if (matchWinner != null) {
            endMatch(match, matchWinner);
            return;
        }

        // Reset arena + players, then start the next round.
        plugin.getArenaManager().resetArena(match.getArena(), () ->
                Bukkit.getScheduler().runTask(plugin, () -> beginRound(match, false)));
    }

    private void endMatch(Match match, MatchSide winner) {
        match.setState(MatchState.ENDING);

        for (MatchSide side : match.getSides()) {
            boolean won = side == winner;
            for (UUID uuid : side.getMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    match.getSavedStates().getOrDefault(uuid, PlayerStateSnapshot.capture(p)).restore(p);
                    MessageUtil.send(p, won ? "<green>You won the match!" : "<red>You lost the match.");
                }
                playerToMatch.remove(uuid);
            }
        }

        // Rating + history only for genuine 1v1 ranked duels here; party/team
        // rating pools are applied separately by PartyManager-aware callers
        // using the same RatingManager (kept out of the engine to stay generic).
        if (match.isRanked() && match.getSides().size() == 2
                && match.getSides().get(0).getMembers().size() == 1
                && match.getSides().get(1).getMembers().size() == 1) {
            UUID winnerId = winner.getMembers().iterator().next();
            UUID loserId = match.getOpposingSide(winner).getMembers().iterator().next();
            PlayerProfile winnerProfile = plugin.getProfileManager().getProfile(winnerId);
            PlayerProfile loserProfile = plugin.getProfileManager().getProfile(loserId);
            if (winnerProfile != null && loserProfile != null) {
                plugin.getRatingManager().applyRankedResult(winnerProfile, loserProfile, match.getMode().getId());
                plugin.getProfileManager().saveProfile(winnerProfile);
                plugin.getProfileManager().saveProfile(loserProfile);
            }
            plugin.getHistoryManager().record(new MatchHistoryEntry(winnerId, loserId, match.getMode().getId(),
                    match.getKit().getId(), match.getArena().getName(), true, "WIN",
                    System.currentTimeMillis() - match.getCreatedAt()));
            plugin.getHistoryManager().record(new MatchHistoryEntry(loserId, winnerId, match.getMode().getId(),
                    match.getKit().getId(), match.getArena().getName(), true, "LOSS",
                    System.currentTimeMillis() - match.getCreatedAt()));
        }

        Bukkit.getPluginManager().callEvent(new MatchEndEvent(match, winner));

        // Cleanup - no match data survives past this point (spec section 60/26).
        match.getArena().release();
        matches.remove(match.getId());
        match.setState(MatchState.ENDED);
    }

    /** Forfeits every remaining member on a side immediately (combat log / disconnect timeout). */
    public void forfeitSide(Match match, MatchSide side) {
        for (UUID uuid : new ArrayList<>(side.getMembers())) {
            side.eliminate(uuid);
        }
        MatchSide winner = match.getRoundWinner();
        if (winner != null) handleRoundEnd(match, winner);
    }

    public void markDisconnected(UUID uuid) {
        disconnectedSince.put(uuid, System.currentTimeMillis());
        Match match = getMatchOf(uuid);
        if (match == null) return;
        int grace = plugin.getConfig().getInt("reconnect.grace-period-seconds", 15);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Long since = disconnectedSince.get(uuid);
            if (since == null) return; // reconnected already
            Match stillActive = getMatchOf(uuid);
            if (stillActive == null) return;
            MatchSide side = stillActive.getSideOf(uuid);
            if (side != null) {
                side.eliminate(uuid);
                MatchSide winner = stillActive.getRoundWinner();
                if (winner != null) handleRoundEnd(stillActive, winner);
            }
        }, grace * 20L);
    }

    public void markReconnected(UUID uuid) {
        disconnectedSince.remove(uuid);
    }

    private void notifyAll(MatchSide side, String messageKey) {
        for (UUID uuid : side.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtil.sendKey(p, messageKey);
        }
    }
}
