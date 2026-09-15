package com.fistpractice.tournament;

import com.fistpractice.FistPractice;
import com.fistpractice.configuration.GameMode;
import com.fistpractice.events.MatchEndEvent;
import com.fistpractice.kit.Kit;
import com.fistpractice.match.Match;
import com.fistpractice.match.MatchSide;
import com.fistpractice.match.MatchType;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TournamentManager implements Listener {

    private final FistPractice plugin;
    private final Map<String, Tournament> tournaments = new ConcurrentHashMap<>();
    // matchId -> tournament id, so MatchEndEvent can advance the right bracket
    private final Map<UUID, String> matchToTournament = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> roundWinners = new ConcurrentHashMap<>();

    public TournamentManager(FistPractice plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Tournament create(String id, Tournament.Format format, String mode, String kit) {
        Tournament t = new Tournament(id, format, mode, kit);
        tournaments.put(id.toLowerCase(), t);
        return t;
    }

    public Tournament get(String id) {
        return id == null ? null : tournaments.get(id.toLowerCase());
    }

    public Collection<Tournament> getAll() {
        return tournaments.values();
    }

    public String join(Tournament t, UUID uuid) {
        if (t.getStatus() != Tournament.Status.OPEN) return "Tournament has already started.";
        return t.join(uuid) ? null : "You are already registered.";
    }

    public String leave(Tournament t, UUID uuid) {
        return t.leave(uuid) ? null : "You are not registered for that tournament.";
    }

    public String start(Tournament t) {
        if (t.getStatus() != Tournament.Status.OPEN) return "Tournament already started.";
        if (t.getParticipants().size() < 2) return "Not enough participants.";
        t.setStatus(Tournament.Status.IN_PROGRESS);
        launchRound(t, t.generateFirstRound());
        return null;
    }

    public void stop(Tournament t) {
        t.setStatus(Tournament.Status.FINISHED);
        tournaments.remove(t.getId().toLowerCase());
    }

    private void launchRound(Tournament t, List<UUID[]> pairings) {
        List<UUID> advancing = new ArrayList<>();
        roundWinners.put(t.getId(), advancing);

        GameMode mode = plugin.getGameModeManager().get(t.getMode());
        Kit kit = plugin.getKitManager().get(t.getKit());
        if (mode == null || kit == null) {
            t.setStatus(Tournament.Status.FINISHED);
            return;
        }

        for (UUID[] pair : pairings) {
            if (pair[1] == null) {
                // bye - auto-advance
                advancing.add(pair[0]);
                continue;
            }
            Player p1 = Bukkit.getPlayer(pair[0]);
            Player p2 = Bukkit.getPlayer(pair[1]);
            if (p1 == null || p2 == null) {
                // whoever is online (or neither) auto-advances/forfeits
                if (p1 != null) advancing.add(pair[0]);
                else if (p2 != null) advancing.add(pair[1]);
                continue;
            }
            MatchSide sideA = new MatchSide(p1.getName());
            sideA.addMember(p1.getUniqueId());
            MatchSide sideB = new MatchSide(p2.getName());
            sideB.addMember(p2.getUniqueId());

            Match match = plugin.getMatchManager().startMatch(MatchType.TOURNAMENT, mode, kit,
                    List.of(sideA, sideB), 1, false, false);
            if (match != null) {
                matchToTournament.put(match.getId(), t.getId());
            }
        }

        maybeAdvance(t);
    }

    @EventHandler
    public void onMatchEnd(MatchEndEvent event) {
        String tournamentId = matchToTournament.remove(event.getMatch().getId());
        if (tournamentId == null) return;
        Tournament t = get(tournamentId);
        if (t == null) return;

        UUID winnerId = event.getWinner().getMembers().iterator().next();
        roundWinners.computeIfAbsent(t.getId(), k -> new ArrayList<>()).add(winnerId);
        maybeAdvance(t);
    }

    /** Once every match in the current round has reported a winner, either finish or start the next round. */
    private void maybeAdvance(Tournament t) {
        int expected = t.getRounds().isEmpty() ? 0 : t.getRounds().get(t.getRounds().size() - 1).size();
        List<UUID> winners = roundWinners.getOrDefault(t.getId(), Collections.emptyList());
        if (winners.size() < expected) return; // still waiting on other matches

        if (winners.size() == 1) {
            t.setWinner(winners.get(0));
            t.setStatus(Tournament.Status.FINISHED);
            Player winner = Bukkit.getPlayer(winners.get(0));
            if (winner != null) MessageUtil.send(winner, "<gold>You won the tournament " + t.getId() + "!");
            plugin.getDiscordWebhook().sendTournamentResult(t.getId(), winner != null ? winner.getName() : winners.get(0).toString());
            return;
        }
        launchRound(t, t.generateNextRound(winners));
    }
}
