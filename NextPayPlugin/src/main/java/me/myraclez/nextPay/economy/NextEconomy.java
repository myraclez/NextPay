package me.myraclez.nextPay.economy;

import me.myraclez.nextPay.NextPay;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class NextEconomy implements Economy {

	private final NextPay plugin;

	public NextEconomy(NextPay plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean isEnabled() {
		return plugin.isEnabled();
	}

	@Override
	public String getName() {
		return "NextPay";
	}

	@Override
	public boolean hasBankSupport() {
		return false;
	}

	@Override
	public int fractionalDigits() {
		return 3;
	}

	@Override
	public String currencyNamePlural() {
		return "Dollars";
	}

	@Override
	public String currencyNameSingular() {
		return "Dollar";
	}

	@Override
	public boolean hasAccount(String playerName) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if (player != null) {
			return hasAccount(player);
		}
		return false;
	}

	@Override
	public boolean hasAccount(OfflinePlayer player) {
		return plugin.getDatabase().hasAccount(player.getUniqueId());
	}

	@Override
	public boolean hasAccount(String playerName, String worldName) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if (player != null) {
			return hasAccount(player);
		}
		return false;
	}

	@Override
	public boolean hasAccount(OfflinePlayer player, String worldName) {
		return hasAccount(player);
	}

	@Override
	public double getBalance(String playerName) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if (player != null) {
			return getBalance(player);
		}
		return 0;
	}

	@Override
	public double getBalance(OfflinePlayer player) {
		return plugin.getDatabase().getBalance(player.getUniqueId());
	}

	@Override
	public double getBalance(String playerName, String world) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if (player != null) {
			return getBalance(player);
		}
		return 0;
	}

	@Override
	public double getBalance(OfflinePlayer player, String world) {
		return plugin.getDatabase().getBalance(player.getUniqueId());
	}

	@Override
	public boolean has(String playerName, double amount) {
		return getBalance(playerName) >= amount;
	}

	@Override
	public boolean has(OfflinePlayer player, double amount) {
		return getBalance(player) >= amount;
	}

	@Override
	public boolean has(String playerName, String worldName, double amount) {
		return getBalance(playerName) >= amount;
	}

	@Override
	public boolean has(OfflinePlayer player, String worldName, double amount) {
		return getBalance(player) >= amount;
	}

	@Override
	public EconomyResponse withdrawPlayer(String playerName, double amount) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		return withdrawPlayer(player, amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
		if (!hasAccount(player)) {
			createPlayerAccount(player);
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Account not found");
		}
		if (!has(player, amount)) {
			return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
		}
		boolean success = plugin.getDatabase().withdraw(player.getUniqueId(), amount);
		if (success) {
			return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, "");
		}
		return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Withdrawal failed");
	}

	@Override
	public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
		return withdrawPlayer(playerName, amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
		return withdrawPlayer(player, amount);
	}

	@Override
	public EconomyResponse depositPlayer(String playerName, double amount) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		return depositPlayer(player, amount);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
		if (!hasAccount(player)) {
			createPlayerAccount(player);
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Account not found");
		}
		boolean success = plugin.getDatabase().deposit(player.getUniqueId(), amount);
		if (success) {
			return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, "");
		}
		return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Deposit failed");
	}

	@Override
	public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
		return depositPlayer(playerName, amount);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
		return depositPlayer(player, amount);
	}

	@Override
	public boolean createPlayerAccount(String playerName) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		return createPlayerAccount(player);
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player) {
		if (hasAccount(player)) return false;
		plugin.getDatabase().createAccount(player.getUniqueId());
		return true;
	}

	@Override
	public boolean createPlayerAccount(String playerName, String worldName) {
		return createPlayerAccount(playerName);
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
		return createPlayerAccount(player);
	}

	@Override
	public String format(double amount) {
		return String.format("%,.2f %s", amount, amount == 1 ? currencyNameSingular() : currencyNamePlural());
	}

	@Override
	public EconomyResponse createBank(String name, String player) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse createBank(String name, OfflinePlayer player) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse deleteBank(String name) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse bankBalance(String name) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse bankHas(String name, double amount) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse bankWithdraw(String name, double amount) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse bankDeposit(String name, double amount) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse isBankOwner(String name, String playerName) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse isBankMember(String name, String playerName) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public EconomyResponse isBankMember(String name, OfflinePlayer player) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank is not supported");
	}

	@Override
	public List<String> getBanks() {
		return List.of();
	}
}

