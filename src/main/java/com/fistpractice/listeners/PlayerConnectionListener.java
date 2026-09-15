package com.fistpractice.listeners;

import com.fistpractice.FistPractice;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final FistPractice plugin;

    public PlayerConnectionListener(FistPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        plugin.getProfileManager().loadProfile(player.getUniqueId(), player.getName());
        plugin.getMatchManager().markReconnected(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();

        // Queue / spectator cleanup - these should never "hold" a slot for an offline player.
        plugin.getQueueManager().leave(player.getUniqueId());
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            plugin.getSpectatorManager().leave(player);
        }

        // Combat log / reconnect handling is delegated to MatchManager so the
        // grace-period timer & forfeit logic lives in one place (spec 39/40).
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) {
            plugin.getMatchManager().markDisconnected(player.getUniqueId());
        }

        plugin.getProfileManager().unload(player.getUniqueId());
        plugin.getMenuManager().clear(player.getUniqueId());
    }
}
