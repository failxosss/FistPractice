package com.fistpractice.match;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class MatchSide {

    private final String name;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();

    public MatchSide(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public Set<UUID> getMembers() { return members; }
    public Set<UUID> getAlive() { return alive; }

    public void addMember(UUID uuid) {
        members.add(uuid);
        alive.add(uuid);
    }

    public void eliminate(UUID uuid) {
        alive.remove(uuid);
    }

    public boolean isEliminated() {
        return alive.isEmpty();
    }

    public void resetForNewRound() {
        alive.clear();
        alive.addAll(members);
    }
}
