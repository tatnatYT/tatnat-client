package com.tatnat.client.ui.render;

import java.util.HashMap;
import java.util.Map;

import com.tatnat.client.ui.theme.Colors;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Gfx;

/**
 * 2D drawing primitives for the menus: anti-aliased rounded rectangles, soft shadows, gradients,
 * dashed outlines and circles.
 *
 * <h2>Pixel space</h2>
 * Minecraft's GUI coordinates are "scaled" pixels (one unit = guiScale real pixels), which makes a
 * 4px corner radius impossible at GUI scale 3. Menus therefore draw between
 * {@link #beginPixels} / {@link #end}, which undoes the GUI scale so one unit is one real pixel.
 *
 * <h2>Anti-aliased corners</h2>
 * Corners are drawn as horizontal spans: fully covered pixels are merged into one fill per row,
 * and the edge pixels get their alpha from a 4x4 super-sampled coverage mask. Masks are computed
 * once per radius and cached, so a rounded rect costs a few dozen tiny fills and no shaders --
 * it works on every GPU and alongside Sodium/Iris without touching the render pipeline.
 */
public final class RenderUtils {
	private RenderUtils() {
	}

	/** Global opacity multiplier for everything drawn through this class (menu fade-in). */
	public static float alpha = 1f;

	private static final int SUPERSAMPLE = 4;
	private static final Map<Integer, float[][]> CORNER_MASKS = new HashMap<>();

	/** Switches {@code g} to real-pixel coordinates. Returns the GUI scale that was undone. */
	public static int beginPixels(Gfx g) {
		int scale = guiScale();
		g.push();
		g.scale(1f / scale, 1f / scale);
		return scale;
	}

	public static void end(Gfx g) {
		g.pop();
	}

	public static int guiScale() {
		return TatnatClient.game().guiScale();
	}

	/** A plain rectangle from (x1, y1) to (x2, y2), faded by {@link #alpha}. */
	public static void rect(Gfx g, int x1, int y1, int x2, int y2, int color) {
		if (x2 <= x1 || y2 <= y1) return;
		int c = Colors.fade(color, alpha);
		if (Colors.alpha(c) == 0) return;
		g.rect(x1, y1, x2, y2, c);
	}

	/** Anti-aliased rounded rectangle; {@code radius} is clamped to half the smaller side. */
	public static void roundedRect(Gfx g, int x, int y, int w, int h, int radius, int color) {
		roundedRect(g, x, y, w, h, radius, color, true, true, true, true);
	}

