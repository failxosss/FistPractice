package com.fistpractice.listeners;

import com.fistpractice.FistPractice;
import com.fistpractice.match.Match;
import com.fistpractice.match.MatchSide;
import com.fistpractice.match.MatchState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class CombatListener implements Listener {

    private final FistPractice plugin;

    public CombatListener(FistPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Match match = plugin.getMatchManager().getMatchOf(player.getUniqueId());

        if (match == null) return; // outside a match: normal server rules apply
        if (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
            return;
        }

        switch (event.getCause()) {
            case FALL -> event.setCancelled(!match.getMode().isFallDamage());
            case FIRE, FIRE_TICK, LAVA -> event.setCancelled(!match.getMode().isFireDamage());
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        Match match = plugin.getMatchManager().getMatchOf(victim.getUniqueId());
        if (match == null || !match.getAllPlayers().contains(attacker.getUniqueId())) {
            // No PvP damage between players outside of a match, or against someone
            // not in the same match (e.g. a spectator can never be hurt/hurt others).
            event.setCancelled(true);
            return;
        }

        if (!match.isFriendlyFire()) {
            MatchSide victimSide = match.getSideOf(victim.getUniqueId());
            MatchSide attackerSide = match.getSideOf(attacker.getUniqueId());
            if (victimSide != null && victimSide == attackerSide) {
                event.setCancelled(true);
            }
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile proj
                && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchOf(player.getUniqueId());
        if (match == null) return;

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDeathMessage(null);

        UUID uuid = player.getUniqueId();
        plugin.getMatchManager().eliminate(match, uuid);

        // Respawn immediately as a spectator of the ongoing match rather than
        // sitting on the death screen.
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.spigot().respawn();
        }, 1L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchOf(player.getUniqueId());
        if (match == null) return;
        if (match.getArena().getSpectatorSpawn() != null) {
            event.setRespawnLocation(match.getArena().getSpectatorSpawn());
        }
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(org.bukkit.GameMode.SPECTATOR));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Match match = plugin.getMatchManager().getMatchOf(event.getPlayer().getUniqueId());
        if (match != null && !match.getMode().isAllowBlockPlace()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Match match = plugin.getMatchManager().getMatchOf(event.getPlayer().getUniqueId());
        if (match != null && !match.getMode().isAllowBlockBreak()) {
            event.setCancelled(true);
        }
    }
}
