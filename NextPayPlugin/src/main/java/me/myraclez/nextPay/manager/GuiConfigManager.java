package me.myraclez.nextPay.manager;

import lombok.Getter;
import me.myraclez.nextPay.NextPay;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class GuiConfigManager {

	NextPay plugin;
	@Getter
	private YamlConfiguration configuration;

	public GuiConfigManager(NextPay plugin) {
		this.plugin = plugin;

		reload();
	}

	public void reload() {
		File file = new File(plugin.getDataFolder(), "guis.yml");
		if (!file.exists()) {
			plugin.saveResource("guis.yml", false);
		}

		configuration = YamlConfiguration.loadConfiguration(file);
	}

}
