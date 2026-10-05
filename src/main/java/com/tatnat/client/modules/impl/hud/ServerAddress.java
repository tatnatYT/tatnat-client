package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;

/** Shows the address of the server you're on ("Singleplayer" in your own worlds). */
public class ServerAddress extends TextHudModule {
	public ServerAddress() {
		super("Server Address", "Shows the IP of the server you are playing on", false, 0.0, 0.375);
		icon = com.tatnat.client.ui.render.Icons.Icon.GLOBE;
	}

	@Override
	protected String label() {
		return "IP";
	}

	@Override
	protected String value(boolean preview) {
		String ip = game().serverIp();
		if (ip != null) return ip;
		if (game().singleplayer()) return "Singleplayer";
		return preview ? "play.example.net" : "-";
	}
}
