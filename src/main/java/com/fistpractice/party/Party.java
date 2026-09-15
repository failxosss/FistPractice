package com.fistpractice.party;

import java.util.*;

public class Party {

    private final UUID id = UUID.randomUUID();
    private String name;
    private UUID leader;
    private final Set<UUID> moderators = new LinkedHashSet<>();
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> chatToggledOff = new HashSet<>(); // members who muted party chat
    private boolean friendlyFire = false;
    private int rating = 1000;

    public Party(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public Set<UUID> getModerators() { return moderators; }
    public Set<UUID> getMembers() { return members; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public PartyRole getRole(UUID uuid) {
        if (uuid.equals(leader)) return PartyRole.LEADER;
        if (moderators.contains(uuid)) return PartyRole.MODERATOR;
        if (members.contains(uuid)) return PartyRole.MEMBER;
        return null;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        moderators.remove(uuid);
    }

    public void promote(UUID uuid) {
        if (members.contains(uuid)) moderators.add(uuid);
    }

    public void demote(UUID uuid) {
        moderators.remove(uuid);
    }

    public boolean isChatMuted(UUID uuid) {
        return chatToggledOff.contains(uuid);
    }

    public void toggleChat(UUID uuid) {
        if (!chatToggledOff.remove(uuid)) chatToggledOff.add(uuid);
    }

    public int size() {
        return members.size();
    }

    /** true if a caller with this role may invite/kick/duel/queue per the permission matrix. */
    public boolean canManage(UUID uuid) {
        PartyRole role = getRole(uuid);
        return role == PartyRole.LEADER || role == PartyRole.MODERATOR;
    }

    public boolean canDisbandOrTransfer(UUID uuid) {
        return getRole(uuid) == PartyRole.LEADER;
    }
}
