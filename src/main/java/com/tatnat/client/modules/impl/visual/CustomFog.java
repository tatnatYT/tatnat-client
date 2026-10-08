package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;

/** Removes the distance fog so the world fades out at your render distance only (1.14 - 1.21.5). */
public class CustomFog extends Module {
	private static CustomFog instance;

	public CustomFog() {
		super("Custom Fog", "Removes distance fog", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.CLOUD;
		instance = this;
	}

	public static boolean disabled() {
		return instance != null && instance.isEnabled();
	}
}
