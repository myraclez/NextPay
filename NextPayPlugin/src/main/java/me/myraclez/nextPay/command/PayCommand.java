package me.myraclez.nextPay.command;

import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.economy.NextEconomy;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.Formatter;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PayCommand implements CommandExecutor, TabCompleter {

	private final NextPay plugin;
	private NextEconomy economy;

	public PayCommand(NextPay plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

		if (!(sender instanceof Player player)) {
			plugin.getLogger().log(Level.FINE, "Only players can use this");
			return true;
		}

		if (args.length < 2) {
			player.sendMessage(ColorUtil.colorize("<red>Use /pay <player> <amount>"));
			return true;
		}

		OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

		if (!target.hasPlayedBefore()) {
			player.sendMessage(ColorUtil.colorize("<red>This player doesn't exist."));
			return true;
		}

		if (target.equals(player)) {
			player.sendMessage(ColorUtil.colorize("<red>You can't pay yourself."));
			return true;
		}

		double amount;
		try {
			amount = Formatter.deformat(args[1]);
		} catch (IllegalArgumentException e) {
			player.sendMessage(ColorUtil.colorize("<red>Invalid amount."));
			return true;
		}

		if (amount <= 0) {
			player.sendMessage(ColorUtil.colorize("<red>Invalid amount."));
			return true;
		}
		economy = plugin.getEconomy();

		if (economy == null) {
			player.sendMessage("Economy is null!");
			return true;
		}

		plugin.getDatabase().getSettingsAsync(player.getUniqueId()).thenAccept(playerSettings -> {
			new BukkitRunnable() {
				@Override
				public void run() {
					pay(player, target, amount, playerSettings.payments(), playerSettings.notifications());
				}
			}.runTask(plugin);
		});
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
		List<String> completions = new ArrayList<>();
		switch (strings.length) {
			case 1: {
				for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
					if (player.getName().startsWith(strings[0])) {
						completions.add(player.getName());
					}

				}
				break;
			}
			case 2: {
				completions.add("<amount>");
				break;
			}

		}
		return completions;
	}

	public void pay(Player player, OfflinePlayer target, double amount, boolean payments, boolean notifications) {
		if (!payments) {
			plugin.getMessageManager().sendMessage(player, "error.payments-disabled");
			return;
		}

		EconomyResponse withDraw = economy.withdrawPlayer(player, amount);
		if (!withDraw.transactionSuccess()) {
			player.sendMessage(ColorUtil.colorize("<red>Something went wrong, you have been refunded your money"));
			economy.depositPlayer(player, amount);
			plugin.getLogger().severe(withDraw.errorMessage);
			return;
		}
		EconomyResponse deposit = economy.depositPlayer(target, amount);
		if (!deposit.transactionSuccess()) {
			player.sendMessage(ColorUtil.colorize("The other player couldn't receive the money, so you have been refunded the amount"));
			economy.depositPlayer(player, amount);
			plugin.getLogger().severe(deposit.errorMessage);
			return;
		}

		plugin.getMessageManager().sendMessage(player, "messages.paid", "%player%", target.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
		if (target.isOnline() && notifications) {
			Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
			onlineTarget.playSound(onlineTarget.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 3, 1);
			plugin.getMessageManager().sendMessage(onlineTarget, "messages.got-paid", "%player%", player.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
		}
	}

}
