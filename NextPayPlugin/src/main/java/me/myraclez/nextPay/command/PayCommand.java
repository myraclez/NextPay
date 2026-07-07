package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.economy.NextEconomy;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.Formatter;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PayCommand {

	public PayCommand() {}

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("pay")
				.requires(source -> source.getSender() instanceof Player)
				.requires(source -> source.getSender().hasPermission("nextpay.pay"))
				.then(Commands.argument("target", StringArgumentType.word())
						.suggests((ctx, builder) -> {
							String input = builder.getRemaining().toLowerCase();
							for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
								String name = offline.getName();
								if (name != null && name.toLowerCase().startsWith(input)) {
									builder.suggest(name);
								}
							}
							return builder.buildFuture();
						})
						.then(Commands.argument("amount", StringArgumentType.word())
								.suggests((ctx, builder) -> {
									builder.suggest("<amount>");
									return builder.buildFuture();
								})
								.executes(ctx -> {
									Player player = (Player) ctx.getSource().getSender();
									String targetName = StringArgumentType.getString(ctx, "target");
									String rawAmount = StringArgumentType.getString(ctx, "amount");

									OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

									if (!target.hasPlayedBefore()) {
										plugin.getMessageManager().sendMessage(player, "invalid-player");
										return Command.SINGLE_SUCCESS;
									}

									if (target.getUniqueId().equals(player.getUniqueId())) {
										plugin.getMessageManager().sendMessage(player, "pay-yourself");
										return Command.SINGLE_SUCCESS;
									}

									double amount;
									try {
										amount = me.myraclez.nextPay.util.Formatter.deformat(rawAmount);
									} catch (IllegalArgumentException e) {
										plugin.getMessageManager().sendMessage(player, "invalid-amount");
										return Command.SINGLE_SUCCESS;
									}

									if (amount <= 0) {
										plugin.getMessageManager().sendMessage(player, "invalid-amount");
										return Command.SINGLE_SUCCESS;
									}

									NextEconomy economy = plugin.getEconomy();
									if (economy == null) {
										player.sendMessage("Economy is null, something has gone seriously wrong, please contact administrators! " + System.currentTimeMillis());
										return Command.SINGLE_SUCCESS;
									}

									double finalAmount = amount;
									plugin.getDatabase().getSettingsAsync(target.getUniqueId()).thenAccept(playerSettings -> {
										new BukkitRunnable() {
											@Override
											public void run() {
												pay(plugin, economy, player, target, finalAmount, playerSettings.payments(), playerSettings.notifications());
											}
										}.runTask(plugin);
									});

									return Command.SINGLE_SUCCESS;
								})
						)
				)
				.build();
	}

	private static void pay(NextPay plugin, NextEconomy economy, Player player, OfflinePlayer target,
							double amount, boolean payments, boolean notifications) {
		if (!payments) {
			plugin.getMessageManager().sendMessage(player, "error.payments-disabled");
			return;
		}

		EconomyResponse withdraw = economy.withdrawPlayer(player, amount);
		if (!withdraw.transactionSuccess()) {
			plugin.getMessageManager().sendMessage(player, "not-enough-money");
			plugin.getLogger().severe(withdraw.errorMessage);
			return;
		}

		EconomyResponse deposit = economy.depositPlayer(target, amount);
		if (!deposit.transactionSuccess()) {
			plugin.getMessageManager().sendMessage(player, "player-not-received");
			economy.depositPlayer(player, amount);
			plugin.getLogger().severe(deposit.errorMessage);
			return;
		}

		plugin.getMessageManager().sendMessage(player, "messages.paid",
				"%player%", target.getName(), "%amount%", String.valueOf(me.myraclez.nextPay.util.Formatter.format(amount)));

		if (target.isOnline() && notifications) {
			Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
			onlineTarget.playSound(onlineTarget.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 3, 1);
			plugin.getMessageManager().sendMessage(onlineTarget, "messages.got-paid",
					"%player%", player.getName(), "%amount%", String.valueOf(Formatter.format(amount)));
		}
	}
}