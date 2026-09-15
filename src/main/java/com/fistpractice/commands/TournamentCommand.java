package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.tournament.Tournament;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class TournamentCommand implements CommandExecutor {

    private final FistPractice plugin;

    public TournamentCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendKey(player, "general.invalid-usage", Map.of("usage", "/tournament <create|join|leave|start|stop> <id>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!player.hasPermission("fistpractice.tournament")) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 4) { MessageUtil.sendRaw(player, "<red>/tournament create <id> <mode> <kit> [format]"); return true; }
                Tournament.Format format = args.length >= 5 ? parseFormat(args[4]) : Tournament.Format.SINGLE_ELIMINATION;
                plugin.getTournamentManager().create(args[1], format, args[2], args[3]);
                MessageUtil.sendRaw(player, "<green>Tournament '" + args[1] + "' created.");
            }
            case "join" -> {
                if (args.length < 2) return true;
                Tournament t = plugin.getTournamentManager().get(args[1]);
                if (t == null) { MessageUtil.sendRaw(player, "<red>No such tournament."); return true; }
                String error = plugin.getTournamentManager().join(t, player.getUniqueId());
                MessageUtil.sendRaw(player, error == null ? "<green>Joined tournament " + t.getId() + "." : "<red>" + error);
            }
            case "leave" -> {
                if (args.length < 2) return true;
                Tournament t = plugin.getTournamentManager().get(args[1]);
                if (t == null) return true;
                plugin.getTournamentManager().leave(t, player.getUniqueId());
            }
            case "start" -> {
                if (!player.hasPermission("fistpractice.tournament")) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                Tournament t = plugin.getTournamentManager().get(args[1]);
                if (t == null) { MessageUtil.sendRaw(player, "<red>No such tournament."); return true; }
                String error = plugin.getTournamentManager().start(t);
                if (error != null) MessageUtil.sendRaw(player, "<red>" + error);
            }
            case "stop" -> {
                if (!player.hasPermission("fistpractice.tournament")) { MessageUtil.sendKey(player, "general.no-permission"); return true; }
                if (args.length < 2) return true;
                Tournament t = plugin.getTournamentManager().get(args[1]);
                if (t != null) plugin.getTournamentManager().stop(t);
            }
            default -> MessageUtil.sendKey(player, "general.invalid-usage", Map.of("usage", "/tournament <create|join|leave|start|stop>"));
        }
        return true;
    }

    private Tournament.Format parseFormat(String s) {
        try { return Tournament.Format.valueOf(s.toUpperCase()); } catch (Exception e) { return Tournament.Format.SINGLE_ELIMINATION; }
    }
}
