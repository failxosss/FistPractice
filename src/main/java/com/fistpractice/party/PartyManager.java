package com.fistpractice.party;

import com.fistpractice.FistPractice;
import com.fistpractice.events.PartyCreateEvent;
import com.fistpractice.events.PartyDisbandEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    private final FistPractice plugin;
    private final Map<UUID, Party> partiesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> memberToParty = new ConcurrentHashMap<>();
    private final Map<UUID, List<PartyInvite>> pendingInvites = new ConcurrentHashMap<>();

    public PartyManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public Party createParty(Player leader) {
        if (getParty(leader.getUniqueId()) != null) return null;
        Party party = new Party(leader.getName() + "'s Party", leader.getUniqueId());
        partiesById.put(party.getId(), party);
        memberToParty.put(leader.getUniqueId(), party.getId());
        Bukkit.getPluginManager().callEvent(new PartyCreateEvent(party));
        return party;
    }

    public void disbandParty(Party party) {
        for (UUID member : new ArrayList<>(party.getMembers())) {
            memberToParty.remove(member);
        }
        partiesById.remove(party.getId());
        Bukkit.getPluginManager().callEvent(new PartyDisbandEvent(party));
    }

    public Party getParty(UUID playerId) {
        UUID partyId = memberToParty.get(playerId);
        return partyId == null ? null : partiesById.get(partyId);
    }

    public boolean isInParty(UUID playerId) {
        return memberToParty.containsKey(playerId);
    }

    public int getMaxSizeFor(Player leader) {
        if (leader.hasPermission("fistpractice.party.size.32")) return 32;
        if (leader.hasPermission("fistpractice.party.size.16")) return 16;
        if (leader.hasPermission("fistpractice.party.size.8")) return 8;
        return plugin.getConfig().getInt("party.max-size", 8);
    }

    public boolean invite(Party party, Player inviter, UUID target) {
        if (isInParty(target)) return false;
        if (hasPendingInvite(party, target)) return false;
        int ttl = plugin.getConfig().getInt("party.invite-expiry-seconds", 30);
        pendingInvites.computeIfAbsent(target, u -> new ArrayList<>())
                .add(new PartyInvite(party.getId(), inviter.getUniqueId(), target, ttl));
        return true;
    }

    private boolean hasPendingInvite(Party party, UUID target) {
        List<PartyInvite> invites = pendingInvites.get(target);
        if (invites == null) return false;
        invites.removeIf(PartyInvite::isExpired);
        return invites.stream().anyMatch(i -> i.getPartyId().equals(party.getId()));
    }

    public PartyInvite consumeInvite(UUID target, UUID partyId) {
        List<PartyInvite> invites = pendingInvites.get(target);
        if (invites == null) return null;
        invites.removeIf(PartyInvite::isExpired);
        Optional<PartyInvite> found = invites.stream().filter(i -> i.getPartyId().equals(partyId)).findFirst();
        found.ifPresent(invites::remove);
        return found.orElse(null);
    }

    public List<PartyInvite> getInvites(UUID target) {
        List<PartyInvite> invites = pendingInvites.getOrDefault(target, Collections.emptyList());
        invites.removeIf(PartyInvite::isExpired);
        return invites;
    }

    public void addMember(Party party, UUID uuid) {
        party.addMember(uuid);
        memberToParty.put(uuid, party.getId());
    }

    public void removeMember(Party party, UUID uuid) {
        party.removeMember(uuid);
        memberToParty.remove(uuid);
        if (party.getLeader().equals(uuid)) {
            handleLeaderLeft(party);
        }
    }

    private void handleLeaderLeft(Party party) {
        String action = plugin.getConfig().getString("party.leader-leave-action", "TRANSFER");
        if (party.getMembers().isEmpty() || action.equalsIgnoreCase("DISBAND")) {
            disbandParty(party);
            return;
        }
        // Transfer to a moderator first, otherwise the longest-standing member.
        UUID next = party.getModerators().stream().findFirst()
                .orElse(party.getMembers().iterator().next());
        party.setLeader(next);
        party.getModerators().remove(next);
    }

    public Party findByName(String name) {
        return partiesById.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Collection<Party> getAll() {
        return partiesById.values();
    }

    /**
     * Splits a party's members into balanced teams for /party split.
     * strategy: RANDOM | MANUAL (returns a single-team placeholder - the GUI
     * handles manual assignment) | ELO_BALANCED (round-robins members sorted
     * by their NoDebuff/overall elo so both teams end up similar strength).
     */
    public List<List<UUID>> splitTeams(Party party, String strategy, int teamCount) {
        List<UUID> members = new ArrayList<>(party.getMembers());
        List<List<UUID>> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) teams.add(new ArrayList<>());

        if (strategy.equalsIgnoreCase("RANDOM")) {
            Collections.shuffle(members);
        } else if (strategy.equalsIgnoreCase("ELO_BALANCED")) {
            members.sort((a, b) -> {
                var pa = plugin.getProfileManager().getProfile(a);
                var pb = plugin.getProfileManager().getProfile(b);
                int eloA = pa == null ? 1000 : pa.getModeStats("CLASSIC", 1000).getElo();
                int eloB = pb == null ? 1000 : pb.getModeStats("CLASSIC", 1000).getElo();
                return Integer.compare(eloB, eloA);
            });
        }
        // snake-draft distribution keeps both random and elo-sorted lists balanced
        for (int i = 0; i < members.size(); i++) {
            int round = i / teamCount;
            int posInRound = i % teamCount;
            int teamIndex = (round % 2 == 0) ? posInRound : (teamCount - 1 - posInRound);
            teams.get(teamIndex).add(members.get(i));
        }
        return teams;
    }
}
