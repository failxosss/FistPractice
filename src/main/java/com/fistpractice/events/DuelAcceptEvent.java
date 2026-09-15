package com.fistpractice.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DuelAcceptEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player sender;
    private final Player target;

    public DuelAcceptEvent(Player sender, Player target) {
        this.sender = sender;
        this.target = target;
    }

    public Player getSender() { return sender; }
    public Player getTarget() { return target; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
