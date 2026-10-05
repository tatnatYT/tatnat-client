package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.util.KeyCodes;
import com.tatnat.client.util.Keys;

/**
 * Auto text hotkeys: up to five keys that each send a chat message or command (anything
 * starting with "/"), or open chat with the text typed in so you can finish it.
 */
public class Macros extends Module {
	private static final int SLOTS = 5;
	private final ModeSetting mode = add(new ModeSetting("Mode", "Send straight away or type it into chat for you", "Send Instantly", "Send Instantly", "Type Into Chat"));
	private final KeybindSetting[] keys = new KeybindSetting[SLOTS];
	private final TextSetting[] texts = new TextSetting[SLOTS];

	public Macros() {
		super("Auto Text Hotkeys", "Keys that send a message or command", Category.UTILITY, false);
		icon = Icons.Icon.KEY;
		String[] examples = {"/l", "gg", "", "", ""};
		for (int i = 0; i < SLOTS; i++) {
			keys[i] = add(new KeybindSetting("Hotkey " + (i + 1), "Key for message " + (i + 1), i == 0 ? KeyCodes.KP_1 : Keys.NONE));
			texts[i] = add(new TextSetting("Message " + (i + 1), "Text or /command to send", examples[i], 256));
		}
	}

	@Subscribe
	public void onKey(Events.Key e) {
		if (!e.inGame || e.action != KeyCodes.PRESS || !game().inWorld()) return;
		for (int i = 0; i < SLOTS; i++) {
			String text = texts[i].get().trim();
			if (keys[i].get() != e.key || text.isEmpty()) continue;
			if (mode.is("Type Into Chat")) {
				game().openChat(text);
			} else if (text.startsWith("/")) {
				game().sendCommand(text.substring(1));
			} else {
				game().sendChat(text);
			}
			e.cancel();
			return;
		}
	}
}
