package com.fistpractice.duel;

import com.fistpractice.FistPractice;
import com.fistpractice.arena.Arena;
import com.fistpractice.configuration.GameMode;
import com.fistpractice.events.DuelAcceptEvent;
import com.fistpractice.events.DuelRequestEvent;
import com.fistpractice.kit.Kit;
import com.fistpractice.match.Match;
import com.fistpractice.match.MatchSide;
import com.fistpractice.match.MatchType;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private final FistPractice plugin;
    // target -> list of incoming requests
    private final Map<UUID, List<DuelRequest>> incoming = new ConcurrentHashMap<>();

    public DuelManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public String canRequest(Player sender, Player target) {
        if (sender.getUniqueId().equals(target.getUniqueId())
                && !plugin.getConfig().getBoolean("duel.allow-self-duel", false)) {
            return "duel.cannot-duel-self";
        }
        if (plugin.getMatchManager().isInMatch(sender.getUniqueId())) return "duel.already-in-duel";
        if (plugin.getMatchManager().isInMatch(target.getUniqueId())) return "duel.already-in-duel";
        if (hasPendingRequestBetween(sender.getUniqueId(), target.getUniqueId())) return "duel.already-requested";
        return null;
    }

    private boolean hasPendingRequestBetween(UUID sender, UUID target) {
        List<DuelRequest> list = incoming.get(target);
        if (list == null) return false;
        list.removeIf(DuelRequest::isExpired);
        return list.stream().anyMatch(r -> r.getSender().equals(sender));
    }

    public void sendRequest(Player sender, Player target, DuelSettings settings) {
        DuelRequestEvent event = new DuelRequestEvent(sender, target);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        int ttl = plugin.getConfig().getInt("duel.request-expiry-seconds", 30);
        DuelRequest request = new DuelRequest(sender.getUniqueId(), target.getUniqueId(), settings, ttl);
        incoming.computeIfAbsent(target.getUniqueId(), u -> new ArrayList<>()).add(request);

        MessageUtil.sendKey(sender, "duel.request-sent", Map.of("target", target.getName()));
        MessageUtil.sendKey(target, "duel.request-received", Map.of(
                "sender", sender.getName(),
                "mode", settings.getMode(),
                "kit", settings.getKit(),
                "bestOf", String.valueOf(settings.getBestOf())
        ));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<DuelRequest> list = incoming.get(target.getUniqueId());
            if (list != null && list.remove(request)) {
                Player s = Bukkit.getPlayer(sender.getUniqueId());
                if (s != null) MessageUtil.sendKey(s, "duel.request-expired", Map.of("target", target.getName()));
            }
        }, ttl * 20L);
    }

    public DuelRequest accept(Player target, UUID senderId) {
        List<DuelRequest> list = incoming.get(target.getUniqueId());
        if (list == null) return null;
        list.removeIf(DuelRequest::isExpired);
        Optional<DuelRequest> found = list.stream().filter(r -> r.getSender().equals(senderId)).findFirst();
        found.ifPresent(list::remove);
        DuelRequest request = found.orElse(null);
        if (request == null) return null;

        Player sender = Bukkit.getPlayer(senderId);
        if (sender != null) {
            Bukkit.getPluginManager().callEvent(new DuelAcceptEvent(sender, target));
            beginDuel(sender, target, request.getSettings());
        }
        return request;
    }

    public boolean decline(Player target, UUID senderId) {
        List<DuelRequest> list = incoming.get(target.getUniqueId());
        if (list == null) return false;
        Optional<DuelRequest> found = list.stream().filter(r -> r.getSender().equals(senderId)).findFirst();
        found.ifPresent(list::remove);
        if (found.isPresent()) {
            Player sender = Bukkit.getPlayer(senderId);
            if (sender != null) MessageUtil.sendKey(sender, "duel.request-declined", Map.of("target", target.getName()));
            return true;
        }
        return false;
    }

    public List<DuelRequest> getIncoming(UUID target) {
        List<DuelRequest> list = incoming.getOrDefault(target, Collections.emptyList());
        list.removeIf(DuelRequest::isExpired);
        return list;
    }

    private void beginDuel(Player a, Player b, DuelSettings settings) {
        GameMode mode = plugin.getGameModeManager().get(settings.getMode());
        if (mode == null) {
            MessageUtil.sendKey(a, "match.mode-disabled");
            MessageUtil.sendKey(b, "match.mode-disabled");
            return;
        }
        Kit kit = plugin.getKitManager().get(settings.getKit());
        if (kit == null) {
            MessageUtil.sendKey(a, "match.kit-not-found");
            MessageUtil.sendKey(b, "match.kit-not-found");
            return;
        }

        MatchSide sideA = new MatchSide(a.getName());
        sideA.addMember(a.getUniqueId());
        MatchSide sideB = new MatchSide(b.getName());
        sideB.addMember(b.getUniqueId());

        Match match = plugin.getMatchManager().startMatch(
                MatchType.DUEL_1V1, mode, kit, List.of(sideA, sideB),
                settings.getBestOf(), settings.isRanked() && mode.isRanked(), false);

        if (match == null) return; // no arena available - message already sent by MatchManager
    }
}
