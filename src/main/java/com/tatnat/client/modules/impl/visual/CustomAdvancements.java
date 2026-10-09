package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;

/** A dark advancements screen: the window and the tab backgrounds drawn in dark charcoal (1.17 - 1.21.5). */
public class CustomAdvancements extends Module {
	private static CustomAdvancements instance;

	private final BooleanSetting dark = add(new BooleanSetting("Dark Mode", "Dark window and backgrounds", true));
	private final SliderSetting darkness = add(new SliderSetting("Darkness", "How dark it gets", 65, 20, 90, 5, "%"));

	public CustomAdvancements() {
		super("Custom Advancements", "A dark advancements screen", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.MEDAL;
		instance = this;
	}

	public static boolean dark() {
		return instance != null && instance.isEnabled() && instance.dark.on();
	}

	public static float shade() {
		return 1f - instance.darkness.floatValue() / 100f;
	}
}
