package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.history.MatchHistoryEntry;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MatchHistoryCommand implements CommandExecutor {

    private final FistPractice plugin;

    public MatchHistoryCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        java.util.UUID uuid;
        String displayName;
        if (args.length >= 1) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            uuid = target.getUniqueId();
            displayName = args[0];
        } else if (sender instanceof Player p) {
            uuid = p.getUniqueId();
            displayName = p.getName();
        } else {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }

        plugin.getHistoryManager().getHistory(uuid, 10).thenAccept(list ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MessageUtil.sendRaw(sender, "<gold>Recent matches for " + displayName + ":");
                    SimpleDateFormat fmt = new SimpleDateFormat("MMM dd HH:mm");
                    for (MatchHistoryEntry entry : list) {
                        OfflinePlayer opponent = Bukkit.getOfflinePlayer(entry.getOpponent());
                        String result = entry.getResult().equals("WIN") ? "<green>WIN" : "<red>LOSS";
                        MessageUtil.sendRaw(sender, "<gray>[" + fmt.format(new Date(entry.getPlayedAt())) + "] "
                                + result + " <gray>vs <white>" + opponent.getName()
                                + " <gray>(" + entry.getMode() + ", " + entry.getKit() + ") "
                                + (entry.getEloChange() != 0 ? "<white>" + (entry.getEloChange() > 0 ? "+" : "") + entry.getEloChange() + " ELO" : ""));
                    }
                    if (list.isEmpty()) MessageUtil.sendRaw(sender, "<gray>No matches recorded yet.");
                }));
        return true;
    }
}
