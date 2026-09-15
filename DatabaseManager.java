package com.fistpractice.database;

import com.fistpractice.FistPractice;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Owns the active {@link Database} implementation and exposes a small async
 * facade so the rest of the plugin never touches JDBC on the main thread.
 */
public class DatabaseManager {

    private final FistPractice plugin;
    private final ExecutorService executor;
    private Database database;

    public DatabaseManager(FistPractice plugin) {
        this.plugin = plugin;
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "FistPractice-Database");
            t.setDaemon(true);
            return t;
        };
        this.executor = Executors.newFixedThreadPool(2, factory);
    }

    public void init() {
        String type = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
        try {
            if (type.equals("MYSQL") || type.equals("MARIADB")) {
                database = new MySQLDatabase(
                        plugin.getConfig().getString("mysql.host", "localhost"),
                        plugin.getConfig().getInt("mysql.port", 3306),
                        plugin.getConfig().getString("mysql.database", "fistpractice"),
                        plugin.getConfig().getString("mysql.username", "root"),
                        plugin.getConfig().getString("mysql.password", ""),
                        plugin.getConfig().getBoolean("mysql.useSSL", false),
                        plugin.getConfig().getInt("mysql.pool-size", 10)
                );
            } else {
                database = new SQLiteDatabase(plugin.getDataFolder(),
                        plugin.getConfig().getString("database.file", "database.db"));
            }
            database.connect();
            database.createTables();
            plugin.getLogger().info("Database (" + type + ") connected and schema verified.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialise database", e);
        }
    }

    public Database getDatabase() {
        return database;
    }

    public <T> CompletableFuture<T> query(Function<Database, T> function) {
        return CompletableFuture.supplyAsync(() -> function.apply(database), executor)
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Database query failed", ex);
                    return null;
                });
    }

    public void execute(java.util.function.Consumer<Database> consumer) {
        executor.submit(() -> {
            try {
                consumer.accept(database);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Database write failed", e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        if (database != null) {
            database.disconnect();
        }
    }
}
