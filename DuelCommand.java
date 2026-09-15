package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.duel.DuelSettings;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {

    private final FistPractice plugin;

    public DuelCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("fistpractice.duel")) {
            MessageUtil.sendKey(player, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendKey(player, "general.invalid-usage", java.util.Map.of("usage", "/duel <player|accept|decline> [player]"));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("decline")) {
            if (args.length < 2) {
                MessageUtil.sendKey(player, "general.invalid-usage", java.util.Map.of("usage", "/duel " + args[0] + " <player>"));
                return true;
            }
            Player requester = Bukkit.getPlayer(args[1]);
            if (requester == null) {
                MessageUtil.sendKey(player, "general.player-not-found");
                return true;
            }
            if (args[0].equalsIgnoreCase("accept")) {
                plugin.getDuelManager().accept(player, requester.getUniqueId());
            } else {
                plugin.getDuelManager().decline(player, requester.getUniqueId());
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendKey(player, "general.player-not-found");
            return true;
        }

        String error = plugin.getDuelManager().canRequest(player, target);
        if (error != null) {
            MessageUtil.sendKey(player, error);
            return true;
        }

        DuelSettings settings = new DuelSettings();
        // Optional inline overrides: /duel <player> <mode> [ranked] [bestOf]
        if (args.length >= 2 && plugin.getGameModeManager().exists(args[1])) settings.setMode(args[1].toUpperCase());
        if (args.length >= 3 && args[2].equalsIgnoreCase("ranked")) settings.setRanked(true);
        if (args.length >= 4) {
            try { settings.setBestOf(Integer.parseInt(args[3])); } catch (NumberFormatException ignored) { }
        }

        plugin.getDuelManager().sendRequest(player, target, settings);
        return true;
    }
}
