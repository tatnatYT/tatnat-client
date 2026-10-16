package com.tatnat.client.modules.impl.cosmetic;

import java.util.Random;

/**
 * The Eclipse Plus capes: animated scenes on a 1536x768 cape texture (the vanilla 64x32 layout at 24x),
 * so the outside face is 240x384 pixels. Scenes are written in "units" of an 80x128 face (3 pixels per
 * unit). Every frame comes from the wall clock, so all players see the same frame. Light is mixed as
 * floats and added together, then softly clamped. Each frame is split into bands of rows painted on
 * several cores at once, and buffers are reused between frames.
 */
public final class PlusCapes {
	public static final String[] IDS = {"eclipse", "corona", "nebula", "aurora", "galaxy", "void", "storm", "ender", "synthwave",
			"black", "purple", "blue", "green", "yellow", "white", "pink"};
	/** The single-colour capes and their colours. */
	private static final String[] SOLID = {"black", "purple", "blue", "green", "yellow", "white", "pink"};
	private static final int[] SOLID_RGB = {0xFF17161C, 0xFF7B3FE4, 0xFF2F6FE0, 0xFF2FB35A, 0xFFF2C230, 0xFFECECEC, 0xFFF06AB8};

	/** Whether the cape changes over time (the single colours don't, so they're drawn once). */
	public static boolean animated(String id) {
		return solidIndex(id) < 0;
	}

	private static int solidIndex(String id) {
		for (int i = 0; i < SOLID.length; i++) if (SOLID[i].equals(id)) return i;
		return -1;
	}
	/** Frames per second the capes are re-drawn at. */
	public static final int FPS = 30;
	public static final int W = 1536, H = 768;

	/** Texture pixels per 128x64 layout unit, and the faces in texture pixels (the vanilla cape layout: outside face at (2, 2), inside at (24, 2), edges either side, in 128x64 units). */
	private static final int K = W / 128, OX = 2 * K, OY = 2 * K, FW = 20 * K, FH = 32 * K, IX = 24 * K;
	/** The part of the texture that changes between frames (faces and edges); the rest never does. */
	public static final int DIRTY_Y0 = OY, DIRTY_Y1 = OY + FH, DIRTY_X1 = IX + FW;
	/** Pixels per scene unit. */
	private static final float S = FW / 80f;
	private static final float UW = 80, UH = 128;

	private PlusCapes() {
	}

	/** Paints bands of rows (or columns) on several cores at once. */
	private interface Band {
		void run(int from, int to);
	}

	private static final java.util.concurrent.ForkJoinPool POOL = new java.util.concurrent.ForkJoinPool(
			Math.max(2, Math.min(6, Runtime.getRuntime().availableProcessors() / 2)), p -> {
				java.util.concurrent.ForkJoinWorkerThread t = java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
				t.setName("Eclipse cape painter");
				t.setDaemon(true);
				t.setPriority(Thread.MIN_PRIORITY + 1);
				return t;
			}, null, false);

