package me.myraclez.nextPay.manager;

import lombok.Getter;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MessageManager {

	private final NextPay plugin;

	@Getter
	private FileConfiguration messageConfig;

	public MessageManager(NextPay plugin) {
		this.plugin = plugin;

		reload();
	}

	public void reload() {
		File messageFile = new File(plugin.getDataFolder(), "messages.yml");
		if (!messageFile.exists()) {
			plugin.saveResource("messages.yml", false);
		}

		messageConfig = YamlConfiguration.loadConfiguration(messageFile);
	}

	public void sendMessage(Player player, String key, String... replacements) {
		try {
			if (messageConfig == null) {
				plugin.getLogger().warning("Messages.yml doesn't exist, attempting to create it");
				reload();
			}
			String message = messageConfig.getString(key, "Uh oh! Message not found: "+ key);
			if (message.startsWith("Uh oh! Message not found:")) {
				plugin.getLogger().warning("Couldn't find message key " + key + "this is most likely a configuration issue");
			}

			for (int i = 0; i + 1 < replacements.length; i += 2) {
				message = message.replace(replacements[i], replacements[i + 1]);
			}

			Component finalMessage = ColorUtil.colorize(message);
			player.sendMessage(finalMessage);

		} catch (Exception e) {
			plugin.getLogger().severe("Couldn't send message for key " + ":" + e.getMessage());
			player.sendMessage(Component.text("This message doesn't exist, please contact administrators", NamedTextColor.RED));
		}

	}
}