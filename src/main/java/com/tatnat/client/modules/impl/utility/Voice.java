package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.platform.Gfx;

/**
 * Voice: proximity voice chat comes from Simple Voice Chat (the server needs it too). This shows
 * whether it's installed, a small "Voice" badge in game, and a button to get it.
 */
public class Voice extends Module {
	private static final String PAGE = "https://modrinth.com/plugin/simple-voice-chat";

	public Voice() {
		super("Voice", "Proximity voice chat (with Simple Voice Chat)", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.MIC;
		add(new ActionSetting("Simple Voice Chat", "Install it from the launcher's Mods tab, or open its page",
				() -> installed() ? "Installed" : "Get it", () -> {
					if (!installed()) TatnatClient.game().openUrl(PAGE);
				}));
	}

	private static boolean installed() {
		try {
			return TatnatClient.game().modLoaded("voicechat");
		} catch (Throwable t) {
			return false;
		}
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden()) return;
		Gfx g = e.gfx;
		String text = installed() ? "Voice ready" : "Voice: install Simple Voice Chat";
		int w = g.mcTextWidth(text, false), x = game().guiWidth() - w - 8, y = 6;
		g.rect(x - 4, y - 3, x + w + 4, y + 11, 0x80000000);
		g.mcText(text, x, y, installed() ? 0xFF55FF55 : 0xFFFFFF55, false, false);
	}
}