	private static void rows(int n, Band band) {
		int parts = POOL.getParallelism() * 2, step = (n + parts - 1) / parts;
		java.util.List<java.util.concurrent.Callable<Object>> jobs = new java.util.ArrayList<>();
		for (int from = 0; from < n; from += step) {
			final int a = from, b = Math.min(n, from + step);
			jobs.add(() -> {
				band.run(a, b);
				return null;
			});
		}
		try {
			for (java.util.concurrent.Future<Object> f : POOL.invokeAll(jobs)) f.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isPlus(String id) {
		for (String s : IDS) if (s.equals(id)) return true;
		return false;
	}

	/** One frame of light, per pixel of the outside face. */
	private static final class F {
		final float[] r = new float[FW * FH], g = new float[FW * FH], b = new float[FW * FH];

		void add(int px, int py, float ar, float ag, float ab) {
			if (px < 0 || py < 0 || px >= FW || py >= FH) return;
			int i = py * FW + px;
			r[i] += ar;
			g[i] += ag;
			b[i] += ab;
		}

		void set(int i, float sr, float sg, float sb) {
			r[i] = sr;
			g[i] = sg;
			b[i] = sb;
		}
	}

	/** The cape {@code id} at {@code timeMs}, as a new W x H ARGB array. */
	public static int[] draw(String id, long timeMs) {
		int[] img = new int[W * H];
		draw(id, timeMs, img, true);
		return img;
	}

	/** Light buffers per cape, reused frame to frame (a cape is never painted twice at once). */
	private static final java.util.Map<String, F> SCRATCH = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * Paints cape {@code id} at {@code timeMs} into {@code img} (W x H ARGB). With {@code fresh} the
	 * whole texture is written; otherwise only the part that changes ({@link #DIRTY_Y0} and so on).
	 */
	public static void draw(String id, long timeMs, int[] img, boolean fresh) {
		int solid = solidIndex(id);
		if (solid >= 0) {
			System.arraycopy(solid(SOLID_RGB[solid]), 0, img, 0, W * H);
			return;
		}
		float t = (timeMs % 3_600_000L) / 1000f;
		F f = SCRATCH.computeIfAbsent(id, k -> new F());
		int base, hemA, hemB;
		switch (id) {
			case "corona": corona(f, t); base = 0xFF1A0603; hemA = 0xFFFFB13B; hemB = 0xFFFF5A1F; break;
			case "nebula": nebula(f, t); base = 0xFF0B0A1F; hemA = 0xFFFF4FB8; hemB = 0xFF3FD8FF; break;
			case "aurora": aurora(f, t); base = 0xFF050A16; hemA = 0xFF3DFFA0; hemB = 0xFFA46BFF; break;
			case "galaxy": galaxy(f, t); base = 0xFF070814; hemA = 0xFF8FB8FF; hemB = 0xFFFFD58A; break;
			case "void": blackHole(f, t); base = 0xFF050406; hemA = 0xFFFFB259; hemB = 0xFFFFF1D6; break;
			case "storm": storm(f, t); base = 0xFF0C1018; hemA = 0xFF9FD4FF; hemB = 0xFFE6F2FF; break;
			case "ender": ender(f, t); base = 0xFF04110F; hemA = 0xFF2CE6B8; hemB = 0xFFB46BFF; break;
			case "synthwave": synthwave(f, t); base = 0xFF1A0730; hemA = 0xFFFF3FA4; hemB = 0xFF35E2FF; break;
			default: eclipse(f, t); base = 0xFF0E0819; hemA = 0xFFB49CFF; hemB = 0xFFE07AFF; break;
		}
		if (fresh) java.util.Arrays.fill(img, base);
		final int ca = hemA, cb2 = hemB;
		// Soft shading towards the edges, and a glowing border with two lights running round it.
		final float run = (t * 0.09f) % 1f, perim = 2 * (UW + UH);
		rows(FH, (y0, y1) -> {
			for (int y = y0; y < y1; y++) {
				for (int x = 0; x < FW; x++) {
					int i = y * FW + x;
					float ux = (x + 0.5f) / S, uy = (y + 0.5f) / S;
					float dl = ux, dr = UW - ux, dt = uy, db = UH - uy;
					float d = Math.min(Math.min(dl, dr), Math.min(dt, db));
					float shade = 1 - 0.32f * (float) Math.exp(-d / 5.5f);
					float sp;
					if (d == dt) sp = ux;
					else if (d == dr) sp = UW + uy;
					else if (d == db) sp = UW + UH + (UW - ux);
					else sp = 2 * UW + UH + (UH - uy);
					float p = sp / perim, d1 = Math.abs(p - run), d2 = Math.abs(p - (run + 0.5f) % 1f);
					d1 = Math.min(d1, 1 - d1) * perim;
					d2 = Math.min(d2, 1 - d2) * perim;
					float runner = (float) Math.exp(-d1 * d1 / 90) + (float) Math.exp(-d2 * d2 / 90);
					float line = (float) Math.exp(-sq((d - 0.9f) / 0.38f)) * (0.3f + 1.6f * runner) + (float) Math.exp(-d / 2.2f) * runner * 0.35f;
					int col = lerp(ca, cb2, 0.5f + 0.5f * (float) Math.sin(p * Math.PI * 4 + t * 0.7f));
					float lr = ((col >> 16) & 255) / 255f, lg = ((col >> 8) & 255) / 255f, lb = (col & 255) / 255f;
					img[(OY + y) * W + OX + x] = rgb(f.r[i] * shade + line * lr, f.g[i] * shade + line * lg, f.b[i] * shade + line * lb);
				}
			}
		});
		hem(img, hemA, hemB, t);
		// Inside (towards your back) and the side edges: the scene's average colour in shadow (sampled).
		long sr = 0, sg = 0, sb = 0, n = 0;
		for (int y = 0; y < FH; y += 6) {
			for (int x = 0; x < FW; x += 6) {
				int c = img[(OY + y) * W + OX + x];
				sr += (c >> 16) & 255;
				sg += (c >> 8) & 255;
				sb += c & 255;
				n++;
			}
		}
		int avg = 0xFF000000 | (int) (sr / n) << 16 | (int) (sg / n) << 8 | (int) (sb / n);
		int inside = shade(avg, 0.55f), edge = shade(avg, 0.4f);
		for (int y = OY; y < OY + FH; y++) {
			for (int x = IX; x < IX + FW; x++) img[y * W + x] = inside;
			for (int x = 0; x < 2 * K; x++) img[y * W + x] = edge;
			for (int x = 22 * K; x < 24 * K; x++) img[y * W + x] = edge;
		}
	}

	// =================================================================== single colours

	/**
	 * One colour, shaded like soft fabric: a touch lighter at the top and towards the middle, darker at
	 * the bottom and edges, with a slightly darker hem. The inside and edges are the shadow side.
	 */
	private static int[] solid(int c) {
		int[] img = new int[W * H];
		java.util.Arrays.fill(img, shade(c, 0.85f));
		for (int y = 0; y < FH; y++) {
			float v = y / (float) (FH - 1);
			for (int x = 0; x < FW; x++) {
				float u = x / (float) (FW - 1);
				float k = 1.06f - 0.16f * v - 0.1f * sq(u * 2 - 1);
				if (y >= FH - (int) (2 * S)) k *= 0.8f;
				img[(OY + y) * W + OX + x] = shade(c, k);
			}
		}
		int inside = shade(c, 0.7f), edge = shade(c, 0.6f);
		for (int y = OY; y < OY + FH; y++) {
			for (int x = IX; x < IX + FW; x++) img[y * W + x] = inside;
			for (int x = 0; x < 2 * K; x++) img[y * W + x] = edge;
			for (int x = 22 * K; x < 24 * K; x++) img[y * W + x] = edge;
		}
		return img;
	}

	// =================================================================== Eclipse

	private static final Star[] ECLIPSE_STARS = stars(1, 90);

	/**
	 * Deep space with a faint Milky Way band, a black moon with a pulsing ring, turning corona rays and
	 * fine streamers, pink prominences on the rim, a bright bead travelling round, and shooting stars.
	 */
	private static void eclipse(F f, float t) {
		float cx = UW / 2, cy = 46, R = 15.5f;
		float pulse = 0.75f + 0.25f * (float) Math.sin(t * 1.6);
		float bead = t * 0.7f;
		float bx = cx + (float) Math.cos(bead) * R, by = cy + (float) Math.sin(bead) * R;
		// Prominences (small glowing loops of pink plasma on the rim): centres and sizes for this frame.
		float[] lx = new float[3], ly = new float[3], lr = new float[3];
		for (int k = 0; k < 3; k++) {
			float pa = 0.9f + k * 2.1f + 0.15f * (float) Math.sin(t * 0.4 + k);
			lx[k] = cx + (float) Math.cos(pa) * (R + 1.2f);
			ly[k] = cy + (float) Math.sin(pa) * (R + 1.2f);
			lr[k] = 2.6f + 0.6f * (float) Math.sin(t * 0.9 + k * 2);
		}
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					int i = py * FW + px;
					float x = (px + 0.5f) / S, y = (py + 0.5f) / S, v = y / UH;
					float dx = x - cx, dy = y - cy, d = (float) Math.sqrt(dx * dx + dy * dy);
					if (d < R - 0.35f) {
						// The moon: almost black, a hint of earthshine on one side.
						float shine = Math.max(0, (dx + dy) / R) * 0.04f;
						f.set(i, 0.012f + shine, 0.01f + shine, 0.02f + shine * 1.4f);
						continue;
					}
					// Night gradient, a drifting haze and a faint diagonal Milky Way band.
					float haze = fbm(x * 0.035f + t * 0.03f, y * 0.035f - t * 0.02f, 3);
					float band = (float) Math.exp(-sq((x * 0.55f + y * 0.35f - 62) / 13));
					float bandDust = band > 0.02f ? fbm(x * 0.09f + 40, y * 0.09f, 4) : 0;
					float st = dust(px, py, t, 1);
					float cr = mix(0.035f, 0.09f, v) + haze * 0.10f + band * (0.06f + bandDust * 0.10f) + st * 0.8f;
					float cg = mix(0.02f, 0.045f, v) + haze * 0.035f + band * (0.04f + bandDust * 0.07f) + st * 0.75f;
					float cb = mix(0.07f, 0.17f, v) + haze * 0.16f + band * (0.10f + bandDust * 0.14f) + st * 0.95f;
	
					float a = (float) Math.atan2(dy, dx), edge = d - R, out = Math.max(0, edge);
					float ring = (float) Math.exp(-edge * edge / 2.6f) * (0.9f + 0.4f * pulse);
					if (d < R) ring *= (d - (R - 0.35f)) / 0.35f;
					// Corona: long soft rays that slowly turn and breathe, plus thin streamers.
					float r1 = 0.5f + 0.5f * (float) Math.sin(a * 11 + t * 0.35 + 2.2 * Math.sin(a * 4 - t * 0.5));
					float r2 = 0.5f + 0.5f * (float) Math.sin(a * 23 - t * 0.6);
					float rays = r1 * r1, rays2 = r2 * r2 * r2 * r2 * 0.5f;
					float s1 = 0.5f + 0.5f * (float) Math.sin(a * 61 + edge * 0.08f + t * 0.25f), s4 = s1 * s1 * s1 * s1;
					float streak = s4 * s4 * s4 * (float) Math.exp(-out / 26);
					float corona = (float) Math.exp(-out / (5.5f + 9f * rays + 4f * rays2)) * (0.55f + 0.45f * pulse);
					float light = 0, prom = 0;
					if (edge < 7) {
						// The bead: a hot spot on the rim.
						float da = angleDiff(a, bead);
						if (Math.abs(da) < 0.7f) light += (float) Math.exp(-(da * da) * 90 - edge * edge / 2.2f) * 2.4f;
						if (edge > 0.3f) {
							for (int k = 0; k < 3; k++) {
								float ld = (float) Math.sqrt(sq(x - lx[k]) + sq(y - ly[k]));
								if (Math.abs(ld - lr[k]) < 2.5f) prom += (float) Math.exp(-sq((ld - lr[k]) / 0.55f)) * (0.7f + 0.3f * (float) Math.sin(t * 2.5 + k * 3 + a * 9));
							}
						}
					}
					// The bead's cross flare.
					float fx = Math.abs(x - bx), fy = Math.abs(y - by);
					float flare = 0;
					if (fx < 24 && fy < 3) flare += (float) Math.exp(-fy * fy * 3f) * (1 - fx / 24);
					if (fy < 24 && fx < 3) flare += (float) Math.exp(-fx * fx * 3f) * (1 - fy / 24);
					flare *= 0.6f + 0.4f * pulse;
					float pinkness = Math.min(1, out / 18);
					f.set(i,
							cr + ring * 0.95f + corona * mix(0.62f, 0.85f, pinkness) + streak * 0.12f + light + flare * 0.9f + prom * 1.0f,
							cg + ring * 0.9f + corona * mix(0.5f, 0.38f, pinkness) + streak * 0.1f + light + flare * 0.85f + prom * 0.25f,
							cb + ring * 1.0f + corona * mix(1.0f, 0.95f, pinkness) + streak * 0.15f + light + flare + prom * 0.55f);
				}
			}
		});
		drawStars(f, ECLIPSE_STARS, t, cx, cy, R + 3, 0.92f, 0.9f, 1f, 0);
		shootingStar(f, t, 5.5f, 0.9f, cx, cy, R + 1, 0.95f, 0.9f, 1f);
		for (int k = 0; k < 16; k++) {
			float ph = hash(k, 21) * 6.283f, sp = 0.35f + 0.25f * hash(k, 22), rad = R + 5 + 12 * hash(k, 23);
			for (int tr = 0; tr < 6; tr++) {
				float a = ph + (t - tr * 0.035f) * sp;
				float ox = (float) Math.cos(a) * rad, oy = (float) Math.sin(a) * rad * 0.32f;
				float rx = ox * 0.94f - oy * 0.34f, ry = ox * 0.34f + oy * 0.94f; // tilt the ring
				boolean behind = Math.sin(a) < 0 && Math.hypot(rx, ry) < R + 0.5f;
				if (behind) continue;
				float tw = 0.6f + 0.4f * (float) Math.sin(t * 6 + k);
				glow(f, cx + rx, cy + ry, tr == 0 ? 0.55f : 0.4f, (tr == 0 ? 1.6f : 0.5f * (1 - tr / 6f)) * tw, 0.95f, 0.85f, 1f);
			}
		}
	}

