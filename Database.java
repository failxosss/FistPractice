package com.fistpractice.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Thin abstraction over the underlying JDBC connection pool.
 * Implementations: {@link SQLiteDatabase}, {@link MySQLDatabase}.
 * All query execution actually happening against this must be dispatched
 * off the main thread by callers (see DatabaseManager).
 */
public interface Database {

    void connect() throws SQLException;

    void disconnect();

    Connection getConnection() throws SQLException;

    DataSource getDataSource();

    /**
     * Runs the CREATE TABLE IF NOT EXISTS statements needed by every module.
     */
    void createTables() throws SQLException;
}
