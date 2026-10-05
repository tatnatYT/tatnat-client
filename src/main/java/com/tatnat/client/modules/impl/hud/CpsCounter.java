package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.util.CpsTracker;

/** {@code [CPS: 8 | 3]} -- left and/or right clicks per second. */
public class CpsCounter extends TextHudModule {
	private final BooleanSetting left = add(new BooleanSetting("Left Click", "Count left clicks", true));
	private final BooleanSetting right = add(new BooleanSetting("Right Click", "Count right clicks", false));
	private final BooleanSetting colorCode = add(new BooleanSetting("Color Code", "Green under 10, yellow 10-14, red 15+", false));

	public CpsCounter() {
		super("CPS Counter", "Shows your clicks per second", true, 0.0, 0.075);
		icon = com.tatnat.client.ui.render.Icons.Icon.MOUSE;
	}

	@Override
	protected String label() {
		return "CPS";
	}

	@Override
	protected String value(boolean preview) {
		int l = CpsTracker.INSTANCE.cps(0), r = CpsTracker.INSTANCE.cps(1);
		if (left.on() && right.on()) return l + " | " + r;
		if (right.on()) return String.valueOf(r);
		return String.valueOf(l);
	}

	@Override
	protected int valueColor(boolean preview) {
		if (!colorCode.on()) return 0;
		int c = Math.max(left.on() ? CpsTracker.INSTANCE.cps(0) : 0, right.on() ? CpsTracker.INSTANCE.cps(1) : 0);
		return c < 10 ? 0xFF55FF55 : c < 15 ? 0xFFFFFF55 : 0xFFFF5555;
	}
}
