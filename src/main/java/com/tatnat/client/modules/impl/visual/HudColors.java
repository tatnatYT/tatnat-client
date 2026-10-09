package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ColorSetting;

/**
 * Hearts and Armor Bar: tint the vanilla health hearts and armor icons in your own colours
 * (hearts 1.20.1 - 1.21.5, armor 1.20.5 - 1.21.5).
 */
public final class HudColors {
	private HudColors() {
	}

	static Hearts hearts;
	static ArmorBar armor;

	public static boolean hearts() {
		return hearts != null && hearts.isEnabled();
	}

	public static int heartColor() {
		return hearts.color.color(0);
	}

	public static boolean armor() {
		return armor != null && armor.isEnabled();
	}

	public static int armorColor() {
		return armor.color.color(0);
	}

	/** Custom colour for your health hearts. */
	public static class Hearts extends Module {
		final ColorSetting color = add(new ColorSetting("Custom Color", "Colour for your hearts", 0xFFFF55AA, true));

		public Hearts() {
			super("Hearts", "Recolour your health hearts", Category.VISUAL, false);
			icon = com.tatnat.client.ui.render.Icons.Icon.HEART;
			HudColors.hearts = this;
		}
	}

	/** Custom colour for the armor icons above your health. */
	public static class ArmorBar extends Module {
		final ColorSetting color = add(new ColorSetting("Color", "Colour for the armor icons", 0xFF4EB1FF, true));

		public ArmorBar() {
			super("Armor Bar", "Recolour the armor icons above your health", Category.VISUAL, false);
			icon = com.tatnat.client.ui.render.Icons.Icon.SHIELD;
			HudColors.armor = this;
		}
	}
}
