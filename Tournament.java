package com.fistpractice.tournament;

import java.util.*;

public class Tournament {

    public enum Format { SINGLE_ELIMINATION, DOUBLE_ELIMINATION, ROUND_ROBIN }
    public enum Status { OPEN, IN_PROGRESS, FINISHED }

    private final String id;
    private final Format format;
    private final String mode;
    private final String kit;
    private final List<UUID> participants = new ArrayList<>();
    private final List<List<UUID[]>> rounds = new ArrayList<>(); // each round: list of [p1, p2] pairings
    private Status status = Status.OPEN;
    private UUID winner;

    public Tournament(String id, Format format, String mode, String kit) {
        this.id = id;
        this.format = format;
        this.mode = mode;
        this.kit = kit;
    }

    public String getId() { return id; }
    public Format getFormat() { return format; }
    public String getMode() { return mode; }
    public String getKit() { return kit; }
    public List<UUID> getParticipants() { return participants; }
    public List<List<UUID[]>> getRounds() { return rounds; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }

    public boolean join(UUID uuid) {
        if (status != Status.OPEN || participants.contains(uuid)) return false;
        participants.add(uuid);
        return true;
    }

    public boolean leave(UUID uuid) {
        return status == Status.OPEN && participants.remove(uuid);
    }

    /** Builds the first round bracket. Odd participant counts get one bye. */
    public List<UUID[]> generateFirstRound() {
        List<UUID> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled);
        List<UUID[]> pairings = new ArrayList<>();
        for (int i = 0; i + 1 < shuffled.size(); i += 2) {
            pairings.add(new UUID[]{shuffled.get(i), shuffled.get(i + 1)});
        }
        if (shuffled.size() % 2 != 0) {
            // last player gets a bye - represented as a pairing with a null opponent
            pairings.add(new UUID[]{shuffled.get(shuffled.size() - 1), null});
        }
        rounds.add(pairings);
        return pairings;
    }

    public List<UUID[]> generateNextRound(List<UUID> winners) {
        List<UUID> shuffled = new ArrayList<>(winners);
        List<UUID[]> pairings = new ArrayList<>();
        for (int i = 0; i + 1 < shuffled.size(); i += 2) {
            pairings.add(new UUID[]{shuffled.get(i), shuffled.get(i + 1)});
        }
        if (shuffled.size() % 2 != 0) {
            pairings.add(new UUID[]{shuffled.get(shuffled.size() - 1), null});
        }
        rounds.add(pairings);
        return pairings;
    }
}
