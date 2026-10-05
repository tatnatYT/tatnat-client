package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Clean wireframe hitboxes (like F3+B, but with your own colours and width) plus a line showing
 * where each entity is looking. Each version's platform layer draws them with these settings.
 */
public class Hitboxes extends Module {
	public static Hitboxes INSTANCE;

	public final ModeSetting targets = add(new ModeSetting("Show On", "Which entities get a hitbox", "Players", "Players", "Living", "All"));
	public final ColorSetting boxColor = add(new ColorSetting("Box Color", "Colour of the hitbox", 0xFFFFFFFF, true));
	public final SliderSetting width = add(new SliderSetting("Line Thickness", "Width of the lines", 1.5, 1, 5, 0.5, "px"));
	public final BooleanSetting lookLine = add(new BooleanSetting("Line of Sight", "Line showing where they're looking", true));
	public final ColorSetting lookColor = add(new ColorSetting("Line of Sight Color", "Colour of the look line", 0xFFFF3B3B, false));

	public Hitboxes() {
		super("Hitboxes", "Wireframe boxes around players and mobs", Category.VISUAL, false);
		icon = Icons.Icon.BOX;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}
}
