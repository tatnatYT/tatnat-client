package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.util.KeyCodes;

/** Hold a key to look behind you (the front-facing third-person view) without turning around. */
public class Snaplook extends Module {
	private final KeybindSetting key = add(new KeybindSetting("Key", "Hold to look behind you", KeyCodes.V));
	private final ModeSetting view = add(new ModeSetting("View", "Which camera to switch to while held", "Behind You", "Behind You", "Over Shoulder"));

	private int before = -1;

	public Snaplook() {
		super("Snaplook", "Hold a key to look behind you", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.EYE;
	}

	@Subscribe
	public void onKey(Events.Key e) {
		if (!key.isBound() || e.key != key.get()) return;
		if (e.action == KeyCodes.PRESS && e.inGame && before < 0) {
			before = game().perspective();
			game().setPerspective(view.is("Behind You") ? 2 : 1);
		} else if (e.action == KeyCodes.RELEASE && before >= 0) {
			game().setPerspective(before);
			before = -1;
		}
	}

	@Override
	protected void onDisable() {
		if (before >= 0) game().setPerspective(before);
		before = -1;
	}
}
