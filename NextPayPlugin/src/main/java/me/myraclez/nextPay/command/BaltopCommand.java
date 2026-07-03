package me.myraclez.nextPay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.gui.BaltopGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BaltopCommand {

	public BaltopCommand() {}

	public static LiteralCommandNode<CommandSourceStack> create(NextPay plugin) {
		return Commands.literal("baltop")
				.requires(source -> source.getSender() instanceof Player)
				.requires(source -> source.getSender().hasPermission("nextpay.baltop"))
				.executes(ctx -> {
					Player player = (Player) ctx.getSource().getSender();

					if (player.isOnline()) {
						new BaltopGUI(plugin, 1).open(player);
						return Command.SINGLE_SUCCESS;
					} else {
						plugin.getLogger().severe("Player logged off, couldn't open Baltop GUI");
						return Command.SINGLE_SUCCESS;
					}
				})
				.build();
	}
}
