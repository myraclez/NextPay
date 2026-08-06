package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.Formatter;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyCommand {

	private static CompletableFuture<Suggestions> suggestUsernames(NextPay plugin, CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining().toLowerCase();
		for (String name : plugin.getEconomyManager().getUsernames()) {
			if (name.toLowerCase().startsWith(remaining)) {
				builder.suggest(name);
			}
		}
		return builder.buildFuture();
	}

	/*

		Actual Logic

	 */

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("economy")
				.requires(source -> source.getSender() instanceof Player)
				.requires(source -> source.getSender().hasPermission("nextpay.admin"))
				.then(Commands.literal("give")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests((ctx, builder) -> suggestUsernames(plugin, ctx, builder))
								.then(Commands.argument("amount", StringArgumentType.word())
										.executes(ctx -> give(plugin, ctx)))))
				.then(Commands.literal("take")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests((ctx, builder) -> suggestUsernames(plugin, ctx, builder))
								.then(Commands.argument("amount", StringArgumentType.word())
										.executes(ctx -> take(plugin, ctx)))))
				.then(Commands.literal("set")
						.then(Commands.argument("player", StringArgumentType.word())
								.then(Commands.argument("amount", StringArgumentType.word())
										.executes(ctx -> set(plugin, ctx)))))
				.then(Commands.literal("clear")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests((ctx, builder) -> suggestUsernames(plugin, ctx, builder))
								.executes(ctx -> clear(plugin, ctx))))
				.build();
	}

	/*

		Give

	 */

	private static int give(NextPay plugin, CommandContext<CommandSourceStack> ctx) {
		Player player = (Player) ctx.getSource().getSender();
		if (player == null) return Command.SINGLE_SUCCESS;

		String targetName = StringArgumentType.getString(ctx, "player");

		if (!plugin.getEconomyManager().isPlayer(targetName)) {
			plugin.getMessageManager().sendMessage(player, "invalid-player");
			return 1;
		}

		UUID targetUUID = plugin.getEconomyManager().getUuid(targetName);

		Double amount = validAmount(plugin, player, StringArgumentType.getString(ctx, "amount"));
		if (amount == null) return Command.SINGLE_SUCCESS;

		plugin.getEconomyManager().deposit(targetUUID, amount);
		plugin.getMessageManager().sendMessage(player, "messages.added",
				"%player%", targetName, "%amount%", String.valueOf(Formatter.format(amount)));
		return Command.SINGLE_SUCCESS;
	}

	/*

			Take

	 */

	private static int take(NextPay plugin, CommandContext<CommandSourceStack> ctx) {
		Player player = (Player) ctx.getSource().getSender();
		if (player == null) return Command.SINGLE_SUCCESS;

		String targetName = StringArgumentType.getString(ctx, "player");

		if (!plugin.getEconomyManager().isPlayer(targetName)) {
			plugin.getMessageManager().sendMessage(player, "invalid-player");
			return 1;
		}

		UUID targetUUID = plugin.getEconomyManager().getUuid(targetName);

		Double amount = validAmount(plugin, player, StringArgumentType.getString(ctx, "amount"));
		if (amount == null) return Command.SINGLE_SUCCESS;


		if (!plugin.getEconomyManager().has(targetUUID, amount)) {
			plugin.getMessageManager().sendMessage(player, "other-not-enough-money");
			return Command.SINGLE_SUCCESS;
		}

		plugin.getEconomyManager().withdraw(targetUUID, amount);
		plugin.getMessageManager().sendMessage(player, "messages.removed",
				"%player%", targetName, "%amount%", String.valueOf(Formatter.format(amount)));
		return Command.SINGLE_SUCCESS;
	}

	/*

			Set

	 */

	private static int set(NextPay plugin, CommandContext<CommandSourceStack> ctx) {
		Player player = (Player) ctx.getSource().getSender();
		if (player == null) return Command.SINGLE_SUCCESS;

		String targetName = StringArgumentType.getString(ctx, "player");

		if (!plugin.getEconomyManager().isPlayer(targetName)) {
			plugin.getMessageManager().sendMessage(player, "invalid-player");
			return 1;
		}

		UUID targetUUID = plugin.getEconomyManager().getUuid(targetName);

		Double amount = validAmount(plugin, player, StringArgumentType.getString(ctx, "amount"));
		if (amount == null) return Command.SINGLE_SUCCESS;

		double before = plugin.getEconomyManager().getBalance(targetUUID);
		plugin.getEconomyManager().withdraw(targetUUID, before);
		plugin.getEconomyManager().deposit(targetUUID, amount);
		plugin.getMessageManager().sendMessage(player, "messages.set",
				"%player%", targetName, "%amount%", String.valueOf(Formatter.format(amount)));
		return Command.SINGLE_SUCCESS;
	}

	/*

			Clear

	 */

	private static int clear(NextPay plugin, CommandContext<CommandSourceStack> ctx) {
		Player player = (Player) ctx.getSource().getSender();
		if (player == null) return Command.SINGLE_SUCCESS;

		String targetName = StringArgumentType.getString(ctx, "player");

		if (!plugin.getEconomyManager().isPlayer(targetName)) {
			plugin.getMessageManager().sendMessage(player, "error.invalid-player");
			return 1;
		}

		UUID targetUUID = plugin.getEconomyManager().getUuid(targetName);

		double before = plugin.getEconomyManager().getBalance(targetUUID);

		plugin.getEconomyManager().withdraw(targetUUID, before);
		plugin.getMessageManager().sendMessage(player, "messages.cleared", "%player%", targetName);
		return Command.SINGLE_SUCCESS;
	}

	/*

		Checks

	 */

	private static Double validAmount(NextPay plugin, Player sender, String raw) {
		try {
			return Formatter.deformat(raw);
		} catch (Exception e) {
			plugin.getMessageManager().sendMessage(sender, "error.invalid-amount");
			return null;
		}
	}

}
