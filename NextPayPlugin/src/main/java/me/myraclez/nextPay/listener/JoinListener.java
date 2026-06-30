package me.myraclez.nextPay.listener;

import me.myraclez.nextPay.NextPay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

	private final NextPay plugin;

	public JoinListener(NextPay pl) {
		this.plugin = pl;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		plugin.getEconomy().createPlayerAccount(player);
		plugin.getDatabase().createSettingsEntry(player.getUniqueId());
	}
}
