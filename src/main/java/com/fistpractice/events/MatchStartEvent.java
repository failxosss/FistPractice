package com.fistpractice.events;

import com.fistpractice.match.Match;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MatchStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Match match;

    public MatchStartEvent(Match match) {
        this.match = match;
    }

    public Match getMatch() { return match; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
