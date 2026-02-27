package com.zskv.claymorepvp.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zskv.claymorepvp.Claymorepvp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {

    private final Claymorepvp plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix;

    public DatabaseManager(Claymorepvp plugin) {
        this.plugin = plugin;
        this.tablePrefix = plugin.getConfig().getString("database.table_prefix", "claymorepvp_");
        setupPool();
        createTables();
    }

    private void setupPool() {
        HikariConfig config = new HikariConfig();
        String host = "104.128.51.122";
        int port = 3306;
        String database = "s63972_claymorepvp";
        String username = "u63972_VKgfs7goe5";
        String password = "CDtl^EOeLcZE@Ezm4NzVjH2X";

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement psPlayers = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS " + tablePrefix + "players (" +
                             "uuid VARCHAR(36) PRIMARY KEY, " +
                             "name VARCHAR(16), " +
                             "kills INT DEFAULT 0, " +
                             "matches INT DEFAULT 0" +
                             ")");
             PreparedStatement psKits = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS " + tablePrefix + "player_kits (" +
                             "uuid VARCHAR(36), " +
                             "kit_id VARCHAR(64), " +
                             "plays INT DEFAULT 0, " +
                             "PRIMARY KEY (uuid, kit_id)" +
                             ")")) {
            psPlayers.executeUpdate();
            psKits.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create database tables: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public void incrementKills(UUID uuid, String name) {
        updatePlayerStat(uuid, name, "kills", 1);
    }

    public void incrementMatches(UUID uuid, String name) {
        updatePlayerStat(uuid, name, "matches", 1);
    }

    public void incrementKitPlays(UUID uuid, String kitId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO " + tablePrefix + "player_kits (uuid, kit_id, plays) VALUES (?, ?, 1) " +
                                 "ON DUPLICATE KEY UPDATE plays = plays + 1")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, kitId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not update kit plays: " + e.getMessage());
            }
        });
    }

    private void updatePlayerStat(UUID uuid, String name, String column, int increment) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO " + tablePrefix + "players (uuid, name, " + column + ") VALUES (?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE name = ?, " + column + " = " + column + " + ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setInt(3, increment);
                ps.setString(4, name);
                ps.setInt(5, increment);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not update player " + column + ": " + e.getMessage());
            }
        });
    }
}
