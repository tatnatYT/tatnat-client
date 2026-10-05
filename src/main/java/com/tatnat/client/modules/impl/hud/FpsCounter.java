package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

/** {@code [FPS: 240]}, green above 60, yellow 30-60, red below 30. */
public class FpsCounter extends TextHudModule {
	private final BooleanSetting colorCode = add(new BooleanSetting("Color Code", "Green above 60, yellow 30-60, red below 30", true));

	public FpsCounter() {
		super("FPS Counter", "Shows your frames per second", true, 0.0, 0.0);
		icon = com.tatnat.client.ui.render.Icons.Icon.MONITOR;
	}

	@Override
	protected String label() {
		return "FPS";
	}

	@Override
	protected String value(boolean preview) {
		return String.valueOf(game().fps());
	}

	@Override
	protected int valueColor(boolean preview) {
		if (!colorCode.on()) return 0;
		int fps = game().fps();
		return fps > 60 ? 0xFF55FF55 : fps >= 30 ? 0xFFFFFF55 : 0xFFFF5555;
	}
}
