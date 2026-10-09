package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

/** {@code Totems: x3}: Totems of Undying in your inventory (offhand included). Red when you have none. */
public class TotemCounter extends TextHudModule {
	private final BooleanSetting hideWhenNone = add(new BooleanSetting("Hide When None", "Only show it while you carry totems", false));

	public TotemCounter() {
		super("Totem Counter", "Shows how many Totems of Undying you have", false, 0.75, 0.075);
		icon = com.tatnat.client.ui.render.Icons.Icon.TOTEM;
	}

	private int totems(boolean preview) {
		return preview ? 3 : ItemCounter.count("totem_of_undying, totem of undying");
	}

	@Override
	protected String label() {
		return "Totems";
	}

	@Override
	protected String value(boolean preview) {
		return "x" + totems(preview);
	}

	@Override
	protected int valueColor(boolean preview) {
		return totems(preview) == 0 ? 0xFFFF5555 : 0;
	}

	@Override
	protected long draw(com.tatnat.client.platform.Gfx g, boolean preview) {
		if (!preview && hideWhenNone.on() && totems(false) == 0) return size(0, 0);
		return super.draw(g, preview);
	}
}
