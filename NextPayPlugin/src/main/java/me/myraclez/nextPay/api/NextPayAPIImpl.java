package me.myraclez.nextPay.api;

import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPayAPI.NextPayAPI;
import me.myraclez.nextPayAPI.PlayerSettings;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NextPayAPIImpl implements NextPayAPI {

	private final NextPay plugin;

	public NextPayAPIImpl(NextPay plugin) {
		this.plugin = plugin;
	}

	@Override
	public void togglePayments(UUID uuid) {
		plugin.getDatabase().togglePayments(uuid);
	}

	@Override
	public void togglePayNotifications(UUID uuid) {
		plugin.getDatabase().toggleNotifications(uuid);
	}

	@Override
	public void setPayments(UUID uuid, boolean enabled) {
		if (plugin.getDatabase().isPayments(uuid) != enabled) {
			plugin.getDatabase().togglePayments(uuid);
		}
	}

	@Override
	public void setNotifications(UUID uuid, boolean enabled) {
		if (plugin.getDatabase().isNotifications(uuid) != enabled) {
			plugin.getDatabase().toggleNotifications(uuid);
		}
	}

	@Override
	public boolean isPayments(UUID uuid) {
		return plugin.getDatabase().isPayments(uuid);
	}

	@Override
	public boolean isNotifications(UUID uuid) {
		return plugin.getDatabase().isNotifications(uuid);
	}

	@Override
	public PlayerSettings getSettings(UUID uuid) {
		return getSettingsAsync(uuid).join();
	}

	@Override
	public CompletableFuture<Boolean> isPaymentsAsync(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> plugin.getDatabase().isPayments(uuid));
	}

	@Override
	public CompletableFuture<Boolean> isNotificationsAsync(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> plugin.getDatabase().isNotifications(uuid));
	}

	@Override
	public CompletableFuture<PlayerSettings> getSettingsAsync(UUID uuid) {
		return plugin.getDatabase().getSettingsAsync(uuid);
	}

	@Override
	public CompletableFuture<List<Map.Entry<UUID, Double>>> getAllBalancesAsync() {
		return plugin.getDatabase().getAllBalances();
	}
}
