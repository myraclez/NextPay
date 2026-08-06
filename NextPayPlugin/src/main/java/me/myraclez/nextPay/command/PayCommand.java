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
import me.myraclez.nextPay.economy.NextEconomy;
import me.myraclez.nextPay.util.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PayCommand {

	public PayCommand() {}

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
		return Commands.literal("pay")
				.requires(source -> source.getSender() instanceof Player)
				.requires(source -> source.getSender().hasPermission("nextpay.pay"))
				.then(Commands.argument("target", StringArgumentType.word())
						.suggests((ctx, builder) ->suggestUsernames(plugin, ctx, builder))
						.then(Commands.argument("amount", StringArgumentType.word())
								.suggests((ctx, builder) -> {
									builder.suggest("<amount>");
									return builder.buildFuture();
								})
								.executes(ctx -> {
									Player player = (Player) ctx.getSource().getSender();
									String targetName = StringArgumentType.getString(ctx, "target");
									String rawAmount = StringArgumentType.getString(ctx, "amount");

									if (!plugin.getEconomyManager().isPlayer(targetName)) {
										plugin.getMessageManager().sendMessage(player, "error.invalid-player");
										return Command.SINGLE_SUCCESS;
									}

									UUID targetUUID = plugin.getEconomyManager().getUuid(targetName);

									if (targetUUID.equals(player.getUniqueId())) {
										plugin.getMessageManager().sendMessage(player, "error.pay-yourself");
										return Command.SINGLE_SUCCESS;
									}

									double amount;
									try {
										amount = me.myraclez.nextPay.util.Formatter.deformat(rawAmount);
									} catch (IllegalArgumentException e) {
										plugin.getMessageManager().sendMessage(player, "error.invalid-amount");
										return Command.SINGLE_SUCCESS;
									}

									if (amount <= 0) {
										plugin.getMessageManager().sendMessage(player, "error.invalid-amount");
										return Command.SINGLE_SUCCESS;
									}

									double finalAmount = amount;
									plugin.getEconomyManager().getSettingsAsync(targetUUID).thenAccept(playerSettings -> {
										new BukkitRunnable() {
											@Override
											public void run() {
												pay(plugin, player, targetUUID, targetName, finalAmount, playerSettings.payments(), playerSettings.notifications());
											}
										}.runTask(plugin);
									});

									return Command.SINGLE_SUCCESS;
								})
						)
				)
				.build();
	}

	private static void pay(NextPay plugin, Player player, UUID targetUUID, String targetName,
							double amount, boolean payments, boolean notifications) {
		if (!payments) {
			plugin.getMessageManager().sendMessage(player, "error.payments-disabled");
			return;
		}

		final boolean withdrawalSuccess = plugin.getEconomyManager().withdraw(player.getUniqueId(), amount);
		if (!withdrawalSuccess) {
			plugin.getMessageManager().sendMessage(player, "error.not-enough-money");
			return;
		}

		final boolean depositSuccess = plugin.getEconomyManager().deposit(targetUUID, amount);
		if (!depositSuccess) {
			plugin.getMessageManager().sendMessage(player, "error.player-not-received");
			plugin.getEconomyManager().deposit(player.getUniqueId(), amount);
			return;
		}

		plugin.getMessageManager().sendMessage(player, "messages.paid",
				"%player%", targetName, "%amount%", String.valueOf(me.myraclez.nextPay.util.Formatter.format(amount)));

		if  (notifications) {
			Player onlineTarget = Bukkit.getPlayer(targetUUID);
			if (onlineTarget != null) {
				onlineTarget.playSound(onlineTarget.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 3, 1);
				plugin.getMessageManager().sendMessage(onlineTarget, "messages.got-paid",
						"%player%", player.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
			}

		}
	}
}