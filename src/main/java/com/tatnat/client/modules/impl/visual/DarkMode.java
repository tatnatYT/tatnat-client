package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;

/** Dark inventories, chests and other containers: the grey panels and slots turn dark charcoal (1.17 - 1.21.5). */
public class DarkMode extends Module {
	private static DarkMode instance;

	private final SliderSetting darkness = add(new SliderSetting("Darkness", "How dark the panels get", 70, 20, 90, 5, "%"));

	public DarkMode() {
		super("Dark Mode", "Dark inventories and containers", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.MOON;
		instance = this;
	}

	public static boolean on() {
		return instance != null && instance.isEnabled();
	}

	/** Brightness multiplier for the container textures. */
	public static float shade() {
		return 1f - instance.darkness.floatValue() / 100f;
	}
}
