package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.platform.Gfx;
import com.tatnat.client.util.WorldProjector;

/** Small helpers for text floating over things in the world (projected onto the HUD, like waypoints). */
final class WorldLabels {
	private WorldLabels() {
	}

	/** Screen position of a world point, or null when it's off screen. */
	static double[] onScreen(double x, double y, double z) {
		double[] p = WorldProjector.project(x, y, z);
		return p[2] == 1 ? p : null;
	}

	/** Centred text on a dark box, shrinking a little with distance. */
	static void text(Gfx g, double[] p, String text, int color, float scale) {
		float s = (float) (scale * Math.max(0.6, Math.min(1.0, 8.0 / Math.max(1, p[3]))));
		int tw = g.mcTextWidth(text, false);
		g.push();
		g.translate((float) p[0], (float) p[1]);
		g.scale(s, s);
		g.rect(-tw / 2 - 3, -6, tw / 2 + 3, 5, 0x90000000);
		g.mcText(text, -tw / 2, -4, color, false, false);
		g.pop();
	}

	/** Seconds as {@code 4m 59s} or {@code 3.2s}. */
	static String time(double seconds) {
		if (seconds < 10) return String.format(java.util.Locale.ROOT, "%.1fs", Math.max(0, seconds));
		long s = (long) seconds;
		return s >= 60 ? s / 60 + "m " + s % 60 + "s" : s + "s";
	}
}
