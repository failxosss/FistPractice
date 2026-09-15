package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectateCommand implements CommandExecutor {

    private final FistPractice plugin;

    public SpectateCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("fistpractice.spectate")) {
            MessageUtil.sendKey(player, "general.no-permission");
            return true;
        }
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            plugin.getSpectatorManager().leave(player);
            MessageUtil.sendRaw(player, "<yellow>You stopped spectating.");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendKey(player, "general.invalid-usage", java.util.Map.of("usage", "/spectate <player>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendKey(player, "general.player-not-found");
            return true;
        }
        String error = plugin.getSpectatorManager().join(player, target);
        if (error != null) {
            MessageUtil.sendKey(player, error);
        } else {
            MessageUtil.sendRaw(player, "<green>Now spectating " + target.getName() + ".");
        }
        return true;
    }
}