	/** Rounded rectangle where each corner can be square (e.g. a tab glued to a panel edge). */
	public static void roundedRect(Gfx g, int x, int y, int w, int h, int radius, int color,
			boolean tl, boolean tr, boolean bl, boolean br) {
		if (w <= 0 || h <= 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		if (r == 0) {
			rect(g, x, y, x + w, y + h, color);
			return;
		}
		// Middle band (full width), then the top and bottom bands between the corners.
		rect(g, x, y + r, x + w, y + h - r, color);
		rect(g, x + (tl ? r : 0), y, x + w - (tr ? r : 0), y + r, color);
		rect(g, x + (bl ? r : 0), y + h - r, x + w - (br ? r : 0), y + h, color);
		if (!tl) rect(g, x, y, x + r, y + r, color);
		if (!tr) rect(g, x + w - r, y, x + w, y + r, color);
		if (!bl) rect(g, x, y + h - r, x + r, y + h, color);
		if (!br) rect(g, x + w - r, y + h - r, x + w, y + h, color);

		float[][] mask = mask(r);
		if (tl) corner(g, mask, r, x, y, false, false, color);
		if (tr) corner(g, mask, r, x + w - r, y, true, false, color);
		if (bl) corner(g, mask, r, x, y + h - r, false, true, color);
		if (br) corner(g, mask, r, x + w - r, y + h - r, true, true, color);
	}

	/** A 1-pixel-thick rounded outline of the given thickness, drawn as a ring. */
	public static void roundedOutline(Gfx g, int x, int y, int w, int h, int radius, int thickness, int color) {
		// Draw the ring by layering: outer shape in the colour, then punch nothing -- instead draw
		// four straight edges plus corner arcs from the coverage masks of two radii.
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		rect(g, x + r, y, x + w - r, y + thickness, color);
		rect(g, x + r, y + h - thickness, x + w - r, y + h, color);
		rect(g, x, y + r, x + thickness, y + h - r, color);
		rect(g, x + w - thickness, y + r, x + w, y + h - r, color);
		if (r == 0) return;
		float[][] outer = mask(r);
		int ir = Math.max(0, r - thickness);
		float[][] inner = ir > 0 ? mask(ir) : null;
		for (int corner = 0; corner < 4; corner++) {
			boolean flipX = corner == 1 || corner == 3, flipY = corner >= 2;
			int ox = flipX ? x + w - r : x, oy = flipY ? y + h - r : y;
			for (int j = 0; j < r; j++) {
				for (int i = 0; i < r; i++) {
					float cov = outer[j][i];
					// Subtract the inner arc's coverage (its mask sits in the inner corner of this one).
					int ii = i - thickness, jj = j - thickness;
					if (inner != null && ii >= 0 && jj >= 0) cov -= inner[jj][ii];
					else if (inner == null && ii >= 0 && jj >= 0) cov = 0;
					if (cov <= 0.01f) continue;
					int px = flipX ? ox + (r - 1 - i) : ox + i;
					int py = flipY ? oy + (r - 1 - j) : oy + j;
					rect(g, px, py, px + 1, py + 1, Colors.fade(color, cov));
				}
			}
		}
	}

	/**
	 * Soft drop shadow: {@code layers} rounded rects, each one pixel bigger and fainter, offset
	 * slightly downwards like light from above. Draw it before the panel it belongs to.
	 */
	public static void shadow(Gfx g, int x, int y, int w, int h, int radius, int layers, float strength) {
		for (int i = layers; i >= 1; i--) {
			float t = 1f - (i - 1) / (float) layers;
			int a = Math.round(255 * strength * t * t / layers * 2.2f);
			int grow = i * 2;
			roundedRect(g, x - grow, y - grow + i / 2 + 2, w + grow * 2, h + grow * 2, radius + grow, Colors.argb(a, 0, 0, 0));
		}
	}

	/** Top-to-bottom gradient. */
	public static void verticalGradient(Gfx g, int x1, int y1, int x2, int y2, int top, int bottom) {
		if (x2 <= x1 || y2 <= y1) return;
		g.gradient(x1, y1, x2, y2, Colors.fade(top, alpha), Colors.fade(bottom, alpha));
	}

	/** Left-to-right gradient, drawn as 1px columns (only used for small things like slider fills). */
	public static void horizontalGradient(Gfx g, int x1, int y1, int x2, int y2, int left, int right) {
		int w = x2 - x1;
		for (int i = 0; i < w; i++) {
			rect(g, x1 + i, y1, x1 + i + 1, y2, Colors.lerp(left, right, w <= 1 ? 0 : i / (float) (w - 1)));
		}
	}

	/** Filled anti-aliased circle centred on (cx, cy). */
	public static void circle(Gfx g, int cx, int cy, int radius, int color) {
		roundedRect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
	}

	/** Rectangle outline made of dashes, {@code thickness} px wide (HUD editor bounding boxes). */
	public static void dashedRect(Gfx g, int x, int y, int w, int h, int dash, int gap, int thickness, int color) {
		// Offset the dash pattern over time so the boxes look "alive" ("marching ants").
		int phase = (int) (System.currentTimeMillis() / 60 % (dash + gap));
		for (int i = -phase; i < w; i += dash + gap) {
			int a = Math.max(0, i), b = Math.min(w, i + dash);
			if (b > a) {
				rect(g, x + a, y, x + b, y + thickness, color);
				rect(g, x + a, y + h - thickness, x + b, y + h, color);
			}
		}
		for (int i = -phase; i < h; i += dash + gap) {
			int a = Math.max(0, i), b = Math.min(h, i + dash);
			if (b > a) {
				rect(g, x, y + a, x + thickness, y + b, color);
				rect(g, x + w - thickness, y + a, x + w, y + b, color);
			}
		}
	}

	/** Draws one corner's arc spans. (ox, oy) is the corner square's top-left. */
	private static void corner(Gfx g, float[][] mask, int r, int ox, int oy, boolean flipX, boolean flipY, int color) {
		for (int j = 0; j < r; j++) {
			float[] row = mask[flipY ? r - 1 - j : j];
			// Coverage rises towards the inside of the corner; find where it becomes solid.
			int solidFrom = r;
			for (int i = 0; i < r; i++) {
				if (row[i] >= 0.999f) {
					solidFrom = i;
					break;
				}
			}
			int py = oy + j;
			if (solidFrom < r) {
				if (flipX) rect(g, ox, py, ox + (r - solidFrom), py + 1, color);
				else rect(g, ox + solidFrom, py, ox + r, py + 1, color);
			}
			for (int i = 0; i < solidFrom; i++) {
				float cov = row[i];
				if (cov <= 0.01f) continue;
				int px = flipX ? ox + (r - 1 - i) : ox + i;
				rect(g, px, py, px + 1, py + 1, Colors.fade(color, cov));
			}
		}
	}

	/**
	 * Coverage of each pixel in a top-left corner square of size r: mask[row][col], where (0,0) is
	 * the outermost pixel and the arc's centre sits at (r, r).
	 */
	private static float[][] mask(int r) {
		return CORNER_MASKS.computeIfAbsent(r, rad -> {
			float[][] m = new float[rad][rad];
			float r2 = rad * (float) rad;
			for (int j = 0; j < rad; j++) {
				for (int i = 0; i < rad; i++) {
					int inside = 0;
					for (int sy = 0; sy < SUPERSAMPLE; sy++) {
						for (int sx = 0; sx < SUPERSAMPLE; sx++) {
							float px = i + (sx + 0.5f) / SUPERSAMPLE, py = j + (sy + 0.5f) / SUPERSAMPLE;
							float dx = rad - px, dy = rad - py;
							if (dx * dx + dy * dy <= r2) inside++;
						}
					}
					m[j][i] = inside / (float) (SUPERSAMPLE * SUPERSAMPLE);
				}
			}
			return m;
		});
	}
}
