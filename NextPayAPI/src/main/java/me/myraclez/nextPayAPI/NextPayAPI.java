package me.myraclez.nextPayAPI;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NextPayAPI {

	static NextPayAPI get() {
		return NextPayProvider.getAPI();
	}

	void togglePayments(UUID uuid);

	void togglePayNotifications(UUID uuid);

	void setPayments(UUID uuid, boolean enabled);

	void setNotifications(UUID uuid, boolean enabled);

	boolean isPayments(UUID uuid);

	boolean isNotifications(UUID uuid);

	PlayerSettings getSettings(UUID uuid);

	CompletableFuture<Boolean> isPaymentsAsync(UUID uuid);

	CompletableFuture<Boolean> isNotificationsAsync(UUID uuid);

	CompletableFuture<PlayerSettings> getSettingsAsync(UUID uuid);

	CompletableFuture<List<Map.Entry<UUID, Double>>> getAllBalancesAsync();
}
