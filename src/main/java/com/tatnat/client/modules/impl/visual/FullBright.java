package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;

/**
 * Lights up caves and nights. Overrides the gamma that the lightmap is built from (only inside
 * {@code LightTexture.updateLightTexture}, see {@code LightTextureMixin}), so your saved
 * brightness option is never touched and turning the mod off puts everything back instantly.
 */
public class FullBright extends Module {
	public static FullBright INSTANCE;

	public final SliderSetting level = add(new SliderSetting("Brightness", "How bright dark areas become (15 = fully lit)", 15, 1, 15, 0.5, ""));

	public FullBright() {
		super("Full Bright", "See in the dark without torches or night vision", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.SUN;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}
}
