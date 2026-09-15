package com.fistpractice.queue;

import com.fistpractice.FistPractice;
import com.fistpractice.configuration.GameMode;
import com.fistpractice.events.QueueJoinEvent;
import com.fistpractice.events.QueueLeaveEvent;
import com.fistpractice.kit.Kit;
import com.fistpractice.match.Match;
import com.fistpractice.match.MatchSide;
import com.fistpractice.match.MatchType;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One FIFO-ish pool per (mode, ranked) pair. A repeating task pairs up
 * compatible entries every second; entries whose ELO windows overlap are
 * matched first-come-first-served to keep things fair and simple.
 */
public class QueueManager {

    private final FistPractice plugin;
    // key: MODE#RANKED/UNRANKED
    private final Map<String, List<QueueEntry>> pools = new HashMap<>();
    private final Set<UUID> queuedPlayers = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public QueueManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private String key(String mode, boolean ranked) {
        return mode.toUpperCase() + "#" + (ranked ? "RANKED" : "UNRANKED");
    }

    public boolean isQueued(UUID uuid) {
        return queuedPlayers.contains(uuid);
    }

    public String joinSolo(Player player, String modeId, boolean ranked) {
        if (isQueued(player.getUniqueId())) return "queue.already-queued";
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) return "duel.already-in-duel";
        GameMode mode = plugin.getGameModeManager().get(modeId);
        if (mode == null) return "match.mode-disabled";

        QueueJoinEvent event = new QueueJoinEvent(player, modeId, ranked);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        int elo = plugin.getProfileManager().getProfile(player)
                .getModeStats(modeId, plugin.getRatingManager().getStartingElo()).getElo();

        QueueEntry entry = new QueueEntry(List.of(player.getUniqueId()), modeId.toUpperCase(), ranked, elo);
        pools.computeIfAbsent(key(modeId, ranked), k -> new CopyOnWriteArrayList<>()).add(entry);
        queuedPlayers.add(player.getUniqueId());

        MessageUtil.sendKey(player, "queue.joined", Map.of("mode", mode.getDisplayName(), "type", ranked ? "Ranked" : "Unranked"));
        return null;
    }

    /** Queues an entire party as one team - matched against another party/team of equal size. */
    public String joinParty(List<UUID> members, String modeId, boolean ranked) {
        for (UUID uuid : members) {
            if (isQueued(uuid) || plugin.getMatchManager().isInMatch(uuid)) return "duel.already-in-duel";
        }
        GameMode mode = plugin.getGameModeManager().get(modeId);
        if (mode == null) return "match.mode-disabled";

        int avgElo = (int) members.stream()
                .mapToInt(u -> {
                    var profile = plugin.getProfileManager().getProfile(u);
                    return profile == null ? plugin.getRatingManager().getStartingElo()
                            : profile.getModeStats(modeId, plugin.getRatingManager().getStartingElo()).getElo();
                }).average().orElse(plugin.getRatingManager().getStartingElo());

        QueueEntry entry = new QueueEntry(members, modeId.toUpperCase(), ranked, avgElo);
        pools.computeIfAbsent(key(modeId, ranked), k -> new CopyOnWriteArrayList<>()).add(entry);
        queuedPlayers.addAll(members);
        return null;
    }

    public void leave(UUID uuid) {
        for (List<QueueEntry> pool : pools.values()) {
            pool.removeIf(e -> {
                if (e.getMembers().contains(uuid)) {
                    queuedPlayers.removeAll(e.getMembers());
                    return true;
                }
                return false;
            });
        }
    }

    private void tick() {
        boolean expansionEnabled = plugin.getConfig().getBoolean("queue.ranked.expansion.enabled", true);
        int initial = plugin.getConfig().getInt("queue.ranked.initial-range", 50);
        int interval = plugin.getConfig().getInt("queue.ranked.expansion.interval-seconds", 30);
        int amount = plugin.getConfig().getInt("queue.ranked.expansion.amount", 50);

        for (Map.Entry<String, List<QueueEntry>> pool : pools.entrySet()) {
            List<QueueEntry> entries = pool.getValue();
            for (int i = 0; i < entries.size(); i++) {
                QueueEntry a = entries.get(i);
                if (a == null) continue;
                for (int j = i + 1; j < entries.size(); j++) {
                    QueueEntry b = entries.get(j);
                    if (b == null) continue;
                    if (a.getMembers().size() != b.getMembers().size()) continue; // keep team sizes matched
                    if (!a.isRanked()) {
                        // Unranked ignores elo range entirely.
                        pairUp(a, b, pool.getKey());
                        entries.set(i, null);
                        entries.set(j, null);
                        break;
                    }
                    int rangeA = a.getSearchRange(initial, expansionEnabled, interval, amount);
                    int rangeB = b.getSearchRange(initial, expansionEnabled, interval, amount);
                    int range = Math.max(rangeA, rangeB);
                    if (Math.abs(a.getAverageElo() - b.getAverageElo()) <= range) {
                        pairUp(a, b, pool.getKey());
                        entries.set(i, null);
                        entries.set(j, null);
                        break;
                    }
                }
            }
            entries.removeIf(Objects::isNull);
        }
    }

    private void pairUp(QueueEntry a, QueueEntry b, String poolKey) {
        String modeId = a.getMode();
        var mode = plugin.getGameModeManager().get(modeId);
        Kit kit = plugin.getKitManager().get("default");
        if (mode == null || kit == null) return;

        MatchSide sideA = new MatchSide("Team A");
        a.getMembers().forEach(sideA::addMember);
        MatchSide sideB = new MatchSide("Team B");
        b.getMembers().forEach(sideB::addMember);

        queuedPlayers.removeAll(a.getMembers());
        queuedPlayers.removeAll(b.getMembers());

        MatchType type = a.getMembers().size() > 1 ? MatchType.PARTY_VS_PARTY : MatchType.DUEL_1V1;
        Match match = plugin.getMatchManager().startMatch(type, mode, kit, List.of(sideA, sideB),
                plugin.getConfig().getInt("match.default-best-of", 1), a.isRanked(), false);

        String opponentNames = String.join(", ", namesOf(a.getMembers().size() == 1 ? b.getMembers() : a.getMembers()));
        for (UUID uuid : a.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtil.sendKey(p, "queue.match-found", Map.of("opponent", opponentNames));
        }
    }

    private List<String> namesOf(List<UUID> uuids) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : uuids) {
            Player p = Bukkit.getPlayer(uuid);
            names.add(p != null ? p.getName() : uuid.toString().substring(0, 8));
        }
        return names;
    }
}
