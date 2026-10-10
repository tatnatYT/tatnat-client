package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;

/** No longer in the menu (removed 2026-10-10); the fog mixins still ask {@link #disabled()}, which stays false. */
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
