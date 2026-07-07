package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
import org.bukkit.entity.Player;

public class PayNotificationsToggle {

	public PayNotificationsToggle() {}

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("paynotificationstoggle")
				.requires(source -> source.getSender() instanceof Player)
				.requires(source -> source.getSender().hasPermission("nextpay.toggle"))
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();

					plugin.getApi().isNotificationsAsync(player.getUniqueId())
							.thenAccept(notifications -> {
								if (notifications) {
									plugin.getMessageManager().sendMessage(player, "notifications-off");
								} else {
									plugin.getMessageManager().sendMessage(player, "notifications-on");
								}

								plugin.getApi().setPayments(player.getUniqueId(), !notifications);
							});
					return Command.SINGLE_SUCCESS;
				}).build();
	}

}
