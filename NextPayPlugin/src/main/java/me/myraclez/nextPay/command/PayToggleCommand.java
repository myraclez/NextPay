package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PayToggleCommand {

	public PayToggleCommand() {}

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("paytoggle")
				.requires(source -> source.getSender() instanceof Player)
				.requires(source -> source.getSender().hasPermission("nextpay.toggle"))
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();

					plugin.getApi().isPaymentsAsync(player.getUniqueId())
							.thenAccept(payments -> {
								Bukkit.getScheduler().runTask(plugin, new Runnable() {
									@Override
									public void run() {
										if (payments) {
											plugin.getMessageManager().sendMessage(player, "messages.pay-off");
										} else {
											plugin.getMessageManager().sendMessage(player, "messages.pay-on");
										}

										plugin.getEconomyManager().togglePayments(player.getUniqueId());
									}
								});
							});
					return Command.SINGLE_SUCCESS;
				}).build();
	}
}
