package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.util.WorldProjector;

/**
 * Light Level Overlay: tints the top of every block around you (a circle, 16 blocks by default)
 * where a mob could spawn: a solid block with room above and no block light (7 or less on 1.17
 * and older). Light those spots up with torches and the tint goes away.
 */
public class LightLevelOverlay extends Module {
	private final SliderSetting radius = add(new SliderSetting("Radius", "How far around you to check", 16, 4, 24, 1, " blocks"));
	private final ColorSetting color = add(new ColorSetting("Color", "Colour of the spawnable blocks", 0x66FF2A2A, false));
	private final BooleanSetting nightOnly = add(new BooleanSetting("Night Only", "Only while it's dark (mobs don't spawn in daylight)", false));
	private final BooleanSetting numbers = add(new BooleanSetting("Show Light Level", "Write the block light on each marked block", false));

	/** Cached scan, refreshed twice a second: {x, y, z, light} per spawnable block. */
	private int[] spots = new int[0];
	private int count;
	private long scannedAt, seenAt;
	/** Per spot: can you see it from where you stand? Refreshed ten times a second. */
	private boolean[] visible = new boolean[0];

	public LightLevelOverlay() {
		super("Light Level Overlay", "Highlights the blocks where mobs can spawn", Category.VISUAL, false);
		icon = Icons.Icon.BULB;
	}

	private int spawnLimit() {
		String[] p = game().minecraftVersion().split("\\.");
		try {
			// 1.18 changed the rule to "block light 0".
			return p[0].equals("1") && Integer.parseInt(p[1]) <= 17 ? 7 : 0;
		} catch (RuntimeException e) {
			return 0;
		}
	}

	private void scan() {
		int r = radius.intValue(), limit = spawnLimit();
		int px = (int) Math.floor(game().x()), py = (int) Math.floor(game().y()), pz = (int) Math.floor(game().z());
		int[] out = new int[(2 * r + 1) * (2 * r + 1) * 4];
		int n = 0;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r) continue; // a circle, not a square
				for (int y = py + 3; y >= py - 4; y--) {
					int x = px + dx, z = pz + dz;
					if (!game().spawnSurface(x, y, z)) continue;
					int light = game().blockLight(x, y, z);
					if (light <= limit) {
						out[n++] = x;
						out[n++] = y;
						out[n++] = z;
						out[n++] = light;
					}
					break; // only the top surface of each column
				}
			}
		}
		spots = out;
		count = n;
		seenAt = 0;
	}

	private void lineOfSight() {
		if (visible.length < count / 4) visible = new boolean[count / 4];
		double ex = game().x(), ey = game().eyeY(), ez = game().z();
		for (int i = 0; i < count; i += 4) visible[i / 4] = clear(ex, ey, ez, spots[i] + 0.5, spots[i + 1] + 0.1, spots[i + 2] + 0.5);
	}

	/** Walks the blocks between the eye and a point (a voxel ray); false when a solid block is in the way. */
	private boolean clear(double x0, double y0, double z0, double x1, double y1, double z1) {
		int x = (int) Math.floor(x0), y = (int) Math.floor(y0), z = (int) Math.floor(z0);
		int tx = (int) Math.floor(x1), ty = (int) Math.floor(y1), tz = (int) Math.floor(z1);
		double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
		int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
		double ix = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx), iy = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy), iz = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
		double nx = dx == 0 ? Double.MAX_VALUE : (sx > 0 ? x + 1 - x0 : x0 - x) * ix;
		double ny = dy == 0 ? Double.MAX_VALUE : (sy > 0 ? y + 1 - y0 : y0 - y) * iy;
		double nz = dz == 0 ? Double.MAX_VALUE : (sz > 0 ? z + 1 - z0 : z0 - z) * iz;
		while (Math.min(nx, Math.min(ny, nz)) < 1) {
			if (nx < ny && nx < nz) {
				x += sx;
				nx += ix;
			} else if (ny < nz) {
				y += sy;
				ny += iy;
			} else {
				z += sz;
				nz += iz;
			}
			if (x == tx && y == ty && z == tz) return true;
			if (game().opaque(x, y, z)) return false;
		}
		return true;
	}

	private boolean dark() {
		if (!nightOnly.on()) return true;
		int d = game().skyDarkness();
		return d < 0 || d >= 4; // unknown on this version: always show
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden() || !dark()) return;
		long now = System.currentTimeMillis();
		if (now - scannedAt > 500) {
			scannedAt = now;
			scan();
		}
		if (now - seenAt > 100) {
			seenAt = now;
			lineOfSight();
		}
		Gfx g = e.gfx;
		int scale = RenderUtils.beginPixels(g);
		int fill = color.color(0), edge = Colors.withAlpha(fill, Math.min(255, (fill >>> 24) + 90));
		float[] quad = new float[8];
		for (int i = 0; i < count; i += 4) {
			if (!visible[i / 4]) continue;
			double x = spots[i], y = spots[i + 1] + 0.02, z = spots[i + 2];
			if (!corner(quad, 0, x, y, z, scale) || !corner(quad, 2, x + 1, y, z, scale) || !corner(quad, 4, x + 1, y, z + 1, scale)
					|| !corner(quad, 6, x, y, z + 1, scale)) continue;
			Icons.polygon(g, quad, fill);
			for (int c = 0; c < 4; c++) {
				int d = (c + 1) % 4;
				Icons.thickLine(g, quad[c * 2], quad[c * 2 + 1], quad[d * 2], quad[d * 2 + 1], Math.max(1f, scale * 0.6f), edge);
			}
		}
		RenderUtils.end(g);
		if (numbers.on()) {
			for (int i = 0; i < count; i += 4) {
			if (!visible[i / 4]) continue;
				double[] p = WorldProjector.project(spots[i] + 0.5, spots[i + 1] + 0.05, spots[i + 2] + 0.5);
				if (p[2] == 1 && p[3] < 12) g.mcText(String.valueOf(spots[i + 3]), (int) p[0] - 2, (int) p[1] - 4, 0xFFFF5555, true, false);
			}
		}
	}

	/** Projects one corner into real pixels; false when it's behind the camera (skip the face). */
	private static boolean corner(float[] out, int at, double x, double y, double z, int scale) {
		double[] p = WorldProjector.project(x, y, z);
		if (p[3] <= 0.05) return false;
		out[at] = (float) p[0] * scale;
		out[at + 1] = (float) p[1] * scale;
		return true;
	}
}
