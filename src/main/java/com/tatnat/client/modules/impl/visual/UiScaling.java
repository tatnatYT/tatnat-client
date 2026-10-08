package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ModeSetting;

/**
 * Sets the size of the game's menus, inventory, chat and hotbar (the GUI Scale option) from here,
 * and puts it back to Auto when you turn the mod off.
 */
public class UiScaling extends Module {
	private final ModeSetting scale = add(new ModeSetting("Scale", "Size of the game's interface", "2", "1", "2", "3", "4", "5", "6"));

	private String applied = "";

	public UiScaling() {
		super("UI Scaling", "Change the size of the game's interface", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.MONITOR;
	}

	@Override
	protected void onEnable() {
		applied = "";
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		String want = scale.get();
		if (want.equals(applied)) return;
		applied = want;
		game().setGuiScale(Integer.parseInt(want));
	}

	@Override
	protected void onDisable() {
		game().setGuiScale(0);
	}
}
