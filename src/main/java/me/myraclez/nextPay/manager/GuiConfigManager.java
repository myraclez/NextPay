package me.myraclez.nextPay.manager;

import lombok.Getter;
import me.myraclez.nextPay.NextPay;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class GuiConfigManager {

	NextPay plugin;

	private File file;
	@Getter
	private YamlConfiguration configuration;

	public GuiConfigManager(NextPay plugin) {
		this.plugin = plugin;

		reload();
	}

	public void reload() {
		this.file = new File(plugin.getDataFolder(), "guis.yml");
		if (!file.exists()) {
			plugin.saveResource("guis.yml", false);
		}

		configuration = YamlConfiguration.loadConfiguration(file);
	}

}
