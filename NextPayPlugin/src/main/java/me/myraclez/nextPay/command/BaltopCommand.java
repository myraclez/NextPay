package me.myraclez.nextPay.command;

import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.gui.BaltopGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BaltopCommand implements CommandExecutor {

	private final NextPay plugin;

	public BaltopCommand(NextPay plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
		if (!(commandSender instanceof Player player)) {
			commandSender.sendMessage("Only players can execute this command");
			return true;
		}

		if (!(player.hasPermission("nextpay.baltop"))) {
			player.sendMessage(Component.text("You don't have the permission to execute this command", NamedTextColor.RED));
			return true;
		}

		new BaltopGUI(plugin, 1).open(player);
		return true;
	}
}
