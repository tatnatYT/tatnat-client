package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.account.AccountSwitcher;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;

/**
 * Discord: the tatnat launcher already shows "Playing Minecraft" on your Discord profile; this adds
 * the server you're on ("Playing on mc.hypixel.net"), or Singleplayer.
 */
public class DiscordStatus extends Module {
	private final BooleanSetting showServer = add(new BooleanSetting("Show Server IP", "Show which server you're on", true));

	private String sent;

	public DiscordStatus() {
		super("Discord", "Shows the server you're on in your Discord status", Category.UTILITY, true);
		icon = com.tatnat.client.ui.render.Icons.Icon.CHAT;
	}

	@Override
	protected void onEnable() {
		sent = null;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		String now = !showServer.on() || !game().inWorld() ? "" : game().singleplayer() ? "Singleplayer" : game().serverIp();
		if (now.equals(sent)) return;
		sent = now;
		AccountSwitcher.presence(now);
	}

	@Override
	protected void onDisable() {
		AccountSwitcher.presence("");
	}
}