	// =================================================================== Corona

	private static final Star[] EMBERS = stars(2, 60);

	/** A black sun ringed by streaming fire and looping flame arcs, embers rising through red smoke. */
	private static void corona(F f, float t) {
		float cx = UW / 2, cy = 50, R = 14.5f;
		// Flame arcs (two loops of fire standing on the sun, slowly rising and falling) for this frame.
		float[] ax = new float[2], ay = new float[2], ar = new float[2];
		for (int k = 0; k < 2; k++) {
			float pa = -1.9f + k * 2.4f + 0.2f * (float) Math.sin(t * 0.3 + k);
			float h = 6.5f + 1.5f * (float) Math.sin(t * 0.8 + k * 2);
			ax[k] = cx + (float) Math.cos(pa) * (R + h * 0.35f);
			ay[k] = cy + (float) Math.sin(pa) * (R + h * 0.35f);
			ar[k] = h * 0.75f;
		}
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					int i = py * FW + px;
					float x = (px + 0.5f) / S, y = (py + 0.5f) / S, v = y / UH;
					float dx = x - cx, dy = y - cy, d = (float) Math.sqrt(dx * dx + dy * dy);
					if (d < R - 0.35f) {
						// The sun's dark face with faint churning granules.
						float gran = fbm(x * 0.4f + t * 0.2f, y * 0.4f, 2) * 0.03f;
						f.set(i, 0.03f + gran, 0.008f + gran * 0.3f, 0.005f);
						continue;
					}
					float smoke = fbm(x * 0.05f, y * 0.04f + t * 0.35f, 4);
					float smoke2 = fbm(x * 0.11f - t * 0.05f, y * 0.09f + t * 0.6f, 3);
					float cr = mix(0.05f, 0.16f, v) + smoke * 0.16f + smoke2 * 0.05f;
					float cg = mix(0.012f, 0.03f, v) + smoke * 0.035f + smoke2 * 0.01f;
					float cb = 0.012f + smoke * 0.01f;
	
					float edge = d - R, heat = 0;
					// Flames reach at most 29 units out, so further away only the glow is left.
					if (edge < 29) {
						float ux = dx / d, uy = dy / d;
						float tongue = fbm(ux * 1.7f + t * 0.12f, uy * 1.7f - t * 0.17f, 3);
						float height = 5 + 24 * tongue * (float) Math.sqrt(tongue) * (0.85f + 0.15f * tongue);
						if (edge < height) {
							float s = 2.6f + (edge - t * 11f) * 0.11f;
							float flick = fbm(ux * s + 3.1f, uy * s - 1.7f, 4);
							float fine = fbm(ux * s * 2.7f, uy * s * 2.7f + 5, 2);
							heat = (1 - edge / height) * (0.4f + 0.9f * flick + 0.25f * fine);
							heat = Math.min(1.2f, heat);
							heat = heat * heat * (3 - 2 * Math.min(1, heat));
							if (d < R) heat *= (d - (R - 0.35f)) / 0.35f;
						}
					}
					float arc = 0;
					if (edge >= 0 && edge < 16) {
						for (int k = 0; k < 2; k++) {
							float ld = (float) Math.sqrt(sq(x - ax[k]) + sq(y - ay[k]));
							if (Math.abs(ld - ar[k]) < 3.5f) arc += (float) Math.exp(-sq((ld - ar[k]) / 0.9f)) * (0.6f + 0.6f * fbm(ld * 0.5f + t * 2, k * 7, 2));
						}
					}
					float glow = (float) Math.exp(-Math.max(0, edge) / 18) * (0.32f + 0.08f * (float) Math.sin(t * 2.3));
					heat = Math.min(1.3f, heat + arc * 0.8f);
					f.set(i, cr + Math.min(1.2f, heat * 2.2f) + glow, cg + Math.max(0, heat * 1.9f - 0.45f) + glow * 0.35f, cb + Math.max(0, heat * 2.4f - 1.75f) + glow * 0.05f);
				}
			}
		});
		for (Star e : EMBERS) {
			float life = (t * e.speed * 0.12f + e.phase) % 1f;
			float ex = e.x * UW + (float) Math.sin(t * 1.7 + e.phase * 20) * 3.5f;
			float ey = UH + 4 - life * (UH + 8);
			if (Math.hypot(ex - cx, ey - cy) < R + 2) continue;
			float bright = (float) Math.sin(Math.PI * life) * (0.65f + 0.35f * (float) Math.sin(t * 13 + e.phase * 40)) * e.size * 1.4f;
			glow(f, ex, ey, 0.5f + e.size * 0.45f, bright * 1.4f, 1.0f, 0.75f, 0.35f);
			for (int k = 1; k <= 5; k++) glow(f, ex - (float) Math.cos(t * 1.7 + e.phase * 20) * k * 0.6f, ey + k * 1.4f, 0.45f + e.size * 0.25f, bright * 0.35f * (1 - k / 6f), 1.0f, 0.45f, 0.1f);
		}
	}

	// =================================================================== Nebula

	private static final Star[] NEBULA_STARS = stars(3, 100);

	/** Swirling gas clouds in purple, pink and cyan with dark dust lanes, a bright star-forming core and sparkling stars. */
	private static void nebula(F f, float t) {
		float coreX = 47, coreY = 54;
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					int i = py * FW + px;
					float x = (px + 0.5f) / S, y = (py + 0.5f) / S;
					float nx = x * 0.028f, ny = y * 0.028f + t * 0.09f;
					float qx = fbm(nx + t * 0.05f, ny, 4), qy = fbm(nx + 5.2f, ny + 1.3f - t * 0.04f, 4);
					float n = fbm(nx + 3.5f * qx + 1.7f, ny + 3.5f * qy + 9.2f + t * 0.06f, 5);
					float wisp = fbm(nx * 3.1f + qx * 2, ny * 3.1f - t * 0.05f, 3);
					float dustv = fbm(nx * 2.6f - t * 0.04f, ny * 2.6f, 3);
					float cr = 0.02f, cg = 0.025f, cb = 0.08f;
					float purple = smooth(0.35f, 0.75f, n), pink = smooth(0.55f, 0.95f, qx * n * 1.6f), cyan = smooth(0.5f, 0.9f, qy * 1.2f) * smooth(0.3f, 0.7f, n);
					float fil = smooth(0.55f, 0.8f, wisp) * n;
					cr += purple * 0.42f + pink * 0.95f + cyan * 0.05f + fil * 0.3f;
					cg += purple * 0.12f + pink * 0.22f + cyan * 0.75f + fil * 0.2f;
					cb += purple * 0.85f + pink * 0.55f + cyan * 0.95f + fil * 0.45f;
					// Bright core: a hot blue-white glow that breathes.
					float cd = (float) Math.hypot(x - coreX, y - coreY);
					float core = (float) Math.exp(-cd / 7) * (0.55f + 0.1f * (float) Math.sin(t * 1.3)) + (float) Math.exp(-cd * cd / 6) * 0.8f;
					float lane = 1 - smooth(0.55f, 0.8f, dustv) * 0.7f;
					float st = dust(px, py, t, 3);
					f.set(i, cr * lane + core * 0.75f + st, cg * lane + core * 0.85f + st, cb * lane + core + st);
				}
			}
		});
		// The core's star with long diffraction spikes.
		spikes(f, coreX, coreY, 26 + 4 * (float) Math.sin(t * 1.3), 1.4f, 0.85f, 0.92f, 1f, t * 0.05f);
		drawStars(f, NEBULA_STARS, t, -100, -100, 0, 1f, 0.92f, 1f, 1.1f);
	}

	// =================================================================== Aurora

	private static final Star[] AURORA_STARS = stars(4, 70);
	private static final float[][] CURTAINS = {{50, 0.07f, 0.9f, 1.0f}, {66, 0.11f, -0.7f, 0.8f}, {34, 0.05f, 0.5f, 0.55f}};
	private static final float LAKE = 112;

	/** Northern lights over a snowy ridge with pine trees, mirrored in a frozen lake. */
	private static void aurora(F f, float t) {
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					float y = (py + 0.5f) / S, v = y / UH;
					int i = py * FW + px;
					f.set(i, mix(0.005f, 0.03f, v), mix(0.012f, 0.06f, v), mix(0.04f, 0.13f, v));
				}
			}
		});
		drawStars(f, AURORA_STARS, t, -100, -100, 0, 0.9f, 0.95f, 1f, 0);
		for (int py = 0; py < FH; py++) for (int px = 0; px < FW; px++) {
			float d = dust(px, py, t, 4) * 0.8f;
			f.add(px, py, d, d, d * 1.1f);
		}
		for (float[] c : CURTAINS) {
			for (int px = 0; px < FW; px++) {
				float x = (px + 0.5f) / S;
				float edge = c[0] + 9 * (float) Math.sin(x * c[1] + t * c[2]) + 4.5f * (float) Math.sin(x * c[1] * 2.7f - t * c[2] * 1.6f + 1.3f)
						+ 3 * fbm(x * 0.06f, t * 0.4f + c[0], 2);
				float ray = 0.45f + 0.55f * (float) Math.pow(0.5 + 0.5 * Math.sin(x * 0.75f + 4 * Math.sin(t * 0.6 + x * 0.04f) + c[0]), 2);
				ray *= 0.75f + 0.25f * (float) Math.sin(t * 5 + x * 0.9f + c[0]);
				// Fine vertical striations on top of the big rays.
				ray *= 0.8f + 0.2f * (float) Math.sin(x * 3.1f + t * 1.7f + c[0]);
				for (int py = 0; py < FH; py++) {
					float y = (py + 0.5f) / S;
					if (y > LAKE) break;
					float up = edge - y;
					float a = up < 0 ? (float) Math.exp(up * 0.3f) : (float) Math.exp(-up / 26f) * (1 - (float) Math.exp(-up * 0.9f) * 0.25f);
					a *= ray * c[3];
					if (a < 0.003f) continue;
					float h = Math.max(0, Math.min(1, up / 40f));
					float ar = a * (0.12f * (1 - h) + 0.62f * h), ag = a * (1.0f * (1 - h) + 0.35f * h), ab = a * (0.55f * (1 - h) + 1.0f * h);
					f.add(px, py, ar, ag, ab);
				}
			}
		}
		// Mountains with snowy slopes, a dark row of pines along the shore, then the frozen lake.
		rows(FW, (px0, px1) -> {
			for (int px = px0; px < px1; px++) {
				float x = (px + 0.5f) / S;
				float ridge = 92 + 8 * (float) Math.sin(x * 0.11f + 0.6) + 5 * (float) Math.sin(x * 0.29f + 2.1) + 3f * fbm(x * 0.15f, 3.3f, 3);
				// Pines: a sawtooth of narrow triangles of varying height.
				float cell = x / 3.2f, fr = cell - (float) Math.floor(cell);
				float treeH = 4 + 5 * hash((int) Math.floor(cell), 77);
				float pine = LAKE - 1 - treeH * (1 - Math.abs(fr - 0.5f) * 2) * (0.75f + 0.25f * (float) Math.abs(Math.sin(fr * 18)));
				for (int py = 0; py < FH; py++) {
					float y = (py + 0.5f) / S;
					int i = py * FW + px;
					if (y >= LAKE) break;
					if (y >= pine) {
						f.set(i, 0.006f, 0.012f, 0.016f);
					} else if (y >= ridge) {
						float depth = y - ridge;
						float snow = Math.max(0, 1 - depth / 4.5f) * (0.55f + 0.45f * (float) Math.sin(x * 0.7f)) + fbm(x * 0.6f, y * 0.6f, 2) * 0.12f * Math.max(0, 1 - depth / 12);
						float rim = (float) Math.exp(-depth * 0.8f) * 0.25f * (0.8f + 0.2f * (float) Math.sin(t * 1.3 + x * 0.2f));
						f.set(i, 0.02f + snow * 0.5f + rim * 0.2f, 0.03f + snow * 0.58f + rim * 0.9f, 0.05f + snow * 0.72f + rim * 0.6f);
					}
				}
			}
		});
		// The lake mirrors everything above its shore, darker and gently rippling.
		int lakePy = (int) (LAKE * S);
		rows(FH - lakePy, (q0, q1) -> {
		for (int py = lakePy + q0; py < lakePy + q1; py++) {
			float depth = (py - lakePy) / S;
			for (int px = 0; px < FW; px++) {
				float ripple = (float) Math.sin(depth * 2.2f - t * 2.5f + px * 0.02f) * 0.9f;
				int sx = Math.max(0, Math.min(FW - 1, px + Math.round(ripple)));
				int sy = Math.max(0, lakePy - 1 - (py - lakePy));
				int s = sy * FW + sx, i = py * FW + px;
				float k = 0.45f * (1 - depth / 20f) + 0.08f;
				float ice = fbm(px * 0.08f, py * 0.3f, 2) * 0.03f;
				f.set(i, f.r[s] * k + ice, f.g[s] * k + ice, f.b[s] * k + ice * 1.4f);
			}
		}
		});
		for (int px = 0; px < FW; px++) f.add(px, lakePy, 0.12f, 0.2f, 0.22f);
		shootingStar(f, t, 9f, 0.8f, -100, -100, 0, 0.8f, 1f, 0.95f);
		for (int k = 0; k < 90; k++) {
			float sp = 4 + 7 * hash(k, 31), size = 0.3f + 0.45f * hash(k, 32);
			float y = (hash(k, 33) * (UH + 10) + t * sp) % (UH + 10) - 5;
			float x = hash(k, 34) * UW + (float) Math.sin(t * (0.6f + hash(k, 35)) + k) * 2.5f;
			glow(f, x, y, size, 0.25f + 0.35f * hash(k, 36), 0.9f, 0.95f, 1f);
		}
	}

	// =================================================================== Galaxy

	private static final Star[] GALAXY_STARS = stars(5, 110);

	/** A spiral galaxy seen at an angle, slowly turning: glowing core, blue arms with pink knots and dark dust lanes. */
	private static void galaxy(F f, float t) {
		float cx = UW / 2, cy = 58, tilt = 0.58f, spin = -t * 0.12f, lean = 0.5f;
		float cl = (float) Math.cos(lean), sl = (float) Math.sin(lean);
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					int i = py * FW + px;
					float x = (px + 0.5f) / S - cx, y = (py + 0.5f) / S - cy;
					// Into the galaxy's own plane: undo the lean, then the tilt.
					float gx = x * cl + y * sl, gy = (-x * sl + y * cl) / tilt;
					float r = (float) Math.hypot(gx, gy), th = (float) Math.atan2(gy, gx) + spin;
					float lr = (float) Math.log(r + 1.5f);
					float arm = (float) Math.pow(0.5 + 0.5 * Math.cos(2 * (th - lr * 2.6f)), 3);
					float armN = fbm(gx * 0.12f, gy * 0.12f + t * 0.01f, 4);
					float disc = (float) Math.exp(-r / 21);
					float arms = arm * disc * (0.55f + 0.9f * armN);
					// Dust lane hugging the inner edge of each arm.
					float lane = (float) Math.pow(0.5 + 0.5 * Math.cos(2 * (th - lr * 2.6f) - 0.9f), 8) * smooth(4, 10, r) * 0.75f;
					float knots = smooth(0.62f, 0.8f, fbm(gx * 0.35f + 11, gy * 0.35f, 3)) * arm * disc * 1.6f;
					float core = (float) Math.exp(-r * r / 22) * 1.6f + (float) Math.exp(-r / 5) * 0.5f;
					float halo = (float) Math.exp(-r / 40) * 0.05f;
					float bg = 0.012f;
					float st = dust(px, py, t, 5);
					float dim = 1 - lane;
					f.set(i,
							bg + (arms * 0.55f + halo * 0.7f) * dim + knots * 0.95f + core * 1.0f + st,
							bg + (arms * 0.7f + halo * 0.7f) * dim + knots * 0.3f + core * 0.86f + st,
							bg * 2 + (arms * 1.15f + halo) * dim + knots * 0.75f + core * 0.62f + st * 1.1f);
				}
			}
		});
		spikes(f, cx, cy, 10, 0.8f, 1f, 0.95f, 0.85f, 0);
		drawStars(f, GALAXY_STARS, t, -100, -100, 0, 0.95f, 0.95f, 1f, 0.6f);
		shootingStar(f, t, 7f, 0.8f, -100, -100, 0, 0.9f, 0.95f, 1f);
	}

	// =================================================================== Void (black hole)

	/**
	 * A black hole: a dark shadow edged by a thin photon ring; a thin, tilted accretion disc crossing in
	 * front, swirling faster near the middle and brighter on the side coming towards you; the far side of
	 * the disc bent by gravity into a narrow arch over the top (and a fainter one underneath); a warm glow
	 * of gas around it, and the starfield stretched around the edge.
	 */
	private static void blackHole(F f, float t) {
		float cx = UW / 2, cy = 54, rs = 10.5f, tilt = 0.16f;
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					int i = py * FW + px;
					float x = (px + 0.5f) / S - cx, y = (py + 0.5f) / S - cy;
					float d = (float) Math.hypot(x, y), a = (float) Math.atan2(y, x);
					// Stars seen through the lens: sampled further out near the hole.
					float bend = rs * rs * 1.8f / Math.max(d, 0.5f);
					float spx = (cx + x + x / Math.max(d, 0.01f) * bend) * S, spy = (cy + y + y / Math.max(d, 0.01f) * bend) * S;
					float star = starAt((int) Math.floor(spx), (int) Math.floor(spy), t, 6) * 1.2f;
					float haze = fbm(x * 0.04f + 30, y * 0.04f, 3);
					float warm = (float) Math.exp(-Math.max(0, d - rs) / 16) * 0.22f;
					float cr = 0.012f + haze * 0.04f + star + warm, cg = 0.01f + haze * 0.025f + star + warm * 0.55f, cb = 0.02f + haze * 0.06f + star * 1.1f + warm * 0.25f;
	
					// Arches: the far side of the disc, lensed into thin bands hugging the shadow.
					float arch = 0;
					float up = (float) Math.max(0, -Math.sin(a)), down = (float) Math.max(0, Math.sin(a));
					float bandTop = (float) Math.exp(-sq((d - rs * (1.32f + 0.18f * up)) / (rs * (0.07f + 0.16f * up))));
					float bandBot = (float) Math.exp(-sq((d - rs * 1.16f) / (rs * 0.06f))) * 0.45f;
					if (y < 0) arch += bandTop * discTexture(rs * (1.8f + 1.5f * (d / rs - 1.15f)), a * 1.4f, t, rs) * (0.35f + 0.65f * up);
					else arch += bandBot * discTexture(rs * 1.9f, -a * 1.4f, t, rs) * down;
					float ring = (float) Math.exp(-sq((d - rs * 1.04f) / 0.3f)) * 1.1f;
					float shadow = smooth(rs * 0.97f, rs * 1.01f, d);
					cr = (cr + arch * 1.2f + ring * 1.0f) * shadow;
					cg = (cg + arch * 0.8f + ring * 0.8f) * shadow;
					cb = (cb + arch * 0.42f + ring * 0.6f) * shadow;
	
					// The near side of the disc: a thin tilted ellipse in front of everything.
					float dy = y / tilt, dr = (float) Math.hypot(x, dy);
					if (dr > rs * 1.5f && dr < rs * 4.6f && (y > 0 || d > rs * 1.02f)) {
						float da = (float) Math.atan2(dy, x);
						float thick = smooth(rs * 1.5f, rs * 1.9f, dr) * (1 - smooth(rs * 3.0f, rs * 4.6f, dr));
						float beam = 1 + 0.5f * (float) Math.cos(da - (float) Math.PI); // the left side comes towards you
						float light = discTexture(dr, da, t, rs) * thick * beam * (float) Math.pow(rs * 1.7f / dr, 1.2);
						cr += light * 1.25f;
						cg += light * 0.82f;
						cb += light * 0.4f;
					}
					f.set(i, cr, cg, cb);
				}
			}
		});
		infall(f, t, cx, cy, rs, tilt);
	}

	/** Clumps of gas spiralling into the black hole in the disc's (tilted) plane, hidden behind the shadow. */
	private static void infall(F f, float t, float cx, float cy, float rs, float tilt) {
		for (int k = 0; k < 46; k++) {
			float life = (t * (0.07f + 0.05f * hash(k, 41)) + hash(k, 42)) % 1f;
			float r = rs * 1.4f + rs * 3.4f * (float) Math.pow(1 - life, 0.8);
			float a = hash(k, 43) * 6.283f + life * 9;
			float x = (float) Math.cos(a) * r, y = (float) Math.sin(a) * r * tilt;
			if (y < 0 && Math.hypot(x, y) < rs * 1.1f) continue;
			float heat = 0.5f + life;
			glow(f, cx + x, cy + y, 0.45f + 0.25f * hash(k, 44), heat * 0.55f * (float) Math.sin(Math.PI * Math.min(1, life * 1.3f)), 1f, 0.7f + 0.2f * life, 0.35f + 0.4f * life);
		}
	}

	/** Swirling streaks of the disc at radius r and angle a: inner parts orbit faster, winding them into spirals. */
	private static float discTexture(float r, float a, float t, float rs) {
		float omega = 2.4f * (float) Math.pow(rs * 1.6f / Math.max(r, rs), 1.5);
		float sa = a - t * omega;
		float streaks = fbm((float) Math.cos(sa) * r * 0.25f, (float) Math.sin(sa) * r * 0.25f, 4);
		float fine = fbm((float) Math.cos(sa) * r * 0.9f + 7, (float) Math.sin(sa) * r * 0.9f, 2);
		return 0.35f + 1.1f * streaks * streaks + 0.08f * fine;
	}

	// =================================================================== Storm

	private static final float BOLT_PERIOD = 1.9f;

	/** Rolling thunderclouds, driving rain and branching lightning that lights the clouds from inside. */
	private static void storm(F f, float t) {
		int n = (int) (t / BOLT_PERIOD);
		float since = t - n * BOLT_PERIOD;
		// Two quick flickers, then a fade.
		float flash = since < 0.6f ? (float) Math.exp(-since * 6) * (since < 0.08f || (since > 0.14f && since < 0.24f) ? 1 : 0.35f) : 0;
		Random rnd = new Random(n * 92821L + 7);
		float[][] bolt = bolt(rnd);
		float bx = bolt[0][0];
		// Sheet lightning: dimmer flashes hidden inside the clouds, between the bolts.
		float mid = t - n * BOLT_PERIOD - BOLT_PERIOD * 0.55f;
		float sheet = mid > 0 && mid < 0.5f ? (float) Math.exp(-mid * 7) * 0.55f : 0;
		Random srnd = new Random(n * 31337L + 3);
		float shx = 10 + srnd.nextFloat() * 60, shy = 15 + srnd.nextFloat() * 50;
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					int i = py * FW + px;
					float x = (px + 0.5f) / S, y = (py + 0.5f) / S, v = y / UH;
					float c1 = fbm(x * 0.03f + t * 0.06f, y * 0.045f, 5);
					float c2 = fbm(x * 0.07f - t * 0.1f, y * 0.08f + 3, 4);
					float cloud = smooth(0.3f, 0.75f, c1 * 0.7f + c2 * 0.45f);
					float top = mix(1, 0.35f, v);
					float base = 0.03f + cloud * 0.16f * top;
					// Lightning lights the clouds near the bolt the most.
					float near = (float) Math.exp(-sq((x - bx) / 22)) * flash;
					float inside = (float) Math.exp(-(sq(x - shx) + sq(y - shy)) / 500) * sheet * (0.3f + cloud * 1.4f);
					float lit = (near * 1.2f + flash * 0.25f) * (0.4f + cloud) + inside;
					f.set(i, base * 0.8f + lit * 0.75f, base * 0.9f + lit * 0.82f, base * 1.25f + lit * 1.0f);
				}
			}
		});
		if (flash > 0.02f) {
			for (float[] seg : bolt) line(f, seg[0], seg[1], seg[2], seg[3], seg[4] * 0.55f, flash * seg[4] * 2.2f, 0.85f, 0.92f, 1f);
		}
		// Rain: slanted streaks falling fast, a few layers deep.
		for (int k = 0; k < 140; k++) {
			float sp = 70 + 50 * hash(k, 3), x0 = hash(k, 1) * (UW + 30), ph = hash(k, 2);
			float y0 = ((t * sp + ph * UH * 3) % (UH + 20)) - 10;
			float x = (x0 - y0 * 0.25f) % (UW + 30) - 15;
			float a = 0.08f + 0.12f * hash(k, 4) + flash * 0.25f;
			line(f, x, y0, x - 1.2f, y0 + 4.5f, 0.25f, a, 0.75f, 0.85f, 1f);
		}
	}

	/** A lightning bolt as segments {x0, y0, x1, y1, width}: a jagged main channel plus forks. */
	private static float[][] bolt(Random rnd) {
		java.util.List<float[]> segs = new java.util.ArrayList<>();
		float x = 20 + rnd.nextFloat() * 40, y = -2;
		float endY = 70 + rnd.nextFloat() * 45;
		while (y < endY) {
			float nx = x + (rnd.nextFloat() - 0.5f) * 9, ny = y + 3 + rnd.nextFloat() * 5;
			segs.add(new float[] {x, y, nx, ny, 1f});
			if (rnd.nextFloat() < 0.28f) {
				float fx = nx, fy = ny, dir = rnd.nextBoolean() ? 1 : -1;
				int len = 2 + rnd.nextInt(4);
				for (int k = 0; k < len; k++) {
					float gx = fx + dir * (2 + rnd.nextFloat() * 5), gy = fy + 2 + rnd.nextFloat() * 4;
					segs.add(new float[] {fx, fy, gx, gy, 0.5f - k * 0.08f});
					fx = gx;
					fy = gy;
				}
			}
			x = nx;
			y = ny;
		}
		return segs.toArray(new float[0][]);
	}

	// =================================================================== Ender

	/** The End portal's layered shimmer, purple particles spiralling in, and an Eye of Ender that looks around and blinks. */
	private static void ender(F f, float t) {
		float ex = UW / 2, ey = 54;
		// The pupil wanders smoothly and the eye blinks every few seconds.
		float lookX = 3.2f * (float) Math.sin(t * 0.7) + 1.2f * (float) Math.sin(t * 1.9), lookY = 1.8f * (float) Math.sin(t * 0.9 + 1);
		float bp = (t % 4.7f) / 0.28f;
		float open = bp < 1 ? (float) Math.abs(Math.cos(bp * Math.PI)) : 1;
		float[][] layers = {{0.7f, 0.2f, 0.8f, 0.2f}, {1.0f, -0.15f, 0.35f, 0.9f}, {1.4f, 0.1f, 0.25f, 0.6f}, {1.9f, -0.05f, 0.45f, 0.95f}};
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					int i = py * FW + px;
					float x = (px + 0.5f) / S, y = (py + 0.5f) / S;
					float cr = 0.008f, cg = 0.03f, cb = 0.035f;
					// Portal layers: grids of specks scrolling at different speeds and angles, like the block in game.
					for (int L = 0; L < layers.length; L++) {
						float[] l = layers[L];
						float ang = 0.4f + L * 0.9f;
						float rx = (x * (float) Math.cos(ang) - y * (float) Math.sin(ang)) * l[0] + t * l[1] * 9, ry = (x * (float) Math.sin(ang) + y * (float) Math.cos(ang)) * l[0] + t * 1.4f * (L + 1);
						int gx = (int) Math.floor(rx), gy = (int) Math.floor(ry);
						float h = hash3(gx, gy, L);
						if (h > 0.955f) {
							float tw = 0.5f + 0.5f * (float) Math.sin(t * (2 + 3 * hash3(gx, gy, L + 40)) + hash3(gx, gy, L + 20) * 6.283f);
							float k = (0.4f + 0.6f * (h - 0.955f) / 0.045f) * tw * 0.95f;
							cr += k * l[2] * 0.6f;
							cg += k * (0.6f + 0.4f * l[3]);
							cb += k * (0.5f + 0.5f * l[2]);
						}
					}
					float mist = fbm(x * 0.05f - t * 0.05f, y * 0.05f + t * 0.08f, 3);
				float breath = smooth(70, 128, y) * smooth(0.4f, 0.85f, fbm(x * 0.07f + t * 0.25f, y * 0.05f - t * 0.12f, 4));
				cr += breath * 0.55f;
				cg += breath * 0.12f;
				cb += breath * 0.75f;
					cr += mist * 0.03f;
					cg += mist * 0.08f;
					cb += mist * 0.07f;
					// The eye: almond shape, green iris with darker rays, slit pupil.
					float qx = (x - ex) / 15, qy = (y - ey) / (8.5f * Math.max(0.04f, open));
					float almond = qx * qx + qy * qy * (1 + qx * qx * 0.6f);
					float aura = (float) Math.exp(-Math.max(0, almond - 1) * 1.6f) * 0.5f;
					cr += aura * 0.25f;
					cg += aura * 0.75f;
					cb += aura * 0.55f;
					if (almond < 1 && open > 0.05f) {
						float ix = x - ex - lookX, iy = y - ey - lookY * open, id = (float) Math.hypot(ix, iy);
						float iris = smooth(7.2f, 6.6f, id);
						float rays = 0.75f + 0.25f * (float) Math.sin(Math.atan2(iy, ix) * 18 + id);
						float pupil = smooth(1.6f, 1.1f, Math.abs(ix) / Math.max(0.3f, 1 - sq(iy / 5.5f)) * 1.0f) * smooth(5.8f, 5.2f, Math.abs(iy));
						float white = 0.85f - 0.25f * almond;
						float er = white * 0.75f * (1 - iris) + iris * rays * mix(0.2f, 0.05f, id / 7) * (1 - pupil);
						float eg = white * 0.92f * (1 - iris) + iris * rays * mix(0.95f, 0.5f, id / 7) * (1 - pupil);
						float eb = white * 0.85f * (1 - iris) + iris * rays * mix(0.6f, 0.3f, id / 7) * (1 - pupil);
						// A glint and a soft dark rim.
						float glint = (float) Math.exp(-sq(ix + 2.2f) * 1.6f - sq(iy + 2.2f) * 1.6f) * 0.9f;
						float rim = smooth(0.7f, 1f, almond);
						cr = mix(er + glint, cr, rim);
						cg = mix(eg + glint, cg, rim);
						cb = mix(eb + glint, cb, rim);
					}
					f.set(i, cr, cg, cb);
				}
			}
		});
		// Portal particles spiralling into the eye.
		for (int k = 0; k < 70; k++) {
			float life = (t * (0.18f + 0.12f * hash(k, 9)) + hash(k, 8)) % 1f;
			float rad = 44 * (1 - life) + 9;
			float a = hash(k, 7) * 6.283f + life * 4.5f;
			float px = ex + (float) Math.cos(a) * rad, py = ey + (float) Math.sin(a) * rad * 0.9f;
			float br = (float) Math.sin(Math.PI * life) * 0.9f;
			glow(f, px, py, 0.55f, br, 0.75f, 0.35f, 1f);
		}
	}

	// =================================================================== Synthwave

	/** A striped retro sun sinking behind neon mountains, a glowing grid racing towards you, stars above. */
	private static void synthwave(F f, float t) {
		float hor = 80, sx = UW / 2, sy = 62, sr = 21;
		rows(FH, (py0, py1) -> {
			for (int py = py0; py < py1; py++) {
				for (int px = 0; px < FW; px++) {
					int i = py * FW + px;
					float x = (px + 0.5f) / S, y = (py + 0.5f) / S;
					float cr, cg, cb;
					if (y < hor) {
						float v = y / hor;
						cr = mix(0.06f, 0.75f, v * v);
						cg = mix(0.02f, 0.12f, v * v * v);
						cb = mix(0.2f, 0.42f, v);
						float st = dust(px, py, t, 8) * (1 - v);
						cr += st;
						cg += st;
						cb += st;
						// Sun: yellow to hot pink, cut by bands that drift down and widen.
						float d = (float) Math.hypot(x - sx, y - sy);
						float sun = smooth(sr + 0.4f, sr - 0.4f, d);
						float u = (y - (sy - sr)) / (2 * sr);
						if (u > 0.45f) {
							float bands = ((y + t * 3.2f) % 5.2f) / 5.2f;
							float gap = 0.12f + (u - 0.45f) * 0.9f;
							if (bands < gap) sun = 0;
						}
						float glowS = (float) Math.exp(-Math.max(0, d - sr) / 9) * 0.55f;
						cr += sun * 1.4f + glowS * 0.9f;
						cg += sun * mix(1.05f, 0.2f, u) + glowS * 0.25f;
						cb += sun * mix(0.3f, 0.55f, u) + glowS * 0.5f;
						// Mountains: two ranges, the front one with a neon edge.
						float m1 = hor - 10 - 7 * Math.abs((float) Math.sin(x * 0.09f + 1.2)) - 4 * fbm(x * 0.12f, 1, 3);
						float m2 = hor - 4 - 9 * (float) Math.pow(Math.abs(Math.sin(x * 0.06f + 2.4)), 1.5) - 2 * fbm(x * 0.3f, 5, 2);
						if (y > m1) {
							float k = 0.35f;
							cr = cr * k + 0.06f;
							cg = cg * k + 0.01f;
							cb = cb * k + 0.12f;
						}
						if (y > m2) {
							cr = 0.05f;
							cg = 0.01f;
							cb = 0.1f;
							float rim = (float) Math.exp(-(y - m2) * 1.6f);
							cr += rim * 1.0f;
							cg += rim * 0.25f;
							cb += rim * 0.9f;
						}
					} else {
						// Ground: perspective grid that scrolls towards you.
						float z = 1 / Math.max(0.02f, (y - hor) / (UH - hor));
						float gzx = (x - sx) * z * 0.1f;
						float gz = z * 1.6f + t * 2.2f;
						float lx = Math.abs(gzx - Math.round(gzx)), lz = Math.abs(gz - Math.round(gz));
						// Line widths follow the perspective so every line is about one unit thick on screen.
						float wx = z * 0.1f * 0.45f, wz = 1.6f * z * z / (UH - hor) * 0.45f;
						float line = Math.max(smooth(wx * 1.6f, wx * 0.6f, lx), smooth(wz * 1.6f, wz * 0.6f, lz));
						float fade = (float) Math.exp(-z * 0.13f);
						cr = 0.05f + line * 1.1f * fade;
						cg = 0.01f + line * 0.2f * fade;
						cb = 0.12f + line * 0.95f * fade;
						float hg = (float) Math.exp(-(y - hor) * 0.5f);
						cr += hg * 0.45f;
						cg += hg * 0.08f;
						cb += hg * 0.4f;
					}
					f.set(i, cr, cg, cb);
				}
			}
		});
		for (int px = 0; px < FW; px++) f.add(px, (int) (hor * S), 1.1f, 0.4f, 1.0f);
		shootingStar(f, t, 4.5f, 0.7f, sx, sy, sr + 2, 1f, 0.6f, 0.95f);
		for (int py = 0; py < FH; py += 3) for (int px = 0; px < FW; px++) {
			int i = py * FW + px;
			f.r[i] *= 0.88f;
			f.g[i] *= 0.88f;
			f.b[i] *= 0.88f;
		}
	}

	// =================================================================== shared pieces

	private static final class Star {
		float x, y, size, phase, speed;
	}

	private static Star[] stars(long seed, int n) {
		Random rnd = new Random(seed);
		Star[] out = new Star[n];
		for (int i = 0; i < n; i++) {
			Star s = new Star();
			s.x = rnd.nextFloat();
			s.y = rnd.nextFloat();
			s.size = 0.35f + (float) Math.pow(rnd.nextFloat(), 3) * 0.9f;
			s.phase = rnd.nextFloat();
			s.speed = 0.6f + rnd.nextFloat() * 2.2f;
			out[i] = s;
		}
		return out;
	}

	/** Twinkling stars (skipped inside the circle at cx, cy); the biggest get a cross glint. */
	private static void drawStars(F f, Star[] stars, float t, float cx, float cy, float hole, float sr, float sg, float sb, float drift) {
		for (Star s : stars) {
			// Nearer (bigger) stars drift faster.
			float x = ((s.x * UW + t * drift * (0.4f + s.size)) % (UW + 6) + UW + 6) % (UW + 6) - 3, y = s.y * UH;
			if (Math.hypot(x - cx, y - cy) < hole) continue;
			float tw = 0.35f + 0.65f * (float) Math.pow(0.5 + 0.5 * Math.sin(t * s.speed * 2 + s.phase * 40), 2);
			float bright = tw * s.size * 1.6f;
			glow(f, x, y, 0.35f + s.size * 0.45f, bright, sr, sg, sb);
			if (s.size > 0.9f) spikes(f, x, y, 3 + 5 * tw, bright * 0.55f, sr, sg, sb, 0);
		}
	}

	/** Faint pixel-sized background stars from a hash, twinkling; {@code seed} picks the pattern. */
	private static float dust(int px, int py, float t, int seed) {
		return starAt(px, py, t, seed);
	}

	private static float starAt(int px, int py, float t, int seed) {
		float h = hash(px * 7 + seed * 131, py * 13 - seed * 71);
		if (h < 0.992f) return 0;
		float k = (h - 0.992f) / 0.008f;
		return k * k * (0.35f + 0.25f * (float) Math.sin(t * (1 + k * 3) + h * 900));
	}

	/** A four-point star of light at (x, y) with arms {@code len} units long, rotated by {@code rot}. */
	private static void spikes(F f, float x, float y, float len, float bright, float cr, float cg, float cb, float rot) {
		glow(f, x, y, 1.1f, bright, cr, cg, cb);
		for (int arm = 0; arm < 4; arm++) {
			float a = rot + arm * (float) (Math.PI / 2);
			float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
			int steps = (int) (len * S);
			for (int k = 1; k < steps; k++) {
				float p = k / (float) steps, fall = (1 - p) * (1 - p) * bright;
				int px = Math.round((x + dx * k / S) * S - 0.5f), py = Math.round((y + dy * k / S) * S - 0.5f);
				f.add(px, py, fall * cr, fall * cg, fall * cb);
			}
		}
	}

	private static void shootingStar(F f, float t, float period, float dur, float cx, float cy, float hole, float sr, float sg, float sb) {
		int n = (int) (t / period);
		float p = (t - n * period) / dur;
		if (p > 1) return;
		Random rnd = new Random(n * 7919L);
		float sx = UW * (0.6f + rnd.nextFloat() * 0.5f), sy = UH * (0.05f + rnd.nextFloat() * 0.45f);
		float hx = sx - p * 70, hy = sy + p * 38;
		float fade = (float) Math.sin(Math.PI * p);
		for (int k = 0; k < 52; k++) {
			float x = hx + k * 0.83f, y = hy - k * 0.45f;
			if (Math.hypot(x - cx, y - cy) < hole) continue;
			glow(f, x, y, k == 0 ? 0.8f : 0.45f, fade * (1 - k / 52f) * (k == 0 ? 2.2f : 1.1f), sr, sg, sb);
		}
	}

	/** A line of light from (x0, y0) to (x1, y1), {@code w} units thick. */
	private static void line(F f, float x0, float y0, float x1, float y1, float w, float bright, float cr, float cg, float cb) {
		float len = (float) Math.hypot(x1 - x0, y1 - y0);
		int steps = Math.max(1, (int) (len * S * 1.5f));
		for (int k = 0; k <= steps; k++) {
			float p = k / (float) steps;
			glow(f, x0 + (x1 - x0) * p, y0 + (y1 - y0) * p, w, bright / (S * 1.5f) * 2.2f, cr, cg, cb);
		}
	}

	/** A soft round light centred at (x, y), radius in units. */
	private static void glow(F f, float x, float y, float radius, float bright, float cr, float cg, float cb) {
		float rp = radius * S;
		float cxp = x * S, cyp = y * S;
		int x0 = (int) Math.floor(cxp - rp * 2.5f), x1 = (int) Math.ceil(cxp + rp * 2.5f);
		int y0 = (int) Math.floor(cyp - rp * 2.5f), y1 = (int) Math.ceil(cyp + rp * 2.5f);
		for (int py = y0; py <= y1; py++) {
			for (int px = x0; px <= x1; px++) {
				float dx = px + 0.5f - cxp, dy = py + 0.5f - cyp;
				float k = bright * (float) Math.exp(-(dx * dx + dy * dy) / (rp * rp));
				if (k > 0.004f) f.add(px, py, k * cr, k * cg, k * cb);
			}
		}
	}

	/** A hem along the bottom with colours flowing across and a light running along it. */
	private static void hem(int[] img, int a, int c, float t) {
		float run = (t * 0.35f) % 1f;
		for (int y = FH - (int) (2 * S); y < FH; y++) {
			for (int x = 0; x < FW; x++) {
				float u = x / (float) FW;
				float d = Math.abs(u - run);
				d = Math.min(d, 1 - d);
				int col = lerp(a, c, 0.5f + 0.5f * (float) Math.sin((u + t * 0.2f) * Math.PI * 2));
				col = lerp(col, 0xFFFFFFFF, (float) Math.exp(-d * d * 300) * 0.8f);
				img[(OY + y) * W + OX + x] = col;
			}
		}
	}

	private static int rgb(float r, float g, float b) {
		return 0xFF000000 | tone(r) << 16 | tone(g) << 8 | tone(b);
	}

	/** Soft clamp: bright light rolls off to white instead of clipping. */
	private static int tone(float v) {
		v = v < 0 ? 0 : v;
		float m = v < 0.8f ? v : 0.8f + 0.2f * (1 - (float) Math.exp(-(v - 0.8f) * 3));
		return Math.min(255, Math.round((float) Math.pow(m, 1 / 1.15) * 255));
	}

	private static int shade(int c, float k) {
		int r = Math.min(255, Math.round(((c >> 16) & 255) * k));
		int g = Math.min(255, Math.round(((c >> 8) & 255) * k));
		int b = Math.min(255, Math.round((c & 255) * k));
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	private static int lerp(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}

	private static float sq(float v) {
		return v * v;
	}

	private static float mix(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static float smooth(float e0, float e1, float x) {
		float t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
		return t * t * (3 - 2 * t);
	}

	private static float angleDiff(float a, float b) {
		float d = (a - b) % (float) (Math.PI * 2);
		if (d > Math.PI) d -= Math.PI * 2;
		if (d < -Math.PI) d += Math.PI * 2;
		return d;
	}

	/** Well-mixed 3D hash (murmur3 finaliser) in 0..1. */
	private static float hash3(int x, int y, int z) {
		int h = x * 0x8DA6B343 ^ y * 0xD8163841 ^ z * 0xCB1AB31F;
		h ^= h >>> 16;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	private static float hash(int x, int y) {
		int h = x * 374761393 + y * 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	private static float noise(float x, float y) {
		int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
		float xf = x - xi, yf = y - yi;
		float u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf);
		float a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
		return mix(mix(a, b, u), mix(c, d, u), v);
	}

	private static float fbm(float x, float y, int octaves) {
		float sum = 0, amp = 0.5f, norm = 0;
		for (int o = 0; o < octaves; o++) {
			sum += noise(x, y) * amp;
			norm += amp;
			amp *= 0.5f;
			x = x * 2.03f + 17.1f;
			y = y * 2.03f - 9.7f;
		}
		return sum / norm;
	}
}
