package com.fistpractice.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QueueJoinEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String mode;
    private final boolean ranked;
    private boolean cancelled;

    public QueueJoinEvent(Player player, String mode, boolean ranked) {
        this.player = player;
        this.mode = mode;
        this.ranked = ranked;
    }

    public Player getPlayer() { return player; }
    public String getMode() { return mode; }
    public boolean isRanked() { return ranked; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
