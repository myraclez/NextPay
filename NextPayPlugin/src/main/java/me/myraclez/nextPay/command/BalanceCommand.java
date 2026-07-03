package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import org.bukkit.entity.Player;


public class BalanceCommand {

	public BalanceCommand() {}

	private static final SuggestionProvider<CommandSourceStack> OFFLINE_PLAYER_SUGGESTIONS = (ctx, builder) -> {
		String remaining = builder.getRemainingLowerCase();
		for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
			String name = offlinePlayer.getName();
			if (name != null && name.toLowerCase().startsWith(remaining)) {
				builder.suggest(name);
			}
		}
		return builder.buildFuture();
	};

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("bal")
				.requires(source -> source.getSender().hasPermission("nextpay.bal"))
				.requires(source -> source.getSender() instanceof Player)
				.executes(ctx -> checkOwnBalance(ctx, plugin))
				.then(Commands.argument("target", StringArgumentType.word())
						.suggests(OFFLINE_PLAYER_SUGGESTIONS)
						.executes(ctx -> checkOthersBalance(ctx, plugin))).build();
	}

	public static int checkOwnBalance(CommandContext<CommandSourceStack> ctx, NextPay plugin) {
		Player player = (Player) ctx.getSource().getSender();
		plugin.getMessageManager()
				.sendMessage(player, "messages.balance-own", "%amount%", Formatter.format(plugin.getEconomy().getBalance(player)));
		return Command.SINGLE_SUCCESS;
	}

	public static int checkOthersBalance(CommandContext<CommandSourceStack> ctx, NextPay plugin) {
		Player player = (Player) ctx.getSource().getSender();
		String targetsName = StringArgumentType.getString(ctx, "target");

		OfflinePlayer target = Bukkit.getOfflinePlayer(targetsName);

		if (!target.hasPlayedBefore()) {
			player.sendMessage(ColorUtil.colorize("<red>This player doesn't exist"));
			return Command.SINGLE_SUCCESS;
		}

		plugin.getMessageManager()
				.sendMessage(player, "messages.balance-other", "%player%", target.getName(),
						"%amount%", Formatter.format(plugin.getEconomy().getBalance(target)));
		return Command.SINGLE_SUCCESS;
	}
}

