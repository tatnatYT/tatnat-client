package com.tatnat.client.perf;

import java.util.Map;
import java.util.WeakHashMap;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.CameraInfo;
import com.tatnat.client.platform.Game;

/**
 * Eclipse Client's built-in FPS optimizations. They are always on (there is no switch) and only
 * skip work you can't see, so the picture stays exactly the same:
 * <ul>
 * <li><b>Entity Culling</b>: mobs, items, armor stands and other entities completely hidden behind
 * solid blocks aren't drawn. Players and glowing entities are always drawn (name tags and outlines
 * show through walls).</li>
 * <li><b>Block Entity Culling</b>: chests, signs, banners, beds, heads and other block entities
 * completely hidden behind solid blocks aren't drawn.</li>
 * </ul>
 * Each check casts rays from the camera to the middle and the eight corners of the thing; it is
 * hidden only when every ray hits a block that fully hides what's behind it (glass, leaves, fences
 * and slabs never hide anything). Results are kept for a few frames and the checks share a small time
 * budget per frame, so the culling itself never costs more than it saves. Both step aside when the
 * EntityCulling mod is installed (it does the same job).
 */
public final class Optimizations {
	private Optimizations() {
	}

	/** A hidden thing is looked at again this often, so it pops back in quickly when it comes into view. */
	private static final long RECHECK_HIDDEN = 40_000_000L;
	/** A visible thing is looked at again this often. */
	private static final long RECHECK_VISIBLE = 150_000_000L;
	/** Time all the checks together may take per ~frame (16 ms window). */
	private static final long BUDGET = 1_500_000L, WINDOW = 16_000_000L;
	/** Things closer than this are always drawn (no point checking, and it avoids edge cases). */
	private static final double NEAR = 3.0;
	/** Things further than this are left to vanilla (long rays cost more than they save). */
	private static final double FAR = 160.0;

	private static Boolean enabled;
	private static final Map<Object, long[]> entities = new WeakHashMap<>();
	private static final Map<Object, long[]> blockEntities = new WeakHashMap<>();
	private static long windowStart, spent;
	private static long cameraAt;
	private static double camX, camY, camZ;
	private static boolean cameraInBlock;
	/** Dev check only: switches the culling off to compare. */
	public static boolean devOff;
	/** Dev check only: how many draws were skipped and how long the checks took, since the last {@link #takeStats}. */
	private static long statEntities, statBlockEntities, statNanos;

	/** Off when the EntityCulling mod is installed (it does the same job). */
	public static boolean enabled() {
		if (devOff) return false;
		if (enabled == null) {
			Game g = TatnatClient.game();
			enabled = g != null && !g.modLoaded("entityculling");
		}
		return enabled;
	}

	/** Shown on the Performance page: what's built in and whether it is running. */
	public static String[][] list() {
		String state = enabled() ? "Always on" : "Off: EntityCulling is installed";
		return new String[][] {
				{"Entity Culling", "Mobs, items and other entities hidden behind blocks aren't drawn", state},
				{"Block Entity Culling", "Chests, signs, banners and other block entities hidden behind blocks aren't drawn", state},
		};
	}

	/**
	 * Entity hook (called where the game decides whether to draw an entity). {@code exempt} = never
	 * hide it (players, glowing entities, the camera entity). The box is the entity's bounding box.
	 */
	public static boolean entityHidden(Object entity, boolean exempt, double cx, double cy, double cz,
			double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		if (exempt || entity == null || !enabled()) return false;
		try {
			camera(cx, cy, cz);
			// Room above for a name tag, a little around for shadows and animations.
			boolean h = hidden(entities, entity, minX - 0.2, minY - 0.1, minZ - 0.2, maxX + 0.2, maxY + 0.6, maxZ + 0.2);
			if (h) statEntities++;
			return h;
		} catch (RuntimeException e) {
			return false;
		}
	}

	/** Block entity hook: the block entity at x, y, z. */
	public static boolean blockEntityHidden(Object blockEntity, int x, int y, int z) {
		if (blockEntity == null || !enabled()) return false;
		try {
			long now = System.nanoTime();
			if (now - cameraAt > 8_000_000L) {
				CameraInfo c = TatnatClient.game().camera();
				if (c == null) return false;
				camera(c.x, c.y, c.z);
			}
			// Some block entities draw outside their block (banners, beds, tall signs): one block around.
			boolean h = hidden(blockEntities, blockEntity, x - 1, y - 1, z - 1, x + 2, y + 2, z + 2);
			if (h) statBlockEntities++;
			return h;
		} catch (RuntimeException e) {
			return false;
		}
	}

