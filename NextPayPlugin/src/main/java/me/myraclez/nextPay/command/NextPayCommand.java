package me.myraclez.nextPay.command;

import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NextPayCommand implements CommandExecutor, TabCompleter {

	private final NextPay plugin;

	public NextPayCommand(NextPay plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
		if (!(commandSender.hasPermission("nextpay.admin"))) return true;
		if (strings.length < 1) {
			List<String> banner = plugin.getMessageManager().getMessageConfig().getStringList("banner");

			for (String string : banner) {
				commandSender.sendMessage(ColorUtil.colorize(string.replace("%version%", plugin.getPluginMeta().getVersion())));
			}

		} else if (strings.length == 1 && strings[0].equals("reload")) {
			long time = System.currentTimeMillis();
			plugin.reload();
			commandSender.sendMessage(ColorUtil.colorize("<gray>Reloaded configurations in <#2baafb>" + (System.currentTimeMillis() - time + "ms")));
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {

		List<String> completions = new ArrayList<>();

		if (strings.length == 1) {
			completions.add("reload");
		}
		return completions;
	}
}
