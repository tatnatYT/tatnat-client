package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

/** UHC at a glance: {@code Gapples: 3 | Heads: 1 | HP: 18.5}. Golden heads are counted by name. */
public class UhcOverlay extends TextHudModule {
	private final BooleanSetting heads = add(new BooleanSetting("Count Golden Heads", "Also count golden heads", true));
	private final BooleanSetting health = add(new BooleanSetting("Show Health", "Add your health", true));

	public UhcOverlay() {
		super("UHC Overlay", "Golden apples, heads and health for UHC", false, 0.75, 0.30);
		icon = com.tatnat.client.ui.render.Icons.Icon.APPLE;
	}

	@Override
	protected String label() {
		return "Gapples";
	}

	@Override
	protected String value(boolean preview) {
		int apples = preview ? 3 : ItemCounter.count("golden_apple, golden apple") - (heads.on() ? ItemCounter.count("golden head") : 0);
		StringBuilder s = new StringBuilder().append(Math.max(0, apples));
		if (heads.on()) s.append(" | Heads: ").append(preview ? 1 : ItemCounter.count("golden head"));
		if (health.on()) s.append(" | HP: ").append(String.format(java.util.Locale.ROOT, "%.1f", preview ? 18.5f : game().health()));
		return s.toString();
	}
}
