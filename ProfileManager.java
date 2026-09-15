package com.fistpractice.profile;

import com.fistpractice.FistPractice;
import com.fistpractice.database.Database;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ProfileManager {

    private final FistPractice plugin;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public ProfileManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    /** Called from PlayerJoinEvent - loads (or creates) the profile asynchronously. */
    public CompletableFuture<PlayerProfile> loadProfile(UUID uuid, String name) {
        return plugin.getDatabaseManager().query(db -> {
            PlayerProfile profile = new PlayerProfile(uuid, name);
            try (var conn = db.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT first_join, last_seen FROM up_profiles WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            profile.setFirstJoin(rs.getLong("first_join"));
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO up_profiles (uuid, name, first_join, last_seen) VALUES (?,?,?,?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, last_seen = excluded.last_seen")) {
                    // NOTE: MySQL doesn't support ON CONFLICT syntax; DatabaseManager picks
                    // the dialect-correct upsert via isMySQL() check in a real deployment.
                    // Kept simple here - see README limitation notes.
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setLong(3, profile.getFirstJoin());
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                } catch (SQLException upsertFail) {
                    // Fallback path for MySQL dialect
                    try (PreparedStatement ps2 = conn.prepareStatement(
                            "REPLACE INTO up_profiles (uuid, name, first_join, last_seen) VALUES (?,?,?,?)")) {
                        ps2.setString(1, uuid.toString());
                        ps2.setString(2, name);
                        ps2.setLong(3, profile.getFirstJoin());
                        ps2.setLong(4, System.currentTimeMillis());
                        ps2.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM up_mode_stats WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ModeStats stats = profile.getModeStats(rs.getString("mode"),
                                    plugin.getConfig().getInt("rating.starting-elo", 1000));
                            stats.setElo(rs.getInt("elo"));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load profile for " + name, e);
            }
            cache.put(uuid, profile);
            return profile;
        });
    }

    public PlayerProfile getProfile(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerProfile getProfile(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(),
                u -> new PlayerProfile(u, player.getName()));
    }

    public void saveProfile(PlayerProfile profile) {
        plugin.getDatabaseManager().execute(db -> {
            try (var conn = db.getConnection()) {
                for (ModeStats stats : profile.getAllModeStats().values()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO up_mode_stats (uuid, mode, elo, wins, losses, kills, deaths, " +
                            "current_streak, best_streak, matches_played, placement_matches) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
                        ps.setString(1, profile.getUuid().toString());
                        ps.setString(2, stats.getMode());
                        ps.setInt(3, stats.getElo());
                        ps.setInt(4, stats.getWins());
                        ps.setInt(5, stats.getLosses());
                        ps.setInt(6, stats.getKills());
                        ps.setInt(7, stats.getDeaths());
                        ps.setInt(8, stats.getCurrentStreak());
                        ps.setInt(9, stats.getBestStreak());
                        ps.setInt(10, stats.getMatchesPlayed());
                        ps.setInt(11, stats.getPlacementMatches());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save profile", e);
            }
        });
    }

    public void unload(UUID uuid) {
        PlayerProfile p = cache.remove(uuid);
        if (p != null) saveProfile(p);
    }
}
