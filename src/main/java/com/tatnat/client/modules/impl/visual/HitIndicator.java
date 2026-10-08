package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.Gfx;

/** Flashes the edges of the screen when you take damage. */
public class HitIndicator extends Module {
	private final ColorSetting color = add(new ColorSetting("Color", "Colour of the flash", 0xFFFF2020, false));
	private final SliderSetting opacity = add(new SliderSetting("Opacity", "How strong the flash is", 60, 10, 100, 5, "%"));
	private final SliderSetting length = add(new SliderSetting("Duration", "How long it lasts", 400, 100, 1500, 50, " ms"));

	private float lastHealth = -1;
	private long hitAt;

	public HitIndicator() {
		super("Hit Indicator", "Flashes the screen edges when you're hurt", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.CROSSHAIR;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) {
			lastHealth = -1;
			return;
		}
		float h = game().health();
		if (lastHealth >= 0 && h < lastHealth && h > 0) hitAt = System.currentTimeMillis();
		lastHealth = h;
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		long age = System.currentTimeMillis() - hitAt;
		if (age > length.get()) return;
		float k = 1f - age / (float) length.get().doubleValue();
		int maxA = (int) (255 * opacity.get() / 100.0 * k);
		Gfx g = e.gfx;
		int w = game().guiWidth(), h = game().guiHeight();
		int band = Math.max(8, Math.min(w, h) / 8);
		int rgb = color.color(0) & 0xFFFFFF;
		// A soft vignette: a few steps of fading bands on every edge.
		int steps = 8;
		for (int i = 0; i < steps; i++) {
			int a = maxA * (steps - i) / steps / 3;
			if (a <= 0) continue;
			int c = (a << 24) | rgb;
			int t = band * i / steps, t2 = band * (i + 1) / steps;
			g.rect(0, t, w, t2, c);
			g.rect(0, h - t2, w, h - t, c);
			g.rect(t, t2, t2, h - t2, c);
			g.rect(w - t2, t2, w - t, h - t2, c);
		}
	}
}
