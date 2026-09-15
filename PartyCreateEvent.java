package com.fistpractice.events;

import com.fistpractice.party.Party;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PartyCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;

    public PartyCreateEvent(Party party) {
        this.party = party;
    }

    public Party getParty() { return party; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
