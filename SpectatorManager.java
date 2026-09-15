package com.fistpractice.spectator;

import com.fistpractice.FistPractice;
import com.fistpractice.match.Match;
import com.fistpractice.utilities.PlayerStateSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpectatorManager {

    private final FistPractice plugin;
    private final Map<UUID, UUID> spectatorToMatch = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerStateSnapshot> preSpectateState = new ConcurrentHashMap<>();

    public SpectatorManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public boolean isSpectating(UUID uuid) {
        return spectatorToMatch.containsKey(uuid);
    }

    public String join(Player spectator, Player target) {
        Match match = plugin.getMatchManager().getMatchOf(target.getUniqueId());
        if (match == null) return "match.no-arena-available"; // reuse generic "not available" key
        if (plugin.getMatchManager().isInMatch(spectator.getUniqueId())) return "duel.already-in-duel";

        preSpectateState.put(spectator.getUniqueId(), PlayerStateSnapshot.capture(spectator));
        spectatorToMatch.put(spectator.getUniqueId(), match.getId());
        match.getSpectators().add(spectator.getUniqueId());

        spectator.setGameMode(org.bukkit.GameMode.SPECTATOR);
        spectator.teleport(match.getArena().getSpectatorSpawn() != null
                ? match.getArena().getSpectatorSpawn() : target.getLocation());
        return null;
    }

    public void leave(Player spectator) {
        UUID matchId = spectatorToMatch.remove(spectator.getUniqueId());
        if (matchId != null) {
            Match match = plugin.getMatchManager().getMatch(matchId);
            if (match != null) match.getSpectators().remove(spectator.getUniqueId());
        }
        PlayerStateSnapshot snapshot = preSpectateState.remove(spectator.getUniqueId());
        if (snapshot != null) snapshot.restore(spectator);
        else spectator.setGameMode(org.bukkit.GameMode.SURVIVAL);
    }

    /** Cycle to the next player within the same match (spectator GUI "next"/"prev"). */
    public void cycleTo(Player spectator, Player newTarget) {
        UUID matchId = spectatorToMatch.get(spectator.getUniqueId());
        if (matchId == null) return;
        Match match = plugin.getMatchManager().getMatch(matchId);
        if (match == null || !match.getAllPlayers().contains(newTarget.getUniqueId())) return;
        spectator.teleport(newTarget.getLocation());
    }

    public List<Player> getSpectatablePlayers(Match match) {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : match.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) players.add(p);
        }
        return players;
    }

    public void removeMatchSpectators(Match match) {
        for (UUID uuid : new ArrayList<>(match.getSpectators())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) leave(p);
        }
    }
}
