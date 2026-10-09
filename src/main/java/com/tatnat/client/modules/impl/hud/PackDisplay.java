package com.tatnat.client.modules.impl.hud;

import java.util.List;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

/** {@code Pack: Faithful 32x}: the texture pack you're using (the top one when several are on). */
public class PackDisplay extends TextHudModule {
	private final BooleanSetting all = add(new BooleanSetting("Show All", "List every pack that's on, not just the top one", false));

	public PackDisplay() {
		super("Pack Display", "Shows which texture pack is active", false, 0.75, 0.225);
		icon = com.tatnat.client.ui.render.Icons.Icon.PALETTE;
	}

	private static String clean(String id) {
		String s = id.startsWith("file/") ? id.substring(5) : id;
		if (s.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) s = s.substring(0, s.length() - 4);
		// Strip Minecraft colour codes some packs put in their file names.
		return s.replaceAll("§.", "").trim();
	}

	private static boolean builtIn(String id) {
		return id.equals("vanilla") || id.equals("fabric") || id.startsWith("fabric") || id.equals("mod_resources")
				|| id.equals("programmer_art") || id.equals("high_contrast") || id.startsWith("minecraft");
	}

	@Override
	protected String label() {
		return "Pack";
	}

	@Override
	protected String value(boolean preview) {
		if (preview) return "Faithful 32x";
		List<String> packs = game().resourcePacks();
		StringBuilder out = new StringBuilder();
		for (int i = packs.size() - 1; i >= 0; i--) {
			String id = packs.get(i);
			if (builtIn(id)) continue;
			if (out.length() > 0) out.append(", ");
			out.append(clean(id));
			if (!all.on()) break;
		}
		return out.length() == 0 ? "Default" : out.toString();
	}
}
