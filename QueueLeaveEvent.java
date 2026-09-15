package com.fistpractice.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QueueLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String mode;

    public QueueLeaveEvent(Player player, String mode) {
        this.player = player;
        this.mode = mode;
    }

    public Player getPlayer() { return player; }
    public String getMode() { return mode; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
