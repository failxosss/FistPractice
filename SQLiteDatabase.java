package com.fistpractice.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteDatabase implements Database {

    private final File dataFolder;
    private final String fileName;
    private HikariDataSource dataSource;

    public SQLiteDatabase(File dataFolder, String fileName) {
        this.dataFolder = dataFolder;
        this.fileName = fileName;
    }

    @Override
    public void connect() {
        HikariConfig config = new HikariConfig();
        File dbFile = new File(dataFolder, fileName);
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite only supports a single writer safely
        config.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public javax.sql.DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void createTables() throws SQLException {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            for (String sql : SchemaSQL.STATEMENTS_SQLITE) {
                s.execute(sql);
            }
        }
    }
}
