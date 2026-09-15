package com.fistpractice.commands;

import com.fistpractice.FistPractice;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LeaderboardCommand implements CommandExecutor {

    private final FistPractice plugin;

    public LeaderboardCommand(FistPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String mode = args.length >= 1 ? args[0].toUpperCase() : "CLASSIC";
        String sortColumn = args.length >= 2 ? mapSort(args[1]) : "elo";

        plugin.getDatabaseManager().query(db -> {
            List<String[]> rows = new ArrayList<>();
            try (var conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT uuid, elo, wins, losses, best_streak FROM up_mode_stats WHERE mode = ? ORDER BY " + sortColumn + " DESC LIMIT 10")) {
                ps.setString(1, mode);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{rs.getString("uuid"), String.valueOf(rs.getInt("elo")),
                                String.valueOf(rs.getInt("wins")), String.valueOf(rs.getInt("losses")),
                                String.valueOf(rs.getInt("best_streak"))});
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Leaderboard query failed: " + e.getMessage());
            }
            return rows;
        }).thenAccept(rows -> Bukkit.getScheduler().runTask(plugin, () -> {
            MessageUtil.sendRaw(sender, "<gold><bold>Leaderboard - " + mode);
            int rank = 1;
            for (String[] row : rows) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(row[0]));
                MessageUtil.sendRaw(sender, "<gray>#" + rank++ + " <white>" + op.getName()
                        + " <gray>- ELO: <white>" + row[1] + " <gray>W/L: <white>" + row[2] + "/" + row[3]
                        + " <gray>Best Streak: <white>" + row[4]);
            }
            if (rows.isEmpty()) MessageUtil.sendRaw(sender, "<gray>No ranked data yet for this mode.");
        }));
        return true;
    }

    private String mapSort(String arg) {
        return switch (arg.toLowerCase()) {
            case "wins" -> "wins";
            case "streak" -> "best_streak";
            default -> "elo";
        };
    }
}
