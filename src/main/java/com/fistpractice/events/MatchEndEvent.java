package com.fistpractice.events;

import com.fistpractice.match.Match;
import com.fistpractice.match.MatchSide;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MatchEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Match match;
    private final MatchSide winner;

    public MatchEndEvent(Match match, MatchSide winner) {
        this.match = match;
        this.winner = winner;
    }

    public Match getMatch() { return match; }
    public MatchSide getWinner() { return winner; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
