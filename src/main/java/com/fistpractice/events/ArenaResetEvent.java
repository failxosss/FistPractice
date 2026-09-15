package com.fistpractice.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ArenaResetEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String arenaName;

    public ArenaResetEvent(String arenaName) {
        this.arenaName = arenaName;
    }

    public String getArenaName() { return arenaName; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
