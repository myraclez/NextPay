package me.myraclez.nextPay.database;

import me.myraclez.nextPayAPI.PlayerSettings;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {

	void connect();
	void disconnect();
	void createAccount(UUID uuid);
	boolean hasAccount(UUID uuid);
	CompletableFuture<Boolean> hasAccountAsync(UUID uuid);
	CompletableFuture<Void> createAccountAsync(UUID uuid);
	double getBalance(UUID uuid);
	CompletableFuture<Double> getBalanceAsync(UUID uuid);
	boolean withdraw(UUID uuid, double amount);
	CompletableFuture<Boolean> withdrawAsync(UUID uuid, double amount);
	boolean deposit(UUID uuid, double amount);
	CompletableFuture<Boolean> depositAsync(UUID uuid, double amount);
	void createTables();
	CompletableFuture<List<Map.Entry<UUID, Double>>> getAllBalances();
	void createSettingsEntry(UUID player);
	CompletableFuture<List<PlayerSettings>> getAllSettings();
	void saveBalance(UUID uuid, double balance);
	void savePlayerSettings(PlayerSettings settings);
	public CompletableFuture<PlayerSettings> getSettingsAsync(UUID uuid);
}