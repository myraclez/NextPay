package me.myraclez.nextPay;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
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
import me.myraclez.nextPayAPI.NextPayProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class NextPay extends JavaPlugin {

	private NextPayAPI api;

	@Getter
	private GuiConfigManager guiConfigManager;
	@Getter
	private MessageManager messageManager;
	@Getter
	private NextEconomy economy;
	@Getter
	private Database database;


	@Override
	public void onEnable() {
		api = new NextPayAPIImpl(this);

		NextPayProvider.register(api);

		saveDefaultConfig();

		initializeDatabase();
		initializeManagers();

		// Register EconomyProvider using Vault
		economy = new NextEconomy(this);
		Bukkit.getServicesManager().register(Economy.class, economy,this, ServicePriority.Normal);

		registerCommands();
		registerListeners();
	}

	@Override
	public void onDisable() {
		if (database != null) {
			database.disconnect();
		}

		NextPayProvider.unregister();
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

		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			commands.registrar().register(PayCommand.create(this), "Payments between to players");
		});

		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			commands.registrar().register(BalanceCommand.create(this), "Check balance of players", List.of("balance"));
		});

		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			commands.registrar().register(BaltopCommand.create(this), "Open top-balances menu", List.of("balancetop"));
		});

		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			commands.registrar().register(EconomyCommand.create(this), "Admin economy commands", List.of("eco"));
		});

		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			commands.registrar().register(NextPayCommand.create(this), "NextPay main command");
		});
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
