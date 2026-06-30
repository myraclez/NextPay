package me.myraclez.nextPay.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemCreator {

	private final ItemStack itemStack;
	private final ItemMeta itemMeta;

	public ItemCreator(Material material, Component displayName, List<Component> lore) {
		this.itemStack = new ItemStack(material);
		itemMeta = itemStack.getItemMeta();
		itemMeta.displayName(displayName);
		itemMeta.lore(lore);
	}

	public ItemCreator(Material material, Component displayName) {
		this.itemStack = new ItemStack(material);
		itemMeta = itemStack.getItemMeta();
		itemMeta.displayName(displayName);
	}

	public ItemCreator(Material material) {
		this.itemStack = new ItemStack(material);
		itemMeta = itemStack.getItemMeta();
	}


	public ItemCreator withLore(List<Component> lore) {
		itemMeta.lore(lore);
		return this;
	}

	public ItemCreator withName(Component displayName) {
		itemMeta.displayName(displayName);
		return this;
	}

	public ItemCreator addEnchant(Enchantment enchantment, int level) {
		itemStack.addEnchantment(enchantment, level);
		return this;
	}

	public ItemStack build() {
		this.itemStack.setItemMeta(itemMeta);
		return itemStack;
	}
}