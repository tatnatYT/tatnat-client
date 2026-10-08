package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;

/** Adds the item's tier under its name in tooltips: [COMMON], [UNCOMMON], [RARE] or [EPIC], coloured to match. */
public class TierTagger extends Module {
	private static TierTagger instance;
	/** Colour codes per rarity (common, uncommon, rare, epic). */
	private static final String[] COLORS = {"§7", "§e", "§b", "§d"};

	public TierTagger() {
		super("Tier Tagger", "Shows each item's rarity in its tooltip", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.SPARKLE;
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** The tooltip line for a rarity (its enum ordinal and name), or null when the mod is off. */
	public static String line(int ordinal, String name) {
		if (!active()) return null;
		return COLORS[Math.max(0, Math.min(3, ordinal))] + "[" + name.toUpperCase(java.util.Locale.ROOT) + "]";
	}
}
