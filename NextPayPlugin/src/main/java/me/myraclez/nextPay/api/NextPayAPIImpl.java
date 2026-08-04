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
		plugin.getEconomyManager().togglePayments(uuid);
	}

	@Override
	public void togglePayNotifications(UUID uuid) {
		plugin.getEconomyManager().toggleNotifications(uuid);
	}

	@Override
	public void setPayments(UUID uuid, boolean enabled) {
		if (plugin.getEconomyManager().isPayments(uuid) != enabled) {
			plugin.getEconomyManager().togglePayments(uuid);
		}
	}

	@Override
	public void setNotifications(UUID uuid, boolean enabled) {
		if (plugin.getEconomyManager().isNotifications(uuid) != enabled) {
			plugin.getEconomyManager().toggleNotifications(uuid);
		}
	}

	@Override
	public boolean isPayments(UUID uuid) {
		return plugin.getEconomyManager().isPayments(uuid);
	}

	@Override
	public boolean isNotifications(UUID uuid) {
		return plugin.getEconomyManager().isNotifications(uuid);
	}

	@Override
	public CompletableFuture<Boolean> isPaymentsAsync(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> plugin.getEconomyManager().isPayments(uuid));
	}

	@Override
	public CompletableFuture<Boolean> isNotificationsAsync(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> plugin.getEconomyManager().isNotifications(uuid));
	}

	@Override
	public CompletableFuture<List<Map.Entry<UUID, Double>>> getAllBalancesAsync() {
		return plugin.getDatabase().getAllBalances();
	}
}
