package com.fistpractice.history;

import com.fistpractice.FistPractice;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HistoryManager {

    private final FistPractice plugin;

    public HistoryManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public void record(MatchHistoryEntry entry) {
        plugin.getDatabaseManager().execute(db -> {
            try (var conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO up_match_history (uuid, opponent, mode, kit, arena, ranked, result, elo_change, duration_ms, played_at) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, entry.getPlayer().toString());
                ps.setString(2, entry.getOpponent().toString());
                ps.setString(3, entry.getMode());
                ps.setString(4, entry.getKit());
                ps.setString(5, entry.getArena());
                ps.setInt(6, entry.isRanked() ? 1 : 0);
                ps.setString(7, entry.getResult());
                ps.setInt(8, entry.getEloChange());
                ps.setLong(9, entry.getDurationMs());
                ps.setLong(10, entry.getPlayedAt());
                ps.executeUpdate();
                trimExcess(conn, entry.getPlayer());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to record match history: " + e.getMessage());
            }
        });
    }

    private void trimExcess(java.sql.Connection conn, UUID uuid) throws Exception {
        int max = plugin.getConfig().getInt("history.max-entries-per-player", 50);
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM up_match_history WHERE uuid = ? AND id NOT IN (" +
                "SELECT id FROM up_match_history WHERE uuid = ? ORDER BY played_at DESC LIMIT ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            ps.setInt(3, max);
            ps.executeUpdate();
        }
    }

    public CompletableFuture<List<MatchHistoryEntry>> getHistory(UUID uuid, int limit) {
        return plugin.getDatabaseManager().query(db -> {
            List<MatchHistoryEntry> list = new ArrayList<>();
            try (var conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM up_match_history WHERE uuid = ? ORDER BY played_at DESC LIMIT ?")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MatchHistoryEntry entry = new MatchHistoryEntry(
                                uuid,
                                UUID.fromString(rs.getString("opponent")),
                                rs.getString("mode"),
                                rs.getString("kit"),
                                rs.getString("arena"),
                                rs.getInt("ranked") == 1,
                                rs.getString("result"),
                                rs.getLong("duration_ms"));
                        entry.setEloChange(rs.getInt("elo_change"));
                        list.add(entry);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load match history: " + e.getMessage());
            }
            return list;
        });
    }
}
