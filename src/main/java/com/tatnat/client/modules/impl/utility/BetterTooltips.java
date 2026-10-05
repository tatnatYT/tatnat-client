package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Restyled item tooltips (your own background and border colours) with extra info added at the
 * bottom: durability, the item's id and how much item data (NBT) it carries. The platform layer
 * draws the box and adds the lines using these settings.
 */
public class BetterTooltips extends Module {
	public static BetterTooltips INSTANCE;

	public final ColorSetting background = add(new ColorSetting("Background Color", "Tooltip background", 0xF0141416, false));
	public final ColorSetting border = add(new ColorSetting("Border Color", "Tooltip border (Chroma works too)", 0xFFE5323E, true));
	public final BooleanSetting durability = add(new BooleanSetting("Show Durability", "Durability left on tools and armour", true));
	public final BooleanSetting itemId = add(new BooleanSetting("Show Item ID", "The item's id, like minecraft:diamond_sword", true));
	public final BooleanSetting components = add(new BooleanSetting("Show Data Count", "How much item data (NBT) the item has", false));

	public BetterTooltips() {
		super("Better Tooltips", "Nicer item tooltips with durability and item IDs", Category.UTILITY, false);
		icon = Icons.Icon.TOOLTIP;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	/** Colour code for "left / max" durability text. */
	public static String durabilityColorCode(int left, int max) {
		float f = left / (float) Math.max(1, max);
		return f > 0.6f ? "§a" : f > 0.3f ? "§e" : "§c";
	}
}
