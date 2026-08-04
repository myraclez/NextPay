package me.myraclez.nextPay.manager;

import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.database.Database;
import me.myraclez.nextPayAPI.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EconomyManager {

	private final NextPay plugin;
	private Database database;

	private BukkitTask saveTask;

	private final Map<UUID, Double> balancesCache = new ConcurrentHashMap<>();
	private final Map<UUID, PlayerSettings> settingsCache = new ConcurrentHashMap<>();

	private final Set<UUID> unsavedBalances = ConcurrentHashMap.newKeySet();
	private final Set<UUID> unsavedSettings = ConcurrentHashMap.newKeySet();

	public EconomyManager(NextPay plugin) {
		this.plugin = plugin;
		init();
	}

	public void startTask() {
		saveTask = new BukkitRunnable() {
			@Override
			public void run() {
				Iterator<UUID> iterator = unsavedBalances.iterator();
				while (iterator.hasNext()) {
					UUID uuid = iterator.next();
					plugin.getDatabase().saveBalance(uuid, balancesCache.get(uuid));
					iterator.remove();
				}

				Iterator<UUID> iterator1 = unsavedSettings.iterator();
				while (iterator1.hasNext()) {
					UUID uuid = iterator.next();
					plugin.getDatabase().savePlayerSettings(settingsCache.get(uuid));
					iterator.remove();
				}
			}
		}.runTaskTimerAsynchronously(plugin, plugin.getConfig().getInt("auto-save-interval") * 20L, plugin.getConfig().getInt("auto-save-interval") * 20L);
	}

	public void stopTask() {
		saveTask.cancel();
	}

	private void init() {
		database = plugin.getDatabase();
		database.getAllBalances().thenAccept(balances -> {
			for (Map.Entry<UUID, Double> entry : balances) {
				balancesCache.put(entry.getKey(), entry.getValue());
			}
		}).exceptionally(ex -> {
			plugin.getLogger().severe("Failed to load balances: " + ex.getMessage());
			Bukkit.getPluginManager().disablePlugin(plugin);
			return null;
		});

		database.getAllSettings().thenAccept(settingsList -> {
			for (PlayerSettings playerSettings : settingsList) {
				settingsCache.put(playerSettings.uuid(), playerSettings);
			}
		}).exceptionally(ex -> {
			plugin.getLogger().severe("Failed to load settings: " + ex.getMessage());
			Bukkit.getPluginManager().disablePlugin(plugin);
			return null;
		});
		startTask();
	}

	public CompletableFuture<Void> createAccountAsync(UUID uuid) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (balancesCache.containsKey(uuid)) {
			future.complete(null);
			return future;
		}
		balancesCache.put(uuid, 0.0);
		unsavedBalances.add(uuid);
		return database.createAccountAsync(uuid);
	}

	public CompletableFuture<Boolean> hasAccountAsync(UUID uuid) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();

		if (balancesCache.containsKey(uuid)) {
			future.complete(true);
			return future;
		}

		return database.hasAccountAsync(uuid);
	}

	public void createAccount(UUID uuid) {
		if (!balancesCache.containsKey(uuid)) database.createAccount(uuid);
	}

	public boolean hasAccount(UUID uuid) {
		if (balancesCache.containsKey(uuid)) return true;
		return database.hasAccount(uuid);
	}

	public double getBalance(UUID uuid) {
		if (balancesCache.containsKey(uuid)) {
			return balancesCache.get(uuid);
		}
		double bal = database.getBalance(uuid);
		balancesCache.put(uuid, bal);
		return bal;
	}

	public CompletableFuture<Double> getBalanceAsync(UUID uuid) {
		CompletableFuture<Double> future = new CompletableFuture<>();
		if (balancesCache.containsKey(uuid)) {
			future.complete(balancesCache.get(uuid));
			return future;
		}
		return database.getBalanceAsync(uuid);
	}

	public boolean deposit(UUID uuid, double amount) {
		double old = balancesCache.containsKey(uuid) ? balancesCache.get(uuid) : getBalance(uuid);

		balancesCache.put(uuid, old + amount);
		unsavedBalances.add(uuid);
		return true;
	}

	public boolean withdraw(UUID uuid, double amount) {
		double old = balancesCache.containsKey(uuid) ? balancesCache.get(uuid) : getBalance(uuid);

		if (old < amount) {
			return false; // insufficient funds
		}

		balancesCache.put(uuid, old - amount);
		unsavedBalances.add(uuid);
		return true;
	}

	public void setSettings(UUID uuid, PlayerSettings playerSettings) {
		settingsCache.put(uuid, playerSettings);
		unsavedSettings.add(uuid);
	}

	public boolean has(UUID uuid, double amount) {
		return getBalance(uuid) >= amount;
	}

	public void togglePayments(UUID uuid) {
		PlayerSettings current = settingsCache.computeIfAbsent(uuid, id -> new PlayerSettings(id, true, true));
		settingsCache.replace(uuid, new PlayerSettings(uuid, !current.payments(), current.notifications()));
		unsavedSettings.add(uuid);
	}

	public void setPayments(UUID uuid, boolean toggle) {
		PlayerSettings current = settingsCache.computeIfAbsent(uuid, id -> new PlayerSettings(id, true, true));
		settingsCache.replace(uuid, new PlayerSettings(uuid, toggle, current.notifications()));
		unsavedSettings.add(uuid);
	}

	public void toggleNotifications(UUID uuid) {
		PlayerSettings current = settingsCache.computeIfAbsent(uuid, id -> new PlayerSettings(id, true, true));
		settingsCache.replace(uuid, new PlayerSettings(uuid, current.payments(), !current.notifications()));
		unsavedSettings.add(uuid);
	}

	public void setNofications(UUID uuid, boolean toggle) {
		PlayerSettings current = settingsCache.computeIfAbsent(uuid, id -> new PlayerSettings(id, true, true));
		settingsCache.replace(uuid, new PlayerSettings(uuid, current.payments(), toggle));
		unsavedSettings.add(uuid);
	}

	public boolean isPayments(UUID uuid) {
		return settingsCache.computeIfAbsent(uuid, id -> new PlayerSettings(id, true, true)).payments();
	}

	public boolean isNotifications(UUID uuid) {
		return settingsCache.computeIfAbsent(uuid, id -> new PlayerSettings(id, true, true)).notifications();
	}

	public void createSettings(UUID uuid) {
		if (!settingsCache.containsKey(uuid)) {
			database.createSettingsEntry(uuid);
			settingsCache.put(uuid, new PlayerSettings(uuid, true, true));
			unsavedSettings.add(uuid);
		}
	}

	public CompletableFuture<PlayerSettings> getSettingsAsync(UUID uuid) {
		CompletableFuture<PlayerSettings> future = new CompletableFuture<>();
		if (settingsCache.containsKey(uuid)) {
			future.complete(settingsCache.get(uuid));
			return future;
		}

		return database.getSettingsAsync(uuid);
	}

	public List<Map.Entry<UUID, Double>> getAllBalances() {
		return balancesCache.entrySet().stream()
				.sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
				.collect(Collectors.toList());
	}
}
