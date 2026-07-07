package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
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
								if (payments) {
									plugin.getMessageManager().sendMessage(player, "pay-off");
								} else {
									plugin.getMessageManager().sendMessage(player, "pay-on");
								}

								plugin.getApi().setPayments(player.getUniqueId(), !payments);
							});
					return Command.SINGLE_SUCCESS;
				}).build();
	}
}
