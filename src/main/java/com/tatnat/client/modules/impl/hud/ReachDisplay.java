package com.tatnat.client.modules.impl.hud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.util.WorldProjector;

/**
 * How far away you hit from, measured from your eyes to the exact point the hit ray touched the
 * target's hitbox. Shows the last value on the HUD and, optionally, as text that floats up from
 * the target and fades out (projected onto the screen, so it works on every version).
 */
public class ReachDisplay extends TextHudModule {
	private final BooleanSetting floating = add(new BooleanSetting("Floating Text", "Show the distance above the entity you hit", true));
	private final ColorSetting floatColor = add(new ColorSetting("Floating Color", "Colour of the floating text", 0xFFFFD54F, true));
	private final SliderSetting fade = add(new SliderSetting("Fade Time", "How long the floating text stays", 1000, 300, 3000, 100, "ms"));

	private static final class Pop {
		final double x, y, z;
		final String text;
		final long time;

		Pop(double[] pos, String text, long time) {
			this.x = pos[0];
			this.y = pos[1];
			this.z = pos[2];
			this.text = text;
			this.time = time;
		}
	}

	private final List<Pop> pops = new ArrayList<>();
	private double last = -1;
	private long lastTime;

	public ReachDisplay() {
		super("Reach Display", "Shows how far away you hit from", false, 0.0, 0.45);
		icon = Icons.Icon.RULER;
	}

	@Subscribe
	public void onAttack(Events.Attack e) {
		double d = game().reachTo(e.target);
		if (d < 0) return;
		last = d;
		lastTime = System.currentTimeMillis();
		if (floating.on()) {
			pops.add(new Pop(game().aboveHead(e.target), String.format(Locale.ROOT, "%.2f", d), lastTime));
			if (pops.size() > 12) pops.remove(0);
		}
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (pops.isEmpty() || game().hudHidden()) return;
		Gfx g = e.gfx;
		long now = System.currentTimeMillis();
		for (Iterator<Pop> it = pops.iterator(); it.hasNext();) {
			Pop p = it.next();
			float t = (now - p.time) / fade.floatValue();
			if (t >= 1f) {
				it.remove();
				continue;
			}
			double[] s = WorldProjector.project(p.x, p.y + t * 0.6, p.z);
			if (s[2] != 1) continue;
			int color = Colors.fade(floatColor.color(), t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f);
			// Closer hits get slightly bigger text, like a real floating label.
			float scale = (float) Math.max(0.7, Math.min(1.6, 4.0 / Math.max(1.0, s[3])));
			g.push();
			g.translate((float) s[0], (float) s[1]);
			g.scale(scale, scale);
			g.mcText(p.text, -g.mcTextWidth(p.text, true) / 2, -4, color, true, true);
			g.pop();
		}
	}

	@Override
	protected String label() {
		return "Reach";
	}

	@Override
	protected String value(boolean preview) {
		if (last < 0 || System.currentTimeMillis() - lastTime > 4000) return preview ? "3.00" : "-";
		return String.format(Locale.ROOT, "%.2f", last);
	}
}
