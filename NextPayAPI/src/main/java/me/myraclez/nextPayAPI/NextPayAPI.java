package me.myraclez.nextPayAPI;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NextPayAPI {

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
}
