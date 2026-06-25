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
import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {

	private final NextPay plugin;

	public BalanceCommand(NextPay plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

		if (!(commandSender instanceof Player player)) {
			plugin.getLogger().warning("Only players can execute this command");
			return true;
		}

		if (args.length == 0) {
			this.plugin
					.getMessageManager()
					.sendMessage(player, "messages.balance-own", "%amount%", Formatter.format(this.plugin.getEconomy().getBalance(player)));
		}

		if (args.length == 1) {
			OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

			if (!target.hasPlayedBefore()) {
				player.sendMessage(ColorUtil.colorize("&cThis player does exist"));
				return true;
			}

			plugin.getMessageManager()
					.sendMessage(player, "messages.balance-other", "%player%", target.getName(),
							"%amount%", Formatter.format(plugin.getEconomy().getBalance(target)));
		}

		return false;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
		List<String> completions = new ArrayList<>();
		if (strings.length == 1) {
			for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
				if (player.getName().startsWith(strings[0])) {
					completions.add(player.getName());
				}
			}
		}
		return completions;
	}
}
