package com.tatnat.client.modules.impl.hud;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

/**
 * Hypixel helper: counts the coins and tokens you earn this session from the chat messages, and
 * shows which game you're in. Only visible while you're on Hypixel.
 */
public class Hypixel extends TextHudModule {
	private static final Pattern COINS = Pattern.compile("\\+(\\d[\\d,]*) (coins|tokens)", Pattern.CASE_INSENSITIVE);
	private static final Pattern GAME = Pattern.compile("(?:Sending you to|You are now playing|Welcome to) ([A-Za-z ]+?)(?:!|\\.|$)");

	private final BooleanSetting showCoins = add(new BooleanSetting("Show Coins", "Coins and tokens earned this session", true));
	private final BooleanSetting showGame = add(new BooleanSetting("Game Detection", "Show which game you're in", true));

	private long coins, tokens;
	private String gameName = "Lobby";

	public Hypixel() {
		super("Hypixel", "Coins earned and current game on Hypixel", false, 0.75, 0.375);
		icon = com.tatnat.client.ui.render.Icons.Icon.BED;
	}

	private boolean onHypixel() {
		String ip = game().serverIp();
		return ip != null && ip.toLowerCase(Locale.ROOT).contains("hypixel");
	}

	@Subscribe
	public void onChat(Events.Chat e) {
		if (!onHypixel()) return;
		String text = e.text.replaceAll("§.", "");
		Matcher m = COINS.matcher(text);
		while (m.find()) {
			long n = Long.parseLong(m.group(1).replace(",", ""));
			if (m.group(2).toLowerCase(Locale.ROOT).startsWith("coin")) coins += n;
			else tokens += n;
		}
		Matcher g = GAME.matcher(text);
		if (g.find()) gameName = g.group(1).trim();
	}

	@Override
	protected String label() {
		return "Hypixel";
	}

	@Override
	protected String value(boolean preview) {
		StringBuilder s = new StringBuilder();
		if (showGame.on()) s.append(preview ? "Bed Wars" : gameName);
		if (showCoins.on()) {
			if (s.length() > 0) s.append(" | ");
			s.append('+').append(preview ? 340 : coins).append(" coins");
			if (preview || tokens > 0) s.append(", +").append(preview ? 25 : tokens).append(" tokens");
		}
		return s.toString();
	}

	@Override
	protected long draw(com.tatnat.client.platform.Gfx g, boolean preview) {
		if (!preview && !onHypixel()) return size(0, 0);
		return super.draw(g, preview);
	}
}
