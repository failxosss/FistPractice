package com.fistpractice.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLDatabase implements Database {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    private final int poolSize;

    private HikariDataSource dataSource;

    public MySQLDatabase(String host, int port, String database, String username,
                          String password, boolean useSSL, int poolSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.poolSize = poolSize;
    }

    @Override
    public void connect() {
        HikariConfig config = new HikariConfig();
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&autoReconnect=true&characterEncoding=utf8",
                host, port, database, useSSL);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(Math.max(2, poolSize));
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
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
            for (String sql : SchemaSQL.STATEMENTS_MYSQL) {
                s.execute(sql);
            }
        }
    }
}
