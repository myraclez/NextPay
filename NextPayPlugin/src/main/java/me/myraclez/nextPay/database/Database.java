package me.myraclez.nextPay.database;

import me.myraclez.nextPayAPI.PlayerSettings;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {

	void connect();
	void disconnect();
	boolean hasAccount(UUID player);
	void createAccount(UUID uuid);
	double getBalance(UUID player);
	boolean withdraw(UUID player, double amount);
	boolean deposit(UUID player, double amount);
	void createTables();
	void toggleNotifications(UUID player);
	void togglePayments(UUID player);
	boolean isPayments(UUID player);
	boolean isNotifications(UUID player);
	CompletableFuture<List<Map.Entry<UUID, Double>>> getAllBalances();
	void createSettingsEntry(UUID player);
	PlayerSettings getSettings(UUID player);
	CompletableFuture<PlayerSettings> getSettingsAsync(UUID player);
}