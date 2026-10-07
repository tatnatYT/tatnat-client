package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

/** {@code Speed: 12.4 b/s}, measured from how far you moved each tick (smoothed over half a second). */
public class SpeedMeter extends TextHudModule {
	private final BooleanSetting horizontal = add(new BooleanSetting("Horizontal Only", "Ignore falling and jumping", true));
	private final BooleanSetting thresholds = add(new BooleanSetting("Color Thresholds", "Green when sprinting speed or faster, yellow when walking", true));

	private double lastX, lastY, lastZ, speed;
	private boolean has;

	public SpeedMeter() {
		super("Speed Meter", "Shows how fast you're moving in blocks per second", false, 0.0, 0.30);
		icon = com.tatnat.client.ui.render.Icons.Icon.RUN;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) {
			has = false;
			speed = 0;
			return;
		}
		double x = game().x(), y = game().y(), z = game().z();
		if (has) {
			double dx = x - lastX, dy = horizontal.on() ? 0 : y - lastY, dz = z - lastZ;
			double now = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
			if (now > 200) now = speed; // a teleport, not movement
			speed += (now - speed) * 0.2;
		}
		lastX = x;
		lastY = y;
		lastZ = z;
		has = true;
	}

	@Override
	protected String label() {
		return "Speed";
	}

	@Override
	protected String value(boolean preview) {
		double v = preview ? 5.6 : speed;
		return String.format(java.util.Locale.ROOT, "%.1f b/s", v < 0.05 ? 0 : v);
	}

	@Override
	protected int valueColor(boolean preview) {
		if (!thresholds.on()) return 0;
		double v = preview ? 5.6 : speed;
		// Walking is about 4.3 b/s, sprinting about 5.6.
		return v >= 5.5 ? 0xFF55FF55 : v >= 4 ? 0xFFFFFF55 : 0;
	}
}
