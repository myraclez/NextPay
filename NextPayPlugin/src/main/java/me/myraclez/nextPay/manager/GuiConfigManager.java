package me.myraclez.nextPay.manager;

import lombok.Getter;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.ItemCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GuiConfigManager {

	private final NextPay plugin;
	@Getter
	public YamlConfiguration configuration;

	private ConfigurationSection refresh;
	private ConfigurationSection prev;
	private ConfigurationSection next;

	@Getter
	private int refreshSlot;
	@Getter
	private int nextSlot;
	@Getter
	private int prevSlot;
	@Getter
	private ItemStack refreshItem;
	@Getter
	private ItemStack nextItem;
	@Getter
	private ItemStack prevItem;
	@Getter
	private String nameFormat;
	@Getter
	private List<String> loreFormat;

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

		refresh = configuration.getConfigurationSection("baltop.items.refresh");
		refreshSlot = refresh.getInt("slot");
		prev = configuration.getConfigurationSection("baltop.items.previous");
		prevSlot = prev.getInt("slot");
		next = configuration.getConfigurationSection("baltop.items.next");
		nextSlot = next.getInt("slot");

		refreshItem = refreshItem();
		prevItem = prevItem();
		nextItem = nextItem();

		nameFormat = configuration.getString("baltop.format.name", "<gray>%player%");

		List<String> list = configuration.getStringList("baltop.format.lore");

		loreFormat = list == null ? List.of("<green>$%balance% <white>(#%position%)") : list;
	}

	public ItemStack refreshItem() {
		if (refresh == null) {
			return new ItemCreator(Material.PAPER, ColorUtil.colorize("<#2baafb>Refresh Page"),List.of(ColorUtil.colorize("<gray>Click to refresh this page"))).build();
		}

		List<Component> lore = new ArrayList<>();

		for (String s : refresh.getStringList("lore")) {
			lore.add(ColorUtil.colorize(s));
		}

		return new ItemCreator(Material.matchMaterial(refresh.getString("material", "PAPER")),
				ColorUtil.colorize(refresh.getString("name", "<#2baafb>Refresh Page")),
				lore).build();
	}

	public ItemStack prevItem() {
		if (prev == null) {
			return new ItemCreator(Material.ARROW, ColorUtil.colorize("<#2baafb>Previous Page"),List.of(ColorUtil.colorize("<gray>Click to go to previous page"))).build();
		}

		List<Component> lore = new ArrayList<>();

		for (String s : prev.getStringList("lore")) {
			lore.add(ColorUtil.colorize(s));
		}

		return new ItemCreator(Material.matchMaterial(prev.getString("material", "ARROW")),
				ColorUtil.colorize(prev.getString("name", "<#2baafb>Previous Page")),
				lore).build();
	}

	public ItemStack nextItem() {
		if (next == null) {
			return new ItemCreator(Material.ARROW, ColorUtil.colorize("<#2baafb>Next Page"),List.of(ColorUtil.colorize("<gray>Click to go to next page"))).build();
		}

		List<Component> lore = new ArrayList<>();

		for (String s : next.getStringList("lore")) {
			lore.add(ColorUtil.colorize(s));
		}

		return new ItemCreator(Material.matchMaterial(next.getString("material", "ARROW")),
				ColorUtil.colorize(next.getString("name", "<#2baafb>Next Page")),
				lore).build();
	}
}
