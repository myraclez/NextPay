package me.myraclez.nextPay;

import lombok.Getter;
import me.myraclez.nextPay.api.NextPayAPIImpl;
import me.myraclez.nextPay.command.*;
import me.myraclez.nextPay.database.Database;
import me.myraclez.nextPay.database.impl.MySQLDatabase;
import me.myraclez.nextPay.database.impl.SQLiteDatabase;
import me.myraclez.nextPay.economy.NextEconomy;
import me.myraclez.nextPay.listener.InventoryListener;
import me.myraclez.nextPay.listener.JoinListener;
import me.myraclez.nextPay.manager.GuiConfigManager;
import me.myraclez.nextPay.manager.MessageManager;
import me.myraclez.nextPayAPI.NextPayAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class NextPay extends JavaPlugin {

	private static NextPayAPI api;

	@Getter
	private GuiConfigManager guiConfigManager;
	@Getter
	private MessageManager messageManager;
	@Getter
	private NextEconomy economy;
	@Getter
	private Database database;

	public static NextPayAPI getAPI() {
		return api;
	}

	@Override
	public void onEnable() {

		saveDefaultConfig();

		initializeDatabase();
		initializeManagers();

		// Register EconomyProvider using Vault
		economy = new NextEconomy(this);
		Bukkit.getServicesManager().register(Economy.class, economy,this, ServicePriority.Normal);

		registerCommands();
		registerListeners();

		api = new NextPayAPIImpl(this);
	}

	@Override
	public void onDisable() {
		if (database != null) {
			database.disconnect();
		}
	}

	public void reload() {
		messageManager.reload();
		guiConfigManager.reload();
		saveDefaultConfig();
	}
	/*
		Initialize Listeners, Commands, Managers & Database
	 */

	public void registerCommands() {
		getCommand("balance").setExecutor(new BalanceCommand(this));
		getCommand("pay").setExecutor(new PayCommand(this));
		getCommand("balancetop").setExecutor(new BaltopCommand(this));
		getCommand("economy").setExecutor(new EconomyCommand(this));
		getCommand("nextpay").setExecutor(new NextPayCommand(this));
	}

	public void registerListeners() {
		Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
		Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
	}

	public void initializeDatabase() {
		if (getConfig().getString("database.type").equalsIgnoreCase("sqlite")) {
			database = new SQLiteDatabase(this);
			database.connect();
		} else if (getConfig().getString("database.type").equalsIgnoreCase("mysql")) {
			database = new MySQLDatabase(this);
			database.connect();
		} else {
			getLogger().severe("Invalid database type, defaulting to sqlite!");
			database = new SQLiteDatabase(this);
			database.connect();
		}
	}

	public void initializeManagers() {
		messageManager = new MessageManager(this);
		guiConfigManager = new GuiConfigManager(this);
	}
}
