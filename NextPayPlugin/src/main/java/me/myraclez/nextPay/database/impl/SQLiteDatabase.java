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

public class SQLiteDatabase implements Database {

	@Getter
	private HikariDataSource dataSource;
	private final NextPay plugin;

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
		final String balances = """
            CREATE TABLE IF NOT EXISTS np_balances (
                uuid TEXT NOT NULL PRIMARY KEY,
                balance REAL NOT NULL DEFAULT 0.0
            );
            """;

		final String settings = """
            CREATE TABLE IF NOT EXISTS np_settings (
                uuid TEXT NOT NULL PRIMARY KEY,
                payments INTEGER NOT NULL DEFAULT 0,
                notifications INTEGER NOT NULL DEFAULT 0
            );
            """;
		
		final String player_names = "CREATE TABLE IF NOT EXISTS np_player_names (" +
				"uuid TEXT NOT NULL PRIMARY KEY," +
				"name TEXT NOT NULL);";

		try (Connection conn = dataSource.getConnection();
			 Statement stmt = conn.createStatement()) {
			stmt.execute(balances);
			stmt.execute(settings);
			stmt.execute(player_names);
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
		}
	}

	/*

		Economy

	 */

	public CompletableFuture<Void> createAccountAsync(UUID uuid) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		new BukkitRunnable() {
			@Override
			public void run() {
				final String sql = "INSERT OR IGNORE INTO np_balances (uuid, balance) VALUES (?, 0.0)";
				try (Connection connection = dataSource.getConnection();
					 PreparedStatement stmt = connection.prepareStatement(sql)) {
					stmt.setString(1, uuid.toString());
					stmt.executeUpdate();
					future.complete(null);
				} catch (SQLException e) {
					plugin.getLogger().severe("Failed to create account for " + uuid + ": " + e.getMessage());
					future.completeExceptionally(e);
				}
			}
		}.runTaskAsynchronously(plugin);

		return future;
	}

	public CompletableFuture<Boolean> hasAccountAsync(UUID player) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();

		new BukkitRunnable() {
			@Override
			public void run() {
				final String sql = "SELECT 1 FROM np_balances WHERE uuid = ?";
				try (Connection connection = dataSource.getConnection();
					 PreparedStatement stmt = connection.prepareStatement(sql)) {
					stmt.setString(1, player.toString());
					try (ResultSet rs = stmt.executeQuery()) {
						future.complete(rs.next());
					}
				} catch (SQLException e) {
					plugin.getLogger().severe("Failed to check account for " + player + ": " + e.getMessage());
					future.completeExceptionally(e);
				}
			}
		}.runTaskAsynchronously(plugin);

		return future;
	}

	public void createAccount(UUID uuid) {

		final String sql = "INSERT OR IGNORE INTO np_balances (uuid, balance) VALUES (?, 0.0)";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, uuid.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to create account for " + uuid + ": " + e.getMessage());
		}

	}

	public boolean hasAccount(UUID player) {

		final String sql = "SELECT 1 FROM np_balances WHERE uuid = ?";

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

		String sql = "SELECT balance FROM np_balances WHERE uuid = ?";
		double balance = 0.0;

		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, player.toString());
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				balance = rs.getDouble(1);
			}
		} catch (SQLException e) {
			plugin.getLogger().severe("Couldn't get balance for UUID: " + player);
			plugin.getLogger().severe(e.getMessage());
		}

		return balance;
	}

	@Override
	public CompletableFuture<Double> getBalanceAsync(UUID player) {
		CompletableFuture<Double> future = new CompletableFuture<>();

		new BukkitRunnable() {
			@Override
			public void run() {
				String sql = "SELECT balance FROM np_balances WHERE uuid = ?";
				double balance = 0.0;

				try (Connection connection = dataSource.getConnection();
					 PreparedStatement statement = connection.prepareStatement(sql)) {
					statement.setString(1, player.toString());
					ResultSet rs = statement.executeQuery();
					if (rs.next()) {
						balance = rs.getDouble(1);
					}
					future.complete(balance);
				} catch (SQLException e) {
					plugin.getLogger().severe("Couldn't get balance for UUID: " + player);
					plugin.getLogger().severe(e.getMessage());
				}
			}
		}.runTaskAsynchronously(plugin);

		return future;
	}

	@Override
	public boolean withdraw(UUID player, double amount) {
		double current = getBalance(player);
		if (current < amount) return false;

		double newBalance = current - amount;

		String sql = "INSERT OR REPLACE INTO np_balances (uuid, balance) VALUES (?, ?)";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, player.toString());
			stmt.setDouble(2, newBalance);
			stmt.executeUpdate();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to withdraw: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public CompletableFuture<Boolean> withdrawAsync(UUID uuid, double amount) {
		return getBalanceAsync(uuid).thenCompose(current -> {
			if (current < amount) {
				return CompletableFuture.completedFuture(false);
			}

			double newBalance = current - amount;
			CompletableFuture<Boolean> future = new CompletableFuture<>();

			new BukkitRunnable() {
				@Override
				public void run() {
					String sql = "INSERT OR REPLACE INTO np_balances (uuid, balance) VALUES (?, ?)";
					try (Connection conn = dataSource.getConnection();
						 PreparedStatement stmt = conn.prepareStatement(sql)) {
						stmt.setString(1, uuid.toString());
						stmt.setDouble(2, newBalance);
						stmt.executeUpdate();
						future.complete(true);
					} catch (SQLException e) {
						plugin.getLogger().severe("Failed to withdraw: " + e.getMessage());
						future.complete(false);
					}
				}
			}.runTaskAsynchronously(plugin);

			return future;
		});
	}

	@Override
	public boolean deposit(UUID player, double amount) {
		double current = getBalance(player);
		double newBalance = current + amount;

		String sql = "INSERT OR REPLACE INTO np_balances (uuid, balance) VALUES (?, ?)";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, player.toString());
			stmt.setDouble(2, newBalance);
			stmt.executeUpdate();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to deposit: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public CompletableFuture<Boolean> depositAsync(UUID uuid, double amount) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();

		getBalanceAsync(uuid).thenCompose(current -> {
			double newBalance = current + amount;

			String sql = "INSERT OR REPLACE INTO np_balances (uuid, balance) VALUES (?, ?)";
			new BukkitRunnable() {
				@Override
				public void run() {
					try (Connection conn = dataSource.getConnection();
						 PreparedStatement stmt = conn.prepareStatement(sql)) {
						stmt.setString(1, uuid.toString());
						stmt.setDouble(2, newBalance);
						stmt.executeUpdate();
						future.complete(true);
					} catch (SQLException e) {
						plugin.getLogger().severe("Failed to deposit: " + e.getMessage());
						future.complete(false);
					}
				}
			}.runTaskAsynchronously(plugin);

			return future;
		});
		return future;
	}

	@Override
	public CompletableFuture<List<Map.Entry<UUID, Double>>> getAllBalances() {
		CompletableFuture<List<Map.Entry<UUID, Double>>> future = new CompletableFuture<>();
		new BukkitRunnable() {
			@Override
			public void run() {
				String sql = "SELECT uuid, balance FROM np_balances ORDER BY balance DESC";
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

	/*

		Settings

	 */

	@Override
	public void createSettingsEntry(UUID player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				final String sql = "INSERT OR IGNORE INTO np_settings (uuid, payments, notifications) VALUES (?, 1, 1)";
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
	public CompletableFuture<List<PlayerSettings>> getAllSettings() {
		CompletableFuture<List<PlayerSettings>> future = new CompletableFuture<>();

		List<PlayerSettings> list = new ArrayList<>();

		final String sql = "SELECT uuid, payments, notifications FROM np_settings;";

		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement()) {
					ResultSet rs = statement.executeQuery(sql);
					while (rs.next()) {
						list.add(new PlayerSettings(UUID.fromString(rs.getString("uuid")), rs.getBoolean("payments"), rs.getBoolean("notifications")));
					}
					future.complete(list);
				} catch (SQLException e) {
					plugin.getLogger().severe("Failed to load all player settings: " + e.getMessage());
					future.completeExceptionally(e);
				}
			}
		}.runTaskAsynchronously(plugin);
		return future;
	}

	@Override
	public void saveBalance(UUID uuid, double balance) {
		final String sql = "INSERT OR REPLACE INTO np_balances (uuid, balance) VALUES (?, ?);";
		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(sql)){
					preparedStatement.setString(1, uuid.toString());
					preparedStatement.setDouble(2, balance);
					preparedStatement.executeUpdate();
				} catch (SQLException e) {
					plugin.getLogger().severe("Failed to save balance for " + uuid.toString());
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	@Override
	public void savePlayerSettings(PlayerSettings settings) {
		final String sql = "INSERT OR REPLACE INTO np_settings (uuid, payments, notifications) VALUES (?, ? ,?);";
		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection conn = dataSource.getConnection();
				PreparedStatement statement = conn.prepareStatement(sql)){
					 statement.setString(1, settings.uuid().toString());
					 statement.setBoolean(2, settings.payments());
					 statement.setBoolean(3, settings.notifications());
					 statement.executeUpdate();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	@Override
	public CompletableFuture<PlayerSettings> getSettingsAsync(UUID player) {

		CompletableFuture<PlayerSettings> future = new CompletableFuture<>();

		new BukkitRunnable(){

			@Override
			public void run() {
				String sql = "SELECT payments, notifications FROM np_settings WHERE uuid = ?";
				try (Connection conn = dataSource.getConnection();
					 PreparedStatement statement = conn.prepareStatement(sql)) {
					statement.setString(1, player.toString());
					try (ResultSet rs = statement.executeQuery()) {
						if (rs.next()) {
							boolean payments = rs.getBoolean("payments");
							boolean notifications = rs.getBoolean("notifications");
							future.complete(new PlayerSettings(player, payments, notifications));
						} else {
							future.complete(new PlayerSettings(player,true, true));
						}

					}
				} catch (SQLException exception) {
					future.completeExceptionally(exception);
				}
			}
		}.runTaskAsynchronously(plugin);
		return future;
	}

	@Override
	public CompletableFuture<List<Map.Entry<UUID, String>>> getUsernames() {
		final CompletableFuture<List<Map.Entry<UUID, String>>> future = new CompletableFuture<>();

		final List<Map.Entry<UUID, String>> list = new ArrayList<>();

		final String sql = "SELECT * FROM np_player_names;";

		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection connection = dataSource.getConnection();
					 Statement statement = connection.createStatement()) {
					ResultSet resultSet = statement.executeQuery(sql);

					while (resultSet.next()) {
						list.add(Map.entry(UUID.fromString(resultSet.getString("uuid")),  resultSet.getString("name")));
					}

					future.complete(list);

				} catch (SQLException e) {
					plugin.getLogger().severe("Couldn't get all usernames, baltop will not have any names shown on the heads :" + e.getMessage());
					future.completeExceptionally(e);
				}
			}
		}.runTaskAsynchronously(plugin);

		return future;
	}

	@Override
	public void updatePlayer(UUID uuid, String name) {
		final String sql = "INSERT OR REPLACE INTO np_player_names (uuid, name) VALUES (?, ?);";

		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)){
					statement.setString(1, uuid.toString());
					statement.setString(2, name);
					statement.executeUpdate();
				} catch (SQLException e) {
					plugin.getLogger().severe("Couldn't save players name with uuid: " + uuid + " : " + e.getMessage());
				}
			}
		}.runTaskAsynchronously(plugin);
	}
}
