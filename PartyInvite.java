package com.fistpractice.party;

import java.util.UUID;

public class PartyInvite {

    private final UUID partyId;
    private final UUID invitedBy;
    private final UUID target;
    private final long expiresAt;

    public PartyInvite(UUID partyId, UUID invitedBy, UUID target, long ttlSeconds) {
        this.partyId = partyId;
        this.invitedBy = invitedBy;
        this.target = target;
        this.expiresAt = System.currentTimeMillis() + ttlSeconds * 1000L;
    }

    public UUID getPartyId() { return partyId; }
    public UUID getInvitedBy() { return invitedBy; }
    public UUID getTarget() { return target; }
    public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
}
