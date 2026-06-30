package me.myraclez.nextPay.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ColorUtil {

	private static final MiniMessage mm = MiniMessage.miniMessage();

	public static Component colorize(String text) {
		return mm.deserialize(text).decoration(TextDecoration.ITALIC, false);
	}
}
