package me.myraclez.nextPay.gui;

import lombok.Getter;
import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.manager.GuiConfigManager;
import me.myraclez.nextPay.util.ColorUtil;
import me.myraclez.nextPay.util.Formatter;
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
	private long lastRefresh;

	private final int nextSlot, prevSlot, refreshSlot;
	private final Material nextMaterial, prevMaterial, refreshMaterial;

	@Getter
	private int page;

	public BaltopGUI(NextPay plugin, int page) {
		this.plugin = plugin;
		this.page = page;
		config = plugin.getGuiConfigManager().getConfiguration().getConfigurationSection("baltop");
		this.inventory = Bukkit.createInventory(this, 54, ColorUtil.colorize(config.getString("title").replace("%page%", String.valueOf(page))));

		nextSlot = config.getInt("items.next.slot");
		prevSlot = config.getInt("items.previous.slot");
		refreshSlot = config.getInt("items.refresh.slot");
		nextMaterial = Material.matchMaterial(config.getString("items.next.material", "ARROW"));
		prevMaterial = Material.matchMaterial(config.getString("items.previous.material", "ARROW"));
		refreshMaterial = Material.matchMaterial(config.getString("items.refresh.material", "PAPER"));

		refresh();
		lastRefresh = System.currentTimeMillis();
	}

	public void refresh() {

		inventory.clear();

		balances = plugin.getEconomyManager().getAllBalances();

		if (balances == null || balances.isEmpty()) {
			plugin.getLogger().severe("Balances is null or empty");
			return;
		}

		GuiConfigManager gui = plugin.getGuiConfigManager();

		if ((balances.size() > page * SLOTS_PER_PAGE)) {
			inventory.setItem(gui.getNextSlot(), gui.getNextItem());
		}

		if (!(page <= 1)) {
			inventory.setItem(gui.getPrevSlot(), gui.getPrevItem());
		}

		inventory.setItem(gui.getRefreshSlot(), gui.getRefreshItem());

		final String nameFormat = gui.getNameFormat();
		final List<String> loreFormat = gui.getLoreFormat();

		final int lower = (page - 1) * SLOTS_PER_PAGE;
		final int upper = Math.min(balances.size(), lower + SLOTS_PER_PAGE);

		for (int i = lower; i < upper; i++) {
			Map.Entry<UUID, Double> entry = balances.get(i);
			final UUID playerId = entry.getKey();
			final String formattedBalance = String.valueOf(Formatter.format(entry.getValue()));
			final String position = String.valueOf(i + 1);

			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta headMeta = (SkullMeta) head.getItemMeta();
			headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerId));
			headMeta.displayName(ColorUtil.colorize(
					nameFormat.replace("%player%", plugin.getEconomyManager().getName(playerId))
			));

			List<Component> lore = new ArrayList<>(loreFormat.size());
			for (String s : loreFormat) {
				lore.add(ColorUtil.colorize(
						s.replace("%balance%", formattedBalance).replace("%position%", position)
				));
			}
			headMeta.lore(lore);
			head.setItemMeta(headMeta);
			inventory.setItem(i - lower, head);
		}

		lastRefresh = System.currentTimeMillis();
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

		if (slot == nextSlot && clicked.getType() == nextMaterial) {
			if (balances.size() > page * SLOTS_PER_PAGE) {
				new BaltopGUI(this.plugin, this.page + 1).open((Player) event.getWhoClicked());
			}
		}

		if (slot == prevSlot && clicked.getType() == prevMaterial) {
			if (page > 1) {
				new BaltopGUI(this.plugin, this.page - 1).open((Player) event.getWhoClicked());
			}
		}

		if (slot == refreshSlot && clicked.getType() == refreshMaterial) {
			if (System.currentTimeMillis() - this.lastRefresh > plugin.getConfig().getLong("block-refresh-time", 2000L)) {
				refresh();
			}
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
