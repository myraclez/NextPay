package me.myraclez.nextPay.gui;

import lombok.Getter;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.Formatter;
import me.myraclez.nextPay.util.ItemCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaltopGUI implements InventoryHolder {

	private List<Map.Entry<UUID, Double>> balances;
	private final int SLOTS_PER_PAGE = 45;
	private final ConfigurationSection config;
	private final NextPay plugin;
	private final Inventory inventory;

	@Getter
	private int page;

	public BaltopGUI(NextPay plugin, int page) {
		this.plugin = plugin;
		this.page = page;
		config = plugin.getGuiConfigManager().getConfiguration().getConfigurationSection("baltop");
		this.inventory = Bukkit.createInventory(this, 54, ColorUtil.colorize(config.getString("title").replace("%page%", String.valueOf(page))));

		refresh();
	}

	public void refresh() {

		inventory.clear();

		plugin.getDatabase().getAllBalances().thenAccept(balances -> {
			Bukkit.getScheduler().runTask(plugin, () -> {

				this.balances = balances;

				if (balances == null || balances.isEmpty()) {
					plugin.getLogger().severe("Balances is null or empty");
					return;
				}

				ConfigurationSection next = config.getConfigurationSection("items.next");
				if (next != null && (balances.size() > page * SLOTS_PER_PAGE)) {
					inventory.setItem(next.getInt("slot"), itemFromSection(next));
				}

				ConfigurationSection previous = config.getConfigurationSection("items.previous");
				if (previous != null && !(page <= 1)) {
					inventory.setItem(previous.getInt("slot"), itemFromSection(previous));
				}

				ConfigurationSection refresh = config.getConfigurationSection("items.refresh");
				if (refresh != null) {
					inventory.setItem(refresh.getInt("slot"), itemFromSection(refresh));
				}

				final int lower = (page - 1) * SLOTS_PER_PAGE;
				final int upper = Math.min(balances.size(), lower + SLOTS_PER_PAGE);
				for (int i = lower; i < upper; i++) {
					Map.Entry<UUID, Double> entry = balances.get(i);
					final UUID player = entry.getKey();
					final double balance = entry.getValue();
					ItemStack head = new ItemStack(Material.PLAYER_HEAD);
					SkullMeta headMeta = (SkullMeta) head.getItemMeta();
					headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(player));
					List<Component> lore = new ArrayList<>();
					headMeta.displayName(ColorUtil.colorize(config.getString("format.name").replace("%player%", Bukkit.getOfflinePlayer(player).getName())));
					for (String s : config.getStringList("format.lore")) {
						lore.add(ColorUtil.colorize(s.replace("%balance%", String.valueOf(Formatter.format(balance))).replace("%position%", String.valueOf(i + 1))));
					}
					headMeta.lore(lore);
					head.setItemMeta(headMeta);
					inventory.setItem(i - lower, head);
				}
			});
		});
	}

	public ItemStack itemFromSection(ConfigurationSection section) {
		List<Component> lore = new ArrayList<>();
		for (String s : section.getStringList("lore")) {
			lore.add(ColorUtil.colorize(s));
		}
		return new ItemCreator(Material.matchMaterial(section.getString("material")), ColorUtil.colorize(section.getString("name")), lore).build();
	}

	public void open(Player player) {
		player.openInventory(inventory);
	}

	/*
		Click & Drag handling, passed from GuiListener.java
	 */

	public void handleClick(InventoryClickEvent event) {

		if (event.getRawSlot() < 54 || event.getClick().isShiftClick()) {
			event.setCancelled(true);
		}
		int slot = event.getRawSlot();

		ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getType() == Material.AIR) {
			return;
		}

		if (slot == config.getInt("items.next.slot") && clicked.getType() == Material.matchMaterial(config.getString("items.next.material"))) {
			if (balances.size() > page * SLOTS_PER_PAGE) {
				new BaltopGUI(plugin, page + 1).open((Player) event.getWhoClicked());
			}
		}

		if (slot == config.getInt("items.previous.slot") && clicked.getType() == Material.matchMaterial(config.getString("items.previous.material"))) {
			if (page > 1) {
				new BaltopGUI(plugin, page - 1).open((Player) event.getWhoClicked());
			}
		}

		if (slot == config.getInt("items.refresh.slot") && clicked.getType() == (Material.matchMaterial(config.getString("items.refresh.material")))) {
			refresh();
		}
	}

	public void handleDrag(InventoryDragEvent event) {
		for (Integer i : event.getRawSlots()) {
			if (i < 53) {
				event.setCancelled(true);
			}
		}
	}

	@Override
	public @NotNull Inventory getInventory() {
		return inventory;
	}
}
