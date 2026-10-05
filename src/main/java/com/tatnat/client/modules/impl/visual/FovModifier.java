package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Sets the field of view beyond vanilla's 110 limit and can stop it changing when you sprint,
 * fly or get Speed (hooks in {@code GameRendererMixin} and {@code AbstractClientPlayerMixin}).
 */
public class FovModifier extends Module {
	public static FovModifier INSTANCE;

	public final SliderSetting fov = add(new SliderSetting("FOV", "Field of view in degrees", 90, 30, 140, 1, "°"));
	public final BooleanSetting staticFov = add(new BooleanSetting("Static FOV", "Don't zoom in/out when sprinting, flying or with Speed", true));

	public FovModifier() {
		super("FOV Modifier", "Custom field of view, also above 110", Category.VISUAL, false);
		icon = Icons.Icon.EYE;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}
}
