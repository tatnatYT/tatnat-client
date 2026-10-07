package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ModeSetting;

/** {@code Facing: North (330°)}: the way you're looking, as a compass bearing (north = 0°, east = 90°). */
public class Direction extends TextHudModule {
	private static final String[] LONG = {"North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West"};
	private static final String[] SHORT = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private final BooleanSetting degrees = add(new BooleanSetting("Show Degrees", "Add the exact bearing, like (330°)", true));
	private final ModeSetting names = add(new ModeSetting("Names", "Full names or compass letters", "Full", "Full", "Short"));

	public Direction() {
		super("Direction", "Shows which way you're facing", false, 0.0, 0.27);
		icon = com.tatnat.client.ui.render.Icons.Icon.GLOBE;
	}

	@Override
	protected String label() {
		return "Facing";
	}

	@Override
	protected String value(boolean preview) {
		// Minecraft's yaw: 0 = south, 90 = west, 180 = north. A bearing puts north at 0 and east at 90.
		float yaw = preview ? 150f : game().yaw();
		double bearing = ((yaw + 180.0) % 360.0 + 360.0) % 360.0;
		int i = (int) Math.round(bearing / 45.0) % 8;
		String name = names.is("Short") ? SHORT[i] : LONG[i];
		return degrees.on() ? name + " (" + Math.round(bearing) % 360 + "°)" : name;
	}
}
