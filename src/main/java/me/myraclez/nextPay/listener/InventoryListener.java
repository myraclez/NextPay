package me.myraclez.nextPay.listener;

import me.myraclez.nextPay.NextPay;
import me.myraclez.nextPay.gui.BaltopGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InventoryListener implements Listener {

	NextPay plugin;

	public InventoryListener(NextPay plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (event.getInventory().getHolder() instanceof BaltopGUI gui) {
			gui.handleClick(event);
		}
	}

	@EventHandler
	public void onDrag(InventoryDragEvent event) {
		if (event.getInventory().getHolder() instanceof BaltopGUI gui) {
			gui.handleDrag(event);
		}
	}
}
