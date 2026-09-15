package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.profile.ModeStats;
import com.fistpractice.profile.PlayerProfile;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProfileCommand implements CommandExecutor {

    private final FistPractice plugin;

    public ProfileCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendKey(sender, "general.player-not-found");
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(target.getUniqueId());
        if (profile == null) {
            MessageUtil.sendRaw(sender, "<red>That player's profile has not loaded yet.");
            return true;
        }

        MessageUtil.sendRaw(sender, "<gray><strikethrough>                    </strikethrough>");
        MessageUtil.sendRaw(sender, "<gold><bold>" + profile.getName() + "'s Profile");
        MessageUtil.sendRaw(sender, "<gray>Total Matches: <white>" + profile.getTotalMatches());
        MessageUtil.sendRaw(sender, "<gray>Wins: <green>" + profile.getTotalWins() + " <gray>Losses: <red>" + profile.getTotalLosses());
        for (ModeStats stats : profile.getAllModeStats().values()) {
            String division = plugin.getRatingManager().getFormattedDivision(stats.getElo());
            MessageUtil.sendRaw(sender, "<gray> - " + stats.getMode() + ": <white>" + stats.getElo()
                    + " ELO <gray>(" + division + ") <white>" + stats.getWins() + "W/" + stats.getLosses() + "L"
                    + " <gray>streak: <white>" + stats.getCurrentStreak());
        }
        MessageUtil.sendRaw(sender, "<gray><strikethrough>                    </strikethrough>");
        return true;
    }
}
