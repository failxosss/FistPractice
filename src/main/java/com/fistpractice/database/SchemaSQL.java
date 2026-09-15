package com.fistpractice.database;

/**
 * Centralised DDL. Two flavours are kept (SQLite vs MySQL) because column
 * types (AUTOINCREMENT vs AUTO_INCREMENT, TEXT vs VARCHAR) differ slightly.
 */
final class SchemaSQL {

    private SchemaSQL() {}

    static final String[] STATEMENTS_SQLITE = {
        "CREATE TABLE IF NOT EXISTS up_profiles (" +
            "uuid TEXT PRIMARY KEY," +
            "name TEXT NOT NULL," +
            "first_join INTEGER," +
            "last_seen INTEGER" +
            ")",
        "CREATE TABLE IF NOT EXISTS up_mode_stats (" +
            "uuid TEXT NOT NULL," +
            "mode TEXT NOT NULL," +
            "elo INTEGER DEFAULT 1000," +
            "wins INTEGER DEFAULT 0," +
            "losses INTEGER DEFAULT 0," +
            "kills INTEGER DEFAULT 0," +
            "deaths INTEGER DEFAULT 0," +
            "current_streak INTEGER DEFAULT 0," +
            "best_streak INTEGER DEFAULT 0," +
            "matches_played INTEGER DEFAULT 0," +
            "placement_matches INTEGER DEFAULT 0," +
            "PRIMARY KEY (uuid, mode)" +
            ")",
        "CREATE TABLE IF NOT EXISTS up_match_history (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "opponent TEXT," +
            "mode TEXT," +
            "kit TEXT," +
            "arena TEXT," +
            "ranked INTEGER," +
            "result TEXT," +
            "elo_change INTEGER," +
            "duration_ms INTEGER," +
            "played_at INTEGER" +
            ")",
        "CREATE TABLE IF NOT EXISTS up_kits (" +
            "id TEXT PRIMARY KEY," +
            "owner TEXT," +
            "data TEXT" +
            ")",
        "CREATE TABLE IF NOT EXISTS up_kit_layouts (" +
            "uuid TEXT NOT NULL," +
            "kit_id TEXT NOT NULL," +
            "layout_name TEXT NOT NULL," +
            "data TEXT," +
            "PRIMARY KEY (uuid, kit_id, layout_name)" +
            ")",
        "CREATE TABLE IF NOT EXISTS up_parties (" +
            "id TEXT PRIMARY KEY," +
            "name TEXT," +
            "leader TEXT," +
            "members TEXT," +
            "moderators TEXT," +
            "rating INTEGER DEFAULT 1000" +
            ")"
    };

    static final String[] STATEMENTS_MYSQL = {
        "CREATE TABLE IF NOT EXISTS up_profiles (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "name VARCHAR(16) NOT NULL," +
            "first_join BIGINT," +
            "last_seen BIGINT" +
            ") ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS up_mode_stats (" +
            "uuid VARCHAR(36) NOT NULL," +
            "mode VARCHAR(32) NOT NULL," +
            "elo INT DEFAULT 1000," +
            "wins INT DEFAULT 0," +
            "losses INT DEFAULT 0," +
            "kills INT DEFAULT 0," +
            "deaths INT DEFAULT 0," +
            "current_streak INT DEFAULT 0," +
            "best_streak INT DEFAULT 0," +
            "matches_played INT DEFAULT 0," +
            "placement_matches INT DEFAULT 0," +
            "PRIMARY KEY (uuid, mode)" +
            ") ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS up_match_history (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "uuid VARCHAR(36) NOT NULL," +
            "opponent VARCHAR(36)," +
            "mode VARCHAR(32)," +
            "kit VARCHAR(32)," +
            "arena VARCHAR(32)," +
            "ranked TINYINT," +
            "result VARCHAR(16)," +
            "elo_change INT," +
            "duration_ms BIGINT," +
            "played_at BIGINT" +
            ") ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS up_kits (" +
            "id VARCHAR(32) PRIMARY KEY," +
            "owner VARCHAR(36)," +
            "data MEDIUMTEXT" +
            ") ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS up_kit_layouts (" +
            "uuid VARCHAR(36) NOT NULL," +
            "kit_id VARCHAR(32) NOT NULL," +
            "layout_name VARCHAR(32) NOT NULL," +
            "data MEDIUMTEXT," +
            "PRIMARY KEY (uuid, kit_id, layout_name)" +
            ") ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS up_parties (" +
            "id VARCHAR(36) PRIMARY KEY," +
            "name VARCHAR(32)," +
            "leader VARCHAR(36)," +
            "members MEDIUMTEXT," +
            "moderators MEDIUMTEXT," +
            "rating INT DEFAULT 1000" +
            ") ENGINE=InnoDB"
    };
}
