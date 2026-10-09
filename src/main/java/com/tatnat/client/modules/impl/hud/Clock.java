package com.tatnat.client.modules.impl.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ModeSetting;

/** Your computer's local time. */
public class Clock extends TextHudModule {
	private final ModeSetting format = add(new ModeSetting("Format", "12 or 24 hour clock", "24h", "24h", "12h"));
	private final BooleanSetting seconds = add(new BooleanSetting("Show Seconds", "Include seconds", false));

	private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	private static final DateTimeFormatter H24S = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
	private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
	private static final DateTimeFormatter H12S = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH);

	public Clock() {
		super("Clock", "Shows the current time", false, 0.75, 0.0);
		icon = com.tatnat.client.ui.render.Icons.Icon.CLOCK;
	}

	@Override
	protected String label() {
		return "";
	}

	@Override
	protected String value(boolean preview) {
		DateTimeFormatter f = format.is("12h") ? (seconds.on() ? H12S : H12) : (seconds.on() ? H24S : H24);
		return LocalTime.now().format(f);
	}
}