	/** Dev check: {skipped entity draws, skipped block entity draws, microseconds spent checking} since the last call. */
	public static long[] takeStats() {
		long[] r = {statEntities, statBlockEntities, statNanos / 1000};
		statEntities = statBlockEntities = statNanos = 0;
		return r;
	}

	private static void camera(double x, double y, double z) {
		long now = System.nanoTime();
		if (x != camX || y != camY || z != camZ || now - cameraAt > 250_000_000L) {
			camX = x;
			camY = y;
			camZ = z;
			// Inside a block (spectator, freecam through walls): everything would look hidden.
			cameraInBlock = TatnatClient.game().occludes(floor(x), floor(y), floor(z));
		}
		cameraAt = now;
	}

	private static boolean hidden(Map<Object, long[]> cache, Object key, double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ) {
		if (cameraInBlock) return false;
		double nx = clamp(camX, minX, maxX) - camX, ny = clamp(camY, minY, maxY) - camY, nz = clamp(camZ, minZ, maxZ) - camZ;
		double dist2 = nx * nx + ny * ny + nz * nz;
		if (dist2 < NEAR * NEAR || dist2 > FAR * FAR) return false;

		long now = System.nanoTime();
		long[] s = cache.get(key);
		if (s != null && now - s[0] < (s[1] == 1 ? RECHECK_HIDDEN : RECHECK_VISIBLE)) return s[1] == 1;
		// Out of time this frame: keep the last answer (new things are drawn).
		if (now - windowStart > WINDOW) {
			windowStart = now;
			spent = 0;
		}
		if (spent > BUDGET) return s != null && s[1] == 1;

		boolean hidden = occluded(minX, minY, minZ, maxX, maxY, maxZ);
		long took = System.nanoTime() - now;
		spent += took;
		statNanos += took;
		if (s == null) cache.put(key, s = new long[2]);
		s[0] = now;
		s[1] = hidden ? 1 : 0;
		return hidden;
	}

	/** True when every ray from the camera to the box's middle and corners is blocked. */
	private static boolean occluded(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		if (clear((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2)) return false;
		for (int i = 0; i < 8; i++) {
			if (clear((i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ)) return false;
		}
		return true;
	}

	/**
	 * Walks the blocks on the line from the camera to a point (Amanatides-Woo): false as soon as one
	 * of them hides what's behind it. The camera's block and the point's block don't count.
	 */
	private static boolean clear(double tx, double ty, double tz) {
		Game g = TatnatClient.game();
		double dx = tx - camX, dy = ty - camY, dz = tz - camZ;
		int x = floor(camX), y = floor(camY), z = floor(camZ);
		int ex = floor(tx), ey = floor(ty), ez = floor(tz);
		int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
		double tdx = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx);
		double tdy = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy);
		double tdz = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
		double tmx = dx == 0 ? Double.MAX_VALUE : ((dx > 0 ? x + 1 - camX : camX - x) * tdx);
		double tmy = dy == 0 ? Double.MAX_VALUE : ((dy > 0 ? y + 1 - camY : camY - y) * tdy);
		double tmz = dz == 0 ? Double.MAX_VALUE : ((dz > 0 ? z + 1 - camZ : camZ - z) * tdz);
		for (int steps = 0; steps < 512; steps++) {
			double t;
			if (tmx < tmy && tmx < tmz) {
				t = tmx;
				x += sx;
				tmx += tdx;
			} else if (tmy < tmz) {
				t = tmy;
				y += sy;
				tmy += tdy;
			} else {
				t = tmz;
				z += sz;
				tmz += tdz;
			}
			if (t > 1.0 || (x == ex && y == ey && z == ez)) return true; // reached the point
			if (g.occludes(x, y, z)) return false;
		}
		return true;
	}

	private static int floor(double v) {
		int i = (int) v;
		return v < i ? i - 1 : i;
	}

	private static double clamp(double v, double lo, double hi) {
		return v < lo ? lo : v > hi ? hi : v;
	}
}
