package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class QueueCommand implements CommandExecutor {

    private final FistPractice plugin;

    public QueueCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendKey(sender, "general.player-only");
            return true;
        }

        if (args.length == 0) {
            plugin.getQueueManager().leave(player.getUniqueId());
            MessageUtil.sendKey(player, "queue.left");
            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            plugin.getQueueManager().leave(player.getUniqueId());
            MessageUtil.sendKey(player, "queue.left");
            return true;
        }

        String modeId = args[0];
        boolean ranked = args.length < 2 || !args[1].equalsIgnoreCase("unranked");
        if (ranked && !plugin.getConfig().getBoolean("queue.ranked.enabled", true)) {
            MessageUtil.sendRaw(player, "<red>Ranked queues are disabled.");
            return true;
        }
        if (!ranked && !plugin.getConfig().getBoolean("queue.unranked.enabled", true)) {
            MessageUtil.sendRaw(player, "<red>Unranked queues are disabled.");
            return true;
        }

        String error = plugin.getQueueManager().joinSolo(player, modeId, ranked);
        if (error != null) MessageUtil.sendKey(player, error, Map.of());
        return true;
    }
}
