package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;

import java.util.List;

public class NextPayCommand {

	public NextPayCommand() {}

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("nextpay")
				.requires(source -> source.getSender().hasPermission("nextpay.admin"))
				.executes(ctx -> {
					List<String> banner = plugin.getMessageManager().getMessageConfig().getStringList("banner");
					for (String string : banner) {
						ctx.getSource().getSender().sendMessage(ColorUtil.colorize(string.replace("%version%", plugin.getPluginMeta().getVersion())));
					}
					return Command.SINGLE_SUCCESS;
				})
				// Reload
				.then(Commands.literal("reload")
						.executes(ctx -> {
							long time = System.currentTimeMillis();
							plugin.reload();
							ctx.getSource().getSender().sendMessage(ColorUtil.colorize("<gray>Reloaded configuration in <#2baafb>" +
									(System.currentTimeMillis() - time) + "ms"));
							return Command.SINGLE_SUCCESS;
						})).build();
	}
}
