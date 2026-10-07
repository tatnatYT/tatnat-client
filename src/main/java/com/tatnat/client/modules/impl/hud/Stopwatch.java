package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.util.KeyCodes;
import com.tatnat.client.util.Keys;

/** An on-screen stopwatch: {@code 00:12:34}, started and stopped with a key (or the buttons in its settings). */
public class Stopwatch extends TextHudModule {
	private final KeybindSetting startStop = add(new KeybindSetting("Start/Stop Key", "Starts or pauses the stopwatch", Keys.NONE));
	private final KeybindSetting reset = add(new KeybindSetting("Reset Key", "Sets the stopwatch back to zero", Keys.NONE));
	private final BooleanSetting tenths = add(new BooleanSetting("Show Tenths", "Show tenths of a second", false));

	private long elapsed, startedAt;
	private boolean running;

	public Stopwatch() {
		super("Stopwatch", "A stopwatch on your screen", false, 0.0, 0.33);
		icon = com.tatnat.client.ui.render.Icons.Icon.CLOCK;
		add(new ActionSetting("Start / Stop", "Start or pause it now", () -> running ? "Pause" : "Start", this::startStop));
		add(new ActionSetting("Reset", "Back to 00:00:00", () -> "Reset", this::resetTimer));
	}

	private long now() {
		return elapsed + (running ? System.currentTimeMillis() - startedAt : 0);
	}

	private void startStop() {
		if (running) elapsed = now();
		else startedAt = System.currentTimeMillis();
		running = !running;
	}

	private void resetTimer() {
		elapsed = 0;
		startedAt = System.currentTimeMillis();
	}

	@Subscribe
	public void onKey(Events.Key e) {
		if (!e.inGame || e.action != KeyCodes.PRESS) return;
		if (startStop.isBound() && e.key == startStop.get()) startStop();
		else if (reset.isBound() && e.key == reset.get()) resetTimer();
	}

	@Override
	protected String label() {
		return "";
	}

	@Override
	protected String value(boolean preview) {
		long ms = preview ? 754_300 : now();
		long s = ms / 1000;
		String out = String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
		return tenths.on() ? out + "." + ms / 100 % 10 : out;
	}
}
