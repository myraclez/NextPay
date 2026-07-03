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

public class EconomyCommand {

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

	/*

		Actual Logic

	 */

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("economy")
				.requires(source -> source.getSender() instanceof Player)
				.requires(source -> source.getSender().hasPermission("nextpay.admin"))
				.then(Commands.literal("give")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests(OFFLINE_PLAYER_SUGGESTIONS)
								.then(Commands.argument("amount", StringArgumentType.word())
										.executes(ctx -> give(plugin, ctx)))))
				.then(Commands.literal("take")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests(OFFLINE_PLAYER_SUGGESTIONS)
								.then(Commands.argument("amount", StringArgumentType.word())
										.executes(ctx -> take(plugin, ctx)))))
				.then(Commands.literal("set")
						.then(Commands.argument("player", StringArgumentType.word())
								.then(Commands.argument("amount", StringArgumentType.word())
										.executes(ctx -> set(plugin, ctx)))))
				.then(Commands.literal("clear")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests(OFFLINE_PLAYER_SUGGESTIONS)
								.executes(ctx -> clear(plugin, ctx))))
				.build();
	}

	/*

		Helper methods

	 */

	private static int give(NextPay plugin, CommandContext<CommandSourceStack> ctx) {
		Player player = (Player) ctx.getSource().getSender();
		if (player == null) return Command.SINGLE_SUCCESS;

		OfflinePlayer target = requireExistingPlayer(player, StringArgumentType.getString(ctx, "player"));
		if (target == null) return Command.SINGLE_SUCCESS;

		Double amount = requireValidAmount(player, StringArgumentType.getString(ctx, "amount"));
		if (amount == null) return Command.SINGLE_SUCCESS;

		plugin.getEconomy().depositPlayer(target, amount);
		plugin.getMessageManager().sendMessage(player, "messages.added",
				"%player%", target.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
		return Command.SINGLE_SUCCESS;
	}

	private static int take(NextPay plugin, CommandContext<CommandSourceStack> ctx) {
		Player player = (Player) ctx.getSource().getSender();
		if (player == null) return Command.SINGLE_SUCCESS;

		OfflinePlayer target = requireExistingPlayer(player, StringArgumentType.getString(ctx, "player"));
		if (target == null) return Command.SINGLE_SUCCESS;

		Double amount = requireValidAmount(player, StringArgumentType.getString(ctx, "amount"));
		if (amount == null) return Command.SINGLE_SUCCESS;

		if (!plugin.getEconomy().has(target, amount)) {
			player.sendMessage(ColorUtil.colorize("<red>This player doesn't have enough money"));
			return Command.SINGLE_SUCCESS;
		}

		plugin.getEconomy().withdrawPlayer(target, amount);
		plugin.getMessageManager().sendMessage(player, "messages.removed",
				"%player%", target.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
		return Command.SINGLE_SUCCESS;
	}

	private static int set(NextPay plugin, CommandContext<CommandSourceStack> ctx) {
		Player player = (Player) ctx.getSource().getSender();
		if (player == null) return Command.SINGLE_SUCCESS;

		OfflinePlayer target = requireExistingPlayer(player, StringArgumentType.getString(ctx, "player"));
		if (target == null) return Command.SINGLE_SUCCESS;

		Double amount = requireValidAmount(player, StringArgumentType.getString(ctx, "amount"));
		if (amount == null) return Command.SINGLE_SUCCESS;

		double before = plugin.getEconomy().getBalance(target);
		plugin.getEconomy().withdrawPlayer(target, before);
		plugin.getEconomy().depositPlayer(target, amount);
		plugin.getMessageManager().sendMessage(player, "messages.set",
				"%player%", target.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
		return Command.SINGLE_SUCCESS;
	}

	private static int clear(NextPay plugin, CommandContext<CommandSourceStack> ctx) {
		Player player = (Player) ctx.getSource().getSender();
		if (player == null) return Command.SINGLE_SUCCESS;

		OfflinePlayer target = requireExistingPlayer(player, StringArgumentType.getString(ctx, "player"));
		if (target == null) return Command.SINGLE_SUCCESS;

		double before = plugin.getEconomy().getBalance(target);
		plugin.getEconomy().withdrawPlayer(target, before);
		plugin.getMessageManager().sendMessage(player, "messages.cleared", "%player%", target.getName());
		return Command.SINGLE_SUCCESS;
	}

	/*

		Checks

	 */

	private static OfflinePlayer requireExistingPlayer(Player sender, String name) {
		OfflinePlayer target = Bukkit.getOfflinePlayer(name);
		if (!target.hasPlayedBefore()) {
			sender.sendMessage(ColorUtil.colorize("<red>This player doesn't exist"));
			return null;
		}
		return target;
	}

	private static Double requireValidAmount(Player sender, String raw) {
		try {
			return Formatter.deformat(raw);
		} catch (Exception e) {
			sender.sendMessage(ColorUtil.colorize("<red>Invalid Amount"));
			return null;
		}
	}

}
