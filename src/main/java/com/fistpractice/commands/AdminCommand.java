package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.match.Match;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final FistPractice plugin;

    public AdminCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fistpractice.admin")) {
            MessageUtil.sendKey(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendRaw(sender, "<gray>/up <reload|version|debug|stats|matches|queues|arenas|players>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                MessageUtil.sendKey(sender, "general.reload-success");
            }
            case "version" -> MessageUtil.sendRaw(sender, "<gray>FistPractice v" + plugin.getDescription().getVersion());
            case "debug" -> MessageUtil.sendRaw(sender, "<gray>Active matches: " + plugin.getMatchManager().getActiveMatches().size());
            case "stats" -> MessageUtil.sendRaw(sender, "<gray>Modes: " + plugin.getGameModeManager().getAll().size()
                    + " Kits: " + plugin.getKitManager().getAll().size()
                    + " Arenas: " + plugin.getArenaManager().getAll().size()
                    + " Parties: " + plugin.getPartyManager().getAll().size());
            case "matches" -> {
                MessageUtil.sendRaw(sender, "<gold>Active matches:");
                for (Match m : plugin.getMatchManager().getActiveMatches()) {
                    MessageUtil.sendRaw(sender, "<gray> - " + m.getId() + " <white>" + m.getMode().getId()
                            + " <gray>(" + m.getType() + ") state=" + m.getState());
                }
            }
            case "queues" -> MessageUtil.sendRaw(sender, "<gray>Use /up debug for a quick summary; per-pool breakdown available via the API.");
            case "arenas" -> MessageUtil.sendRaw(sender, "<gray>Arenas: <white>"
                    + String.join(", ", plugin.getArenaManager().getAll().stream().map(a -> a.getName()
                            + (a.isInUse() ? "(in use)" : "")).toList()));
            case "players" -> MessageUtil.sendRaw(sender, "<gray>Online: <white>" + plugin.getServer().getOnlinePlayers().size());
            default -> MessageUtil.sendRaw(sender, "<red>Unknown subcommand.");
        }
        return true;
    }
}
