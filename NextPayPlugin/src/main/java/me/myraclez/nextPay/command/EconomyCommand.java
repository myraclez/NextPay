package me.myraclez.nextPay.command;

import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EconomyCommand implements CommandExecutor, TabCompleter {

	NextPay plugin;

	public EconomyCommand(NextPay plugin) {
		this.plugin = plugin;
	}


	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
		if (!(commandSender instanceof Player player)) {
			commandSender.sendMessage("Only players can execute this command");
			return true;
		}

		if (!(player.hasPermission("nextpay.admin"))) {
			player.sendMessage("You don't have permission to execute this command");
			return true;
		}

		if (args.length < 2) {
			return false;
		}

		String param = args[0];
		switch (param) {
			case "give": {
				if (args.length < 3) {
					return false;
				}

				OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
				if (!target.hasPlayedBefore()) {
					player.sendMessage(ColorUtil.colorize("<red>This player does exist"));
					return true;
				}
				double amount;
				try {
					amount = Formatter.deformat(args[2]);
				} catch (Exception e) {
					player.sendMessage(ColorUtil.colorize("<red>Invalid Amount"));
					break;
				}
				plugin.getEconomy().depositPlayer(target,  amount);
				plugin.getMessageManager().sendMessage(player, "messages.added", "%player%", target.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
				break;
			}
			case "take": {
				if (args.length < 3) {
					return false;
				}

				OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
				if (!target.hasPlayedBefore()) {
					player.sendMessage(ColorUtil.colorize("<red>This player does exist"));
					return true;
				}
				double amount;
				try {
					amount = Formatter.deformat(args[2]);
				} catch (Exception e) {
					player.sendMessage(ColorUtil.colorize("<red>Invalid Amount"));
					break;
				}

				if (!plugin.getEconomy().has(target, amount)) {
					player.sendMessage(ColorUtil.colorize("<red>This player doesn't have enough money"));
					return true;
				}

				plugin.getEconomy().withdrawPlayer(target,  amount);
				plugin.getMessageManager().sendMessage(player, "messages.removed", "%player%", target.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
				break;
			}
			case "set": {
				if (args.length < 3) {
					return false;
				}

				OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
				if (!target.hasPlayedBefore()) {
					player.sendMessage(ColorUtil.colorize("<red>This player doesn't exist"));
					return true;
				}
				double amount;
				try {
					amount = Formatter.deformat(args[2]);
				} catch (Exception e) {
					player.sendMessage(ColorUtil.colorize("<red>Invalid amount."));
					break;
				}

				double before = plugin.getEconomy().getBalance(target);
				plugin.getEconomy().withdrawPlayer(target, before);
				plugin.getEconomy().depositPlayer(target, amount);
				plugin.getMessageManager().sendMessage(player, "messages.set", "%player%", target.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
				break;
			}
			case "clear": {
				if (args.length < 2) {
					return false;
				}

				OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
				if (!target.hasPlayedBefore()) {
					player.sendMessage(ColorUtil.colorize("<red>This player doesn't exist"));
					return true;
				}

				double before = plugin.getEconomy().getBalance(target);
				plugin.getEconomy().withdrawPlayer(target, before);
				plugin.getMessageManager().sendMessage(player, "messages.cleared", "%player%", target.getName());
				break;
			}
			default: {
				return false;
			}
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
		List<String> completions = new ArrayList<>();
		switch (args.length) {
			case 1: {
				for (String s : Arrays.asList("give", "take", "set", "clear")) {
					if (s.startsWith(args[0])) {
						completions.add(s);
					}
				}
				break;
			}
			case 2: {
				for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
					if (player.getName().startsWith(args[1])) {
						completions.add(player.getName());
					}
				}
				break;
			}
			case 3: {
				if (!("clear".equals(args[0]))) {
					completions.add("<amount>");
				}
				break;
			}
		}
		return completions;
	}
}
