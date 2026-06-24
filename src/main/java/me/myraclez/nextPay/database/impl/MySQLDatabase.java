package me.myraclez.nextPay.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.database.Database;
import me.myraclez.nextPay.model.PlayerSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQLDatabase implements Database {

	@Getter
	HikariDataSource dataSource;
	NextPay plugin;
	private String host;
	private int port;
	private String database;
	private String username;
	private String password;


	public MySQLDatabase(NextPay plugin) {
		this.plugin = plugin;
		ConfigurationSection con = plugin.getConfig().getConfigurationSection("database");
		host = con.getString("host");
		port = con.getInt("port");
		database = con.getString("database");
		username = con.getString("username");
		password = con.getString("password");
	}

	@Override
	public void connect() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(String.format(
				"jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
				host,
				port,
				database
		));
		config.setUsername(username);
		config.setPassword(password);
		config.setMaximumPoolSize(plugin.getConfig().getInt("database.max-pool-size"));
		dataSource = new HikariDataSource(config);
		try (Connection connection = dataSource.getConnection()) {
			plugin.getLogger().info("Successfully connected to MySQL!");
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to connect to MySQL!");
			e.printStackTrace();
		}
		createTables();
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
					uuid VARCHAR(36) NOT NULL PRIMARY KEY,
					payments BOOLEAN NOT NULL DEFAULT FALSE,
					notifications BOOLEAN NOT NULL DEFAULT FALSE
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

	/*
		Economy methods
	 */

	@Override
	public void createAccount(UUID uuid) {

		final String sql = """
    			INSERT INTO npbalances (uuid, balance)
   				VALUES (?, ?)
    			AS new
    			ON DUPLICATE KEY UPDATE
        		balance = new.balance
    			""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, uuid.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to create account for " + uuid + ": " + e.getMessage());
		}
	}

	@Override
	public boolean hasAccount(UUID player) {

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

		String sql = "SELECT balance FROM npbalances WHERE uuid = ?";
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
	public boolean withdraw(UUID player, double amount) {
		double current = getBalance(player);
		if (current < amount) return false;

		double newBalance = current - amount;

		final String sql = """
    			INSERT INTO npbalances (uuid, balance)
    			VALUES (?, ?)
    			AS new
    			ON DUPLICATE KEY UPDATE
    			balance = new.balance
    			""";
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
	public boolean deposit(UUID player, double amount) {
		double current = getBalance(player);
		double newBalance = current + amount;

		String sql = "INSERT OR REPLACE INTO npbalances (uuid, balance) VALUES (?, ?)";
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

	/*
		PaySettings methods
	 */

	@Override
	public void createSettingsEntry(UUID player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				final String sql = "INSERT IGNORE INTO npsettings (uuid, payments, notifications) VALUES (?, 1, 1)";

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
	public CompletableFuture<PlayerSettings> getSettings(UUID player) {
		CompletableFuture<PlayerSettings> future = new CompletableFuture<>();
		new BukkitRunnable(){

			@Override
			public void run() {
				String sql = "SELECT payments, notifications FROM npsettings WHERE uuid = ?";
				try (Connection conn = dataSource.getConnection();
					 PreparedStatement statement = conn.prepareStatement(sql)) {
					statement.setString(1, player.toString());
					try (ResultSet rs = statement.executeQuery();) {
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

	@Override
	public void toggleNotifications(UUID player) {

	}

	@Override
	public void togglePayments(UUID player) {

	}

	@Override
	public boolean isPayments(UUID player) {
		String sql = "SELECT payments FROM npsettings WHERE uuid = ?";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement statement = conn.prepareStatement(sql);
		) {
			statement.setString(1, String.valueOf(player));
			try (ResultSet rs = statement.executeQuery()){
				if (rs.next()) {
					return rs.getBoolean("payments");
				}
			}
		} catch (SQLException e) {

		}
		return true;
	}

	@Override
	public boolean isNotifications(UUID player) {
		String sql = "SELECT notifications FROM npsettings WHERE uuid = ?";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement statement = conn.prepareStatement(sql);
		) {
			statement.setString(1, String.valueOf(player));
			try (ResultSet rs = statement.executeQuery()){
				if (rs.next()) {
					return rs.getBoolean("notifications");
				}
			}
		} catch (SQLException e) {

		}
		return true;
	}
}
