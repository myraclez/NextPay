package me.myraclez.nextPay.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.database.Database;
import me.myraclez.nextPayAPI.PlayerSettings;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteDatabase implements Database {

	Map<UUID, Double> balancesCache = new ConcurrentHashMap<>();
	Map<UUID, PlayerSettings> settingsCache = new ConcurrentHashMap<>();

	@Getter
	HikariDataSource dataSource;
	NextPay plugin;

	public SQLiteDatabase(NextPay plugin) {
		this.plugin = plugin;
	}

	@Override
	public void connect() {

		try {
			File file = new File(plugin.getDataFolder(), "data.db");
			if (!file.exists()) {

				if (!file.createNewFile()) {
					plugin.getLogger().severe("Failed to create data.db file");
				}
			}

			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:sqlite:" + file.getPath());
			config.setPoolName("NextPay");

			// SQLite doesn't benefit from huge pools
			config.setMaximumPoolSize(4);
			config.setMinimumIdle(1);
			config.setConnectionTimeout(10000);

			// Important for SQLite stability
			config.addDataSourceProperty("journal_mode", "WAL");
			config.addDataSourceProperty("synchronous", "NORMAL");
			config.addDataSourceProperty("temp_store", "MEMORY");
			config.addDataSourceProperty("foreign_keys", "ON");
			config.addDataSourceProperty("busy_timeout", "5000");

			dataSource = new HikariDataSource(config);

			createTables();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

		@Override
	public void disconnect() {
		try {
			dataSource.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void createTables() {
		String balances = """
            CREATE TABLE IF NOT EXISTS npbalances (
                uuid TEXT NOT NULL PRIMARY KEY,
                balance REAL NOT NULL DEFAULT 0.0
            );
            """;

		String settings = """
            CREATE TABLE IF NOT EXISTS npsettings (
                uuid TEXT NOT NULL PRIMARY KEY,
                payments INTEGER NOT NULL DEFAULT 0,
                notifications INTEGER NOT NULL DEFAULT 0
            );
            """;

		try (Connection conn = dataSource.getConnection();
			 Statement stmt = conn.createStatement()) {
			stmt.execute(balances);
			stmt.execute(settings);
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
		}
	}

	@Override
	public void createAccount(UUID uuid) {
		if (balancesCache.containsKey(uuid)) return;

		final String sql = "INSERT OR REPLACE INTO npbalances (uuid, balance) VALUES (?, 0.0)";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, uuid.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to create account for " + uuid + ": " + e.getMessage());
		}

		balancesCache.put(uuid, 0.0);
	}

	@Override
	public boolean hasAccount(UUID player) {
		if (balancesCache.containsKey(player)) {
			return true;
		}

		final String sql = "SELECT 1 FROM npbalances WHERE uuid = ?";

		try (Connection connection = dataSource.getConnection();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {

			stmt.setString(1, player.toString());
			ResultSet rs = stmt.executeQuery();
			return rs.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public double getBalance(UUID player) {
		if (balancesCache.containsKey(player)) return balancesCache.get(player);

		String sql = "SELECT balance FROM npbalances WHERE uuid = ?";
		double balance = 0.0;

		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, player.toString());
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				balance = rs.getDouble(1);
			}
			balancesCache.put(player, balance);
		} catch (SQLException e) {
			plugin.getLogger().severe("Couldn't get balance for UUID: " + player);
			plugin.getLogger().severe(e.getMessage());
		}

		return balance;
	}

	@Override
	public boolean withdraw(UUID player, double amount) {
		double current = getBalance(player);
		if (current < amount) return false;

		double newBalance = current - amount;

		String sql = "INSERT OR REPLACE INTO npbalances (uuid, balance) VALUES (?, ?)";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, player.toString());
			stmt.setDouble(2, newBalance);
			stmt.executeUpdate();
			balancesCache.replace(player, newBalance);
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to withdraw: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean deposit(UUID player, double amount) {
		double current = getBalance(player);
		double newBalance = current + amount;

		String sql = "INSERT OR REPLACE INTO npbalances (uuid, balance) VALUES (?, ?)";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, player.toString());
			stmt.setDouble(2, newBalance);
			stmt.executeUpdate();
			balancesCache.replace(player, newBalance);
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to deposit: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public CompletableFuture<List<Map.Entry<UUID, Double>>> getAllBalances() {
		CompletableFuture<List<Map.Entry<UUID, Double>>> future = new CompletableFuture<>();
		new BukkitRunnable() {
			@Override
			public void run() {
				String sql = "SELECT uuid, balance FROM npbalances";
				List<Map.Entry<UUID, Double>> result = new ArrayList<>();
				try (Connection conn = dataSource.getConnection();
					 PreparedStatement statement = conn.prepareStatement(sql);
					 ResultSet rs = statement.executeQuery()) {

					while (rs.next()) {
						UUID uuid = UUID.fromString(rs.getString("uuid"));
						double balance = rs.getDouble("balance");
						result.add(Map.entry(uuid, balance));
					}
					future.complete(result);

				} catch (SQLException exception) {
					future.completeExceptionally(exception);
				}
			}
		}.runTaskAsynchronously(plugin);
		return future;
	}

	@Override
	public void createSettingsEntry(UUID player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				final String sql = "INSERT OR IGNORE INTO npsettings (uuid, payments, notifications) VALUES (?, 1, 1)";

				try (Connection connection = dataSource.getConnection();
					 PreparedStatement stmt = connection.prepareStatement(sql)) {

					stmt.setString(1, player.toString());
					stmt.executeUpdate();

				} catch (SQLException e) {
					plugin.getLogger().severe("Couldn't create settings entry for " + player + ": " + e.getMessage());
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	@Override
	public void toggleNotifications(UUID player) {
	}

	@Override
	public void togglePayments(UUID player) {
	}

	@Override
	public boolean isPayments(UUID player) {
		return false;
	}

	@Override
	public boolean isNotifications(UUID player) {
		return false;
	}

	@Override
	public CompletableFuture<PlayerSettings> getSettings(UUID player) {
		CompletableFuture<PlayerSettings> future = new CompletableFuture<>();
		new BukkitRunnable(){

			@Override
			public void run() {
				String sql = "SELECT payments, notifications FROM npsettings WHERE uuid = ?";
				try (Connection conn = dataSource.getConnection();
						PreparedStatement statement = conn.prepareStatement(sql)) {
					statement.setString(1, player.toString());
					try (ResultSet rs = statement.executeQuery()) {
						if (rs.next()) {
							boolean payments = rs.getBoolean("payments");
							boolean notifications = rs.getBoolean("notifications");
							future.complete(new PlayerSettings(payments, notifications));
						} else {
							future.complete(new PlayerSettings(true, true));
						}

					}
				} catch (SQLException exception) {
					future.completeExceptionally(exception);
				}
			}
		}.runTaskAsynchronously(plugin);
		return future;
	}
}
