package com.fistpractice.listeners;

import com.fistpractice.FistPractice;
import com.fistpractice.match.Match;
import com.fistpractice.match.MatchState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class MatchListener implements Listener {

    private final FistPractice plugin;

    public MatchListener(FistPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("match.freeze-during-countdown", true)) return;
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchOf(player.getUniqueId());
        if (match == null) return;
        if (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING) {
            // allow looking around, block actual translation
            if (event.getFrom().getX() != event.getTo().getX()
                    || event.getFrom().getY() != event.getTo().getY()
                    || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        // Spectators and active match participants alike should never be able
        // to drop items - prevents duplication/exploit vectors (spec 62).
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())
                || plugin.getMatchManager().isInMatch(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        Match match = plugin.getMatchManager().getMatchOf(player.getUniqueId());
        if (match != null && (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING)) {
            event.setCancelled(true);
        }
    }
}
