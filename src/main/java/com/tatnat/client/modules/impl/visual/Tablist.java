package com.tatnat.client.modules.impl.visual;

import java.util.Locale;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.TextSetting;

/** Tablist: your friends stand out in the player list (Tab), in the colour you pick (1.14+). */
public class Tablist extends Module {
	private static Tablist instance;
	private static final String[] CODES = {"a", "b", "e", "d", "6", "c"};

	private final TextSetting friends = add(new TextSetting("Friends", "Player names, separated by commas", "", 256));
	private final ModeSetting color = add(new ModeSetting("Friend Color", "Colour for your friends' names", "Green", "Green", "Aqua", "Yellow", "Pink", "Gold", "Red"));

	public Tablist() {
		super("Tablist", "Highlights your friends in the player list", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.LIST;
		instance = this;
	}

	/** The highlighted text for a tab-list name, or null to leave it as it is. */
	public static String highlight(String shown) {
		Tablist t = instance;
		if (t == null || !t.isEnabled() || shown == null) return null;
		String plain = shown.replaceAll("§.", "");
		for (String f : t.friends.get().split(",")) {
			String name = f.trim();
			if (name.isEmpty()) continue;
			// Ranks and tags can sit around the name, so match it as a whole word.
			if ((" " + plain.toLowerCase(Locale.ROOT) + " ").matches(".*[^a-z0-9_]" + java.util.regex.Pattern.quote(name.toLowerCase(Locale.ROOT)) + "[^a-z0-9_].*")) {
				int i = java.util.Arrays.asList("Green", "Aqua", "Yellow", "Pink", "Gold", "Red").indexOf(t.color.get());
				return "§" + CODES[Math.max(0, i)] + plain;
			}
		}
		return null;
	}
}
