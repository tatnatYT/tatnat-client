package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

/** {@code [RAM: 512MB / 2048MB]}. */
public class MemoryUsage extends TextHudModule {
	private final BooleanSetting percentage = add(new BooleanSetting("Show Percentage", "Show a percentage instead of megabytes", false));

	public MemoryUsage() {
		super("Memory Usage", "Shows how much RAM the game is using", false, 0.0, 0.225);
		icon = com.tatnat.client.ui.render.Icons.Icon.RAM;
	}

	@Override
	protected String label() {
		return "RAM";
	}

	@Override
	protected String value(boolean preview) {
		Runtime rt = Runtime.getRuntime();
		long max = rt.maxMemory() / 1048576L;
		long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
		if (percentage.on()) return (used * 100 / Math.max(1, max)) + "%";
		return used + "MB / " + max + "MB";
	}
}
