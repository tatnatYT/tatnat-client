package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.platform.Gfx;

/** A live graph of your mouse movement (how far you turned each tick, left/right and up/down). */
public class Mousestrokes extends HudModule {
	private static final int N = 60, W = 90, H = 40;

	private final ColorSetting color = add(new ColorSetting("Color", "Line colour", 0xFF4EB1FF, true));
	private final BooleanSetting vertical = add(new BooleanSetting("Show Up/Down", "Also graph looking up and down", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the graph", true));

	private final float[] dx = new float[N], dy = new float[N];
	private int head;
	private float lastYaw = Float.NaN, lastPitch;

	public Mousestrokes() {
		super("Mousestrokes", "Graphs your mouse movement", false, 0.8, 0.8);
		icon = com.tatnat.client.ui.render.Icons.Icon.WAVE;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) return;
		float yaw = game().yaw(), pitch = game().pitch();
		if (!Float.isNaN(lastYaw)) {
			float d = yaw - lastYaw;
			while (d > 180) d -= 360;
			while (d < -180) d += 360;
			head = (head + 1) % N;
			dx[head] = d;
			dy[head] = pitch - lastPitch;
		}
		lastYaw = yaw;
		lastPitch = pitch;
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		if (background.on()) g.rect(0, 0, W, H, 0x6F000000);
		g.rect(0, H / 2, W, H / 2 + 1, 0x30FFFFFF);
		plot(g, dx, color.color(0), preview, 0);
		if (vertical.on()) plot(g, dy, 0xFFFF8855, preview, 1);
		return size(W, H);
	}

	/** One bar per tick, scaled so a fast 30°/tick flick fills the half-height. */
	private void plot(Gfx g, float[] data, int c, boolean preview, int seed) {
		for (int i = 0; i < N; i++) {
			float v = preview ? (float) (Math.sin(i * 0.3 + seed) * 12) : data[(head + 1 + i) % N];
			int px = i * W / N;
			int len = (int) Math.max(-H / 2, Math.min(H / 2, v / 30f * (H / 2)));
			if (len == 0) continue;
			int y0 = H / 2, y1 = H / 2 - len;
			g.rect(px, Math.min(y0, y1), px + Math.max(1, W / N), Math.max(y0, y1), c);
		}
	}
}
