package me.myraclez.nextPay.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.Formatter;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderApiHook extends PlaceholderExpansion {

	private final NextPay plugin;

	public PlaceholderApiHook(NextPay plugin) {
		this.plugin = plugin;

		register();
	}

	@Override
	public @NotNull String getIdentifier() {
		return "nextpay";
	}

	@Override
	public @NotNull String getAuthor() {

		return String.join(", ", plugin.getPluginMeta().getAuthors());
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}

	@Override
	public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {

		final double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());

		if (params.equalsIgnoreCase("balance")) {
			return String.format("%.0f", balance);
		} else if (params.equalsIgnoreCase("balance_formatted")) {
			return Formatter.format(balance);
		}

		return "Invalid Placeholder!";
	}
}
