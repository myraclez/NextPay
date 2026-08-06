package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class BalanceCommand {

	public BalanceCommand() {}

	private static CompletableFuture<Suggestions> suggestUsernames(NextPay plugin, CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining().toLowerCase();
		for (String name : plugin.getEconomyManager().getUsernames()) {
			if (name.toLowerCase().startsWith(remaining)) {
				builder.suggest(name);
			}
		}
		return builder.buildFuture();
	}

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("bal")
				.requires(source -> source.getSender().hasPermission("nextpay.bal"))
				.requires(source -> source.getSender() instanceof Player)
				.executes(ctx -> checkOwnBalance(ctx, plugin))
				.then(Commands.argument("target", StringArgumentType.word())
						.suggests((ctx, builder) -> suggestUsernames(plugin, ctx, builder))
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
		String targetName = StringArgumentType.getString(ctx, "target");

		if (!plugin.getEconomyManager().isPlayer(targetName)) {
			plugin.getMessageManager().sendMessage(player , "error.invalid-player");
			return Command.SINGLE_SUCCESS;
		}

		UUID targetUUID = plugin.getEconomyManager().getUuid(targetName);

		plugin.getEconomyManager().getBalanceAsync(targetUUID).thenAccept(balance -> {
			plugin.getMessageManager()
					.sendMessage(player, "messages.balance-other", "%player%", targetName,
							"%amount%", Formatter.format(balance));
		});

		return Command.SINGLE_SUCCESS;
	}
}

