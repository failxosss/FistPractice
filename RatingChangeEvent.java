package com.fistpractice.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class RatingChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final String mode;
    private final int before;
    private final int after;

    public RatingChangeEvent(UUID player, String mode, int before, int after) {
        this.player = player;
        this.mode = mode;
        this.before = before;
        this.after = after;
    }

    public UUID getPlayer() { return player; }
    public String getMode() { return mode; }
    public int getBefore() { return before; }
    public int getAfter() { return after; }
    public int getDelta() { return after - before; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
