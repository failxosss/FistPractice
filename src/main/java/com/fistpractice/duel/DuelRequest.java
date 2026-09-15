package com.fistpractice.duel;

import java.util.UUID;

public class DuelRequest {

    private final UUID sender;
    private final UUID target;
    private final DuelSettings settings;
    private final long expiresAt;

    public DuelRequest(UUID sender, UUID target, DuelSettings settings, long ttlSeconds) {
        this.sender = sender;
        this.target = target;
        this.settings = settings;
        this.expiresAt = System.currentTimeMillis() + ttlSeconds * 1000L;
    }

    public UUID getSender() { return sender; }
    public UUID getTarget() { return target; }
    public DuelSettings getSettings() { return settings; }
    public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
}
