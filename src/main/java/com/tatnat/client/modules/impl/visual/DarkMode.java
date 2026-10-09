package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;

/**
 * Dark inventories, chests and other containers (1.17 - 1.21.11): a dark charcoal panel is laid over
 * the grey background right after the game draws it (so slots and items stay bright on top) and the
 * dark-grey labels ("Inventory", "Crafting") turn light so they stay readable.
 */
public class DarkMode extends Module {
	private static DarkMode instance;

	private final SliderSetting darkness = add(new SliderSetting("Darkness", "How dark the panels get", 75, 20, 95, 5, "%"));

	public DarkMode() {
		super("Dark Mode", "Dark inventories and containers", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.MOON;
		instance = this;
	}

	public static boolean on() {
		return instance != null && instance.isEnabled();
	}

	/** The overlay colour: charcoal (#1e1e22) at the chosen strength. */
	public static int overlay() {
		int a = Math.round(255 * instance.darkness.floatValue() / 100f);
		return a << 24 | 0x1E1E22;
	}

	/** Vanilla's dark-grey label colour becomes light grey; anything else is left alone. */
	public static int label(int color) {
		return (color & 0xFFFFFF) == 0x404040 ? (color & 0xFF000000) | 0xD8D8D8 : color;
	}
}
