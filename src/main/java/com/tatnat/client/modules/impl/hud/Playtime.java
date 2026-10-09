package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.ModeSetting;

/** {@code Played: 2h 14m}: how long the game has been open this session. */
public class Playtime extends TextHudModule {
	/** When the game (this mod) started. */
	private static final long START = System.currentTimeMillis();

	private final ModeSetting format = add(new ModeSetting("Format", "Hours and minutes, or a clock", "2h 14m", "2h 14m", "02:14:05", "134 min"));

	public Playtime() {
		super("Playtime", "Shows how long you've been playing this session", false, 0.38, 0.15);
		icon = com.tatnat.client.ui.render.Icons.Icon.HOURGLASS;
	}

	@Override
	protected String label() {
		return "Played";
	}

	@Override
	protected String value(boolean preview) {
		long s = preview ? 8045 : (System.currentTimeMillis() - START) / 1000;
		if (format.is("02:14:05")) return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
		if (format.is("134 min")) return s / 60 + " min";
		return s >= 3600 ? s / 3600 + "h " + s / 60 % 60 + "m" : s / 60 + "m";
	}
}
