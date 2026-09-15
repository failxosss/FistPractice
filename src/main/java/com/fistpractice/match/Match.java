package com.fistpractice.match;

import com.fistpractice.arena.Arena;
import com.fistpractice.configuration.GameMode;
import com.fistpractice.kit.Kit;
import com.fistpractice.utilities.PlayerStateSnapshot;

import java.util.*;

public class Match {

    private final UUID id = UUID.randomUUID();
    private final MatchType type;
    private final GameMode mode;
    private final Kit kit;
    private final Arena arena;
    private final List<MatchSide> sides;
    private final int bestOf;
    private final boolean ranked;
    private final boolean friendlyFire;
    private final Map<String, Integer> roundWins = new HashMap<>();
    private final Set<UUID> spectators = Collections.newSetFromMap(new HashMap<>());
    private final Map<UUID, PlayerStateSnapshot> savedStates = new HashMap<>();
    private final Map<UUID, UUID> disconnectedAt = new HashMap<>(); // uuid -> not used directly, timestamps tracked in listener
    private MatchState state = MatchState.STARTING;
    private final long createdAt = System.currentTimeMillis();
    private long roundStartedAt;

    public Match(MatchType type, GameMode mode, Kit kit, Arena arena, List<MatchSide> sides,
                 int bestOf, boolean ranked, boolean friendlyFire) {
        this.type = type;
        this.mode = mode;
        this.kit = kit;
        this.arena = arena;
        this.sides = sides;
        this.bestOf = bestOf;
        this.ranked = ranked;
        this.friendlyFire = friendlyFire;
        for (MatchSide side : sides) roundWins.put(side.getName(), 0);
    }

    public UUID getId() { return id; }
    public MatchType getType() { return type; }
    public GameMode getMode() { return mode; }
    public Kit getKit() { return kit; }
    public Arena getArena() { return arena; }
    public List<MatchSide> getSides() { return sides; }
    public int getBestOf() { return bestOf; }
    public boolean isRanked() { return ranked; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public Map<String, Integer> getRoundWins() { return roundWins; }
    public Set<UUID> getSpectators() { return spectators; }
    public Map<UUID, PlayerStateSnapshot> getSavedStates() { return savedStates; }
    public MatchState getState() { return state; }
    public void setState(MatchState state) { this.state = state; }
    public long getCreatedAt() { return createdAt; }
    public long getRoundStartedAt() { return roundStartedAt; }
    public void markRoundStart() { this.roundStartedAt = System.currentTimeMillis(); }

    public int getRequiredWins() {
        return (bestOf / 2) + 1;
    }

    public boolean isAllPlayers(int expected) {
        return getAllPlayers().size() == expected;
    }

    public Set<UUID> getAllPlayers() {
        Set<UUID> all = new LinkedHashSet<>();
        for (MatchSide side : sides) all.addAll(side.getMembers());
        return all;
    }

    public MatchSide getSideOf(UUID uuid) {
        for (MatchSide side : sides) {
            if (side.getMembers().contains(uuid)) return side;
        }
        return null;
    }

    public MatchSide getOpposingSide(MatchSide side) {
        for (MatchSide s : sides) {
            if (s != side) return s;
        }
        return null;
    }

    /** @return the winning side of the CURRENT round, or null if still contested. */
    public MatchSide getRoundWinner() {
        List<MatchSide> stillAlive = new ArrayList<>();
        for (MatchSide side : sides) {
            if (!side.isEliminated()) stillAlive.add(side);
        }
        return stillAlive.size() == 1 ? stillAlive.get(0) : null;
    }

    /** @return the overall match winner once one side has reached the required round wins. */
    public MatchSide getMatchWinner() {
        for (MatchSide side : sides) {
            if (roundWins.getOrDefault(side.getName(), 0) >= getRequiredWins()) return side;
        }
        return null;
    }

    public void resetForNewRound() {
        for (MatchSide side : sides) side.resetForNewRound();
    }
}
