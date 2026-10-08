package com.tatnat.client.ui.render;

import com.tatnat.client.ui.theme.Colors;

import com.tatnat.client.platform.Gfx;

/**
 * Line-art icons drawn in code (no textures), in the style of Feather's mod cards.
 *
 * Every icon is described on a 64x64 grid and scaled to the requested size. Strokes are
 * anti-aliased along their horizontal edges: each pixel row of a thick line is one solid span
 * plus two partially transparent end pixels, so a whole icon is a few hundred tiny fills.
 */
public final class Icons {
	private Icons() {
	}

	/** Icon names, used by modules to pick their card icon. */
	public enum Icon {
		KEYBOARD, MONITOR, MOUSE, MAP, ARMOR, CLOCK, CHIP, SIGNAL, GLOBE, CUBE, SUN, ZOOM, RUN,
		COMBO, RULER, POTION, CROSSHAIR, EYE, DROP, SWORD, BOX, MASK, MOON, LAYERS, CHAT, KEY, TOOLTIP, SCROLL, CAMERA, PIN,
		CAPE, SPARKLE, USER, COMPASS, GAUGE, HOURGLASS, STOPWATCH, TNT, FLAG, REFRESH, CLOUD, BEAM,
		GRID, GEAR, MOVE, HEART, HEART_FILLED, SEARCH, BACK, FORWARD, CHEVRON_DOWN, LIST, YOUTUBE
	}

	private static Gfx g;
	/** Rows a stroke may cover (pixel space); thickLine limits it to the screen. */
	private static int rowMin = Integer.MIN_VALUE / 2, rowMax = Integer.MAX_VALUE / 2;
	private static float ox, oy, k;
	private static int color;

	/** A rasterised icon: its coverage grid (dim x dim, centred) and the same as merged spans. */
	private static final class Raster {
		final int half, dim;
		final float[] cov;
		/** {dx, dy, dx2, alpha} spans relative to the centre. */
		final int[] spans;
		byte[] alpha;

		Raster(int half, int dim, float[] cov, int[] spans) {
			this.half = half;
			this.dim = dim;
			this.cov = cov;
			this.spans = spans;
		}

		byte[] alpha() {
			if (alpha == null) {
				alpha = new byte[cov.length];
				for (int i = 0; i < cov.length; i++) alpha[i] = (byte) Math.round(Math.min(1f, cov[i]) * 255);
			}
			return alpha;
		}
	}

	private static final java.util.Map<Long, Raster> CACHE = new java.util.HashMap<>();

	/**
	 * Draws {@code icon} centred on (cx, cy), {@code size} pixels across, in pixel space.
	 * The stroke geometry is rasterised once per size into merged spans and replayed from a cache:
	 * drawn live, overlapping strokes cost hundreds to thousands of tiny fills per icon per frame.
	 */
	public static void draw(Gfx graphics, Icon icon, int cx, int cy, int size, int argb) {
		if (icon == Icon.MONITOR || icon == Icon.CHAT || icon == Icon.YOUTUBE) {
			// Text or a second colour, which a one-colour coverage raster cannot hold.
			drawLive(graphics, icon, cx, cy, size, argb);
			return;
		}
		long key = ((long) icon.ordinal() << 20) | ((long) size << 1) | (com.tatnat.client.modules.Performance.roundedCorners() ? 1 : 0);
		Raster r = CACHE.get(key);
		if (r == null) {
			r = rasterise(icon, size);
			CACHE.put(key, r);
		}
		if (graphics.masks()) {
			// One textured quad for the whole icon.
			Raster raster = r;
			graphics.mask("icon/" + key, r.dim, r.dim, raster::alpha, cx - r.half, cy - r.half, Colors.fade(argb, RenderUtils.alpha));
			return;
		}
		int[] spans = r.spans;
		int baseA = argb >>> 24, rgb = argb & 0xFFFFFF;
		for (int i = 0; i < spans.length; i += 4) {
			int a = spans[i + 3] * baseA / 255;
			if (a > 0) RenderUtils.rect(graphics, cx + spans[i], cy + spans[i + 1], cx + spans[i + 2], cy + spans[i + 1] + 1, (a << 24) | rgb);
		}
	}

	/** Draws the icon at (0, 0) into a coverage grid, then encodes each row as runs of equal alpha. */
	private static Raster rasterise(Icon icon, int size) {
		int half = size + 2, dim = half * 2;
		// Drawn at 4x and averaged down: proper supersampled anti-aliasing on every curve and diagonal.
		final int ss = 4;
		int hHalf = half * ss, hDim = dim * ss;
		float[] hi = new float[hDim * hDim];
		float[] cov = new float[dim * dim];
		Gfx rec = new Gfx() {
			@Override
			public void rect(int x1, int y1, int x2, int y2, int argb) {
				float a = (argb >>> 24) / 255f;
				for (int y = Math.max(y1 + hHalf, 0); y < Math.min(y2 + hHalf, hDim); y++) {
					for (int x = Math.max(x1 + hHalf, 0); x < Math.min(x2 + hHalf, hDim); x++) {
						int i = y * hDim + x;
						hi[i] = hi[i] + a * (1 - hi[i]);
					}
				}
			}

			@Override
			public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
				rect(x1, y1, x2, y2, top);
			}

			@Override
			public void push() {
			}

			@Override
			public void pop() {
			}

			@Override
			public void translate(float x, float y) {
			}

			@Override
			public void scale(float x, float y) {
			}

			@Override
			public void scissor(int x1, int y1, int x2, int y2) {
			}

			@Override
			public void endScissor() {
			}

			@Override
			public void uiText(int weight, int px, String s, int x, int y, int argb) {
			}

			@Override
			public int uiTextWidth(int weight, int px, String s) {
				return 0;
			}

			@Override
			public void mcText(String s, int x, int y, int argb, boolean shadow, boolean bold) {
			}

			@Override
			public int mcTextWidth(String s, boolean bold) {
				return 0;
			}

			@Override
			public void logo(int x, int y, int sz, int argb) {
			}

			@Override
			public void item(Object stack, int x, int y) {
			}

			@Override
			public void effectIcon(Object effect, int x, int y, int sz) {
			}
		};
		float fade = RenderUtils.alpha;
		RenderUtils.alpha = 1f;
		try {
			drawLive(rec, icon, 0, 0, size * ss, 0xFFFFFFFF);
		} finally {
			RenderUtils.alpha = fade;
		}
		for (int y = 0; y < dim; y++) {
			for (int x = 0; x < dim; x++) {
				float sum = 0;
				for (int sy = 0; sy < ss; sy++) for (int sx = 0; sx < ss; sx++) sum += hi[(y * ss + sy) * hDim + x * ss + sx];
				cov[y * dim + x] = sum / (ss * ss);
			}
		}
		java.util.List<int[]> out = new java.util.ArrayList<>();
		for (int y = 0; y < dim; y++) {
			int x = 0;
			while (x < dim) {
				// 32 alpha levels: smooth edges, while flat runs still merge into one span.
				int q = Math.round(cov[y * dim + x] * 31);
				int end = x + 1;
				while (end < dim && Math.round(cov[y * dim + end] * 31) == q) end++;
				if (q > 0) out.add(new int[] {x - half, y - half, end - half, Math.min(255, Math.round(q * 255f / 31))});
				x = end;
			}
		}
		int[] spans = new int[out.size() * 4];
		for (int i = 0; i < out.size(); i++) System.arraycopy(out.get(i), 0, spans, i * 4, 4);
		return new Raster(half, dim, cov, spans);
	}

	private static void drawLive(Gfx graphics, Icon icon, int cx, int cy, int size, int argb) {
		g = graphics;
		k = size / 64f;
		ox = cx - size / 2f;
		oy = cy - size / 2f;
		color = argb;
		float w = 4.2f; // default stroke width on the 64 grid
		switch (icon) {
			case KEYBOARD: {
				// W over A S D
				box(22, 6, 20, 20, 2, w);
				box(1, 32, 20, 20, 2, w);
				box(22, 32, 20, 20, 2, w);
				box(43, 32, 20, 20, 2, w);
			}
			break;
			case MONITOR: {
				box(4, 8, 56, 38, 4, w);
				line(32, 46, 32, 55, w);
				line(20, 56, 44, 56, w);
				text("FPS", 32, 27, 15);
			}
			break;
			case MOUSE: {
				box(16, 4, 32, 56, 15, w);
				line(32, 6, 32, 24, w);
				line(17, 26, 47, 26, w);
			}
			break;
			case MAP: {
				poly(w, 4, 14, 22, 6, 42, 14, 60, 6, 60, 50, 42, 58, 22, 50, 4, 58, 4, 14);
				line(22, 6, 22, 50, w);
				line(42, 14, 42, 58, w);
			}
			break;
			case ARMOR: {
				// A shield: protection at a glance.
				poly(w, 32, 4, 56, 12, 55, 34, 46, 49, 32, 60, 18, 49, 9, 34, 8, 12, 32, 4);
				line(32, 14, 32, 49, 3.4f);
				line(18, 27, 46, 27, 3.4f);
			}
			break;
			case CLOCK: {
				ring(32, 32, 27, w);
				line(32, 32, 32, 15, w);
				line(32, 32, 44, 39, w);
			}
			break;
			case CHIP: {
				box(14, 14, 36, 36, 4, w);
				box(24, 24, 16, 16, 2, 3f);
				for (int i = 0; i < 3; i++) {
					float p = 22 + i * 10;
					line(p, 4, p, 13, 3.4f);
					line(p, 51, p, 60, 3.4f);
					line(4, p, 13, p, 3.4f);
					line(51, p, 60, p, 3.4f);
				}
			}
			break;
			case SIGNAL: {
				for (int i = 0; i < 4; i++) fillRect(6 + i * 14, 50 - i * 12, 10, 8 + i * 12);
			}
			break;
			case GLOBE: {
				ring(32, 32, 27, w);
				line(6, 32, 58, 32, 3.4f);
				ellipse(32, 32, 11, 27, 3.4f);
				line(10, 19, 54, 19, 3f);
				line(10, 45, 54, 45, 3f);
			}
			break;
			case CUBE: {
				poly(w, 32, 4, 58, 17, 58, 46, 32, 60, 6, 46, 6, 17, 32, 4);
				poly(w, 6, 17, 32, 31, 58, 17);
				line(32, 31, 32, 60, w);
			}
			break;
			case SUN: {
				ring(32, 32, 12, w);
				for (int i = 0; i < 8; i++) {
					double a = Math.PI / 4 * i;
					line(32 + (float) Math.cos(a) * 20, 32 + (float) Math.sin(a) * 20, 32 + (float) Math.cos(a) * 28, 32 + (float) Math.sin(a) * 28, w);
				}
			}
			break;
			case ZOOM: {
				ring(27, 27, 19, w);
				line(41, 41, 58, 58, 6.5f);
				line(27, 19, 27, 35, 3.6f);
				line(19, 27, 35, 27, 3.6f);
			}
			break;
			case RUN: {
				// A running figure.
				disc(42, 9, 6);
				line(39, 18, 30, 36, w);
				poly(w, 37, 22, 48, 28, 54, 22);
				poly(w, 37, 22, 26, 22, 18, 30);
				poly(w, 30, 36, 40, 45, 37, 58);
				poly(w, 30, 36, 20, 46, 8, 45);
			}
			break;
			case COMBO: {
				// A lightning bolt: hit after hit.
				poly(w, 38, 4, 14, 36, 30, 36, 24, 60, 50, 26, 34, 26, 38, 4);
			}
			break;
			case RULER: {
				box(2, 18, 60, 28, 3, w);
				for (int i = 0; i < 6; i++) line(11 + i * 8.4f, 18, 11 + i * 8.4f, i % 2 == 0 ? 32 : 26, 3.4f);
			}
			break;
			case POTION: {
				poly(w, 26, 6, 38, 6);
				poly(w, 28, 6, 28, 22, 12, 46, 14, 58, 50, 58, 52, 46, 36, 22, 36, 6);
				line(16, 44, 48, 44, 3.4f);
			}
			break;
			case CROSSHAIR: {
				ring(32, 32, 18, w);
				line(32, 2, 32, 22, w);
				line(32, 42, 32, 62, w);
				line(2, 32, 22, 32, w);
				line(42, 32, 62, 32, w);
			}
			break;
			case EYE: {
				ellipse(32, 32, 27, 15, w);
				ring(32, 32, 8, w);
			}
			break;
			case DROP: {
				poly(w, 32, 4, 48, 30, 50, 40, 44, 52, 32, 58, 20, 52, 14, 40, 16, 30, 32, 4);
				line(24, 40, 28, 48, 3.2f);
			}
			break;
			case SWORD: {
				line(12, 52, 50, 14, w + 1);
				poly(w, 44, 10, 54, 10, 54, 20);
				line(8, 40, 24, 56, w + 1);
				line(8, 56, 14, 50, w + 1);
			}
			break;
			case BOX: {
				// A figure inside its hitbox.
				box(12, 2, 40, 60, 1, 3.4f);
				ring(32, 15, 6, 3.6f);
				line(32, 22, 32, 40, 3.6f);
				line(22, 28, 42, 28, 3.6f);
				line(32, 40, 25, 54, 3.6f);
				line(32, 40, 39, 54, 3.6f);
			}
			break;
			case MASK: {
				poly(w, 4, 22, 60, 22, 56, 38, 44, 44, 36, 36, 28, 36, 20, 44, 8, 38, 4, 22);
				fillRect(14, 28, 10, 6);
				fillRect(40, 28, 10, 6);
			}
			break;
			case MOON: crescent(30, 34, 25, 42, 24, 21, w); break;
			case LAYERS: {
				poly(w, 32, 6, 58, 18, 32, 30, 6, 18, 32, 6);
				poly(w, 6, 30, 32, 42, 58, 30);
				poly(w, 6, 42, 32, 54, 58, 42);
			}
			break;
			case CHAT: {
				box(4, 8, 56, 36, 8, w);
				poly(w, 16, 44, 14, 56, 28, 44);
				text("gg", 32, 25, 22);
			}
			break;
			case KEY: {
				box(4, 14, 56, 36, 6, w);
				for (int i = 0; i < 4; i++) fillRect(11 + i * 12, 21, 7, 6);
				line(18, 40, 46, 40, w);
			}
			break;
			case TOOLTIP: {
				box(4, 6, 56, 46, 4, w);
				line(12, 18, 44, 18, w);
				line(12, 29, 52, 29, 3.4f);
				line(12, 40, 36, 40, 3.4f);
			}
			break;
			case SCROLL: {
				box(10, 4, 34, 56, 4, w);
				line(52, 8, 52, 56, 3f);
				fillRect(50, 18, 5, 16);
				line(18, 18, 36, 18, 3.4f);
				line(18, 30, 36, 30, 3.4f);
				line(18, 42, 30, 42, 3.4f);
			}
			break;
			case CAMERA: {
				box(4, 16, 56, 38, 6, w);
				poly(w, 20, 16, 24, 8, 40, 8, 44, 16);
				ring(32, 35, 11, w);
			}
			break;
			case PIN: {
				poly(w, 32, 60, 14, 32, 12, 22, 18, 10, 32, 4, 46, 10, 52, 22, 50, 32, 32, 60);
				ring(32, 24, 7, w);
			}
			break;
			case USER: {
				ring(32, 21, 12, w);
				arc(32, 62, 24, 0.5, 1.0, w);
			}
			break;
			case COMPASS: {
				ring(32, 32, 27, w);
				poly(w, 32, 12, 39, 32, 32, 52, 25, 32, 32, 12);
				disc(32, 32, 3.5f);
			}
			break;
			case GAUGE: {
				arc(32, 40, 26, 0.5, 1.0, w);
				line(32, 40, 47, 24, w);
				disc(32, 40, 4.5f);
			}
			break;
			case HOURGLASS: {
				line(14, 6, 50, 6, w);
				line(14, 58, 50, 58, w);
				poly(w, 18, 6, 46, 6, 34, 32, 46, 58, 18, 58, 30, 32, 18, 6);
				fillRect(24, 48, 16, 6);
			}
			break;
			case STOPWATCH: {
				ring(32, 36, 22, w);
				line(32, 36, 32, 23, w);
				line(26, 6, 38, 6, w);
				line(32, 6, 32, 14, w);
				line(49, 17, 54, 12, w);
			}
			break;
			case TNT: {
				box(8, 18, 48, 40, 4, w);
				line(8, 30, 56, 30, w);
				line(8, 46, 56, 46, w);
				line(32, 18, 34, 8, w);
				disc(36, 5, 3.5f);
			}
			break;
			case FLAG: {
				line(16, 6, 16, 58, w);
				poly(w, 16, 9, 50, 17, 16, 31);
			}
			break;
			case REFRESH: {
				arc(32, 32, 22, 0.05, 0.88, w);
				poly(w, 48, 9, 55, 24, 40, 24, 48, 9);
			}
			break;
			case CLOUD: {
				arc(22, 40, 12, 0.25, 0.75, w);
				arc(33, 30, 14, 0.5, 1.0, w);
				arc(45, 39, 11, 0.72, 1.25, w);
				line(22, 52, 45, 52, w);
			}
			break;
			case BEAM: {
				line(32, 6, 32, 40, w * 1.4f);
				line(22, 14, 22, 36, w * 0.7f);
				line(42, 14, 42, 36, w * 0.7f);
				disc(32, 50, 7f);
			}
			break;
			case CAPE: {
				poly(w, 18, 6, 46, 6, 54, 58, 40, 54, 32, 58, 24, 54, 10, 58, 18, 6);
				line(18, 6, 12, 14, w);
				line(46, 6, 52, 14, w);
			}
			break;
			case SPARKLE: {
				poly(w, 26, 6, 31, 25, 48, 30, 31, 35, 26, 56, 21, 35, 4, 30, 21, 25, 26, 6);
				poly(3.4f, 50, 6, 52, 13, 59, 15, 52, 17, 50, 24, 48, 17, 41, 15, 48, 13, 50, 6);
			}
			break;
			case GRID: {
				for (int i = 0; i < 4; i++) box(6 + (i % 2) * 28, 6 + (i / 2) * 28, 22, 22, 4, w);
			}
			break;
			case GEAR: {
				ring(32, 32, 9, w);
				ring(32, 32, 19, w);
				for (int i = 0; i < 8; i++) {
					double a = Math.PI / 4 * i;
					line(32 + (float) Math.cos(a) * 19, 32 + (float) Math.sin(a) * 19, 32 + (float) Math.cos(a) * 28, 32 + (float) Math.sin(a) * 28, 7f);
				}
			}
			break;
			case MOVE: {
				line(32, 6, 32, 58, w);
				line(6, 32, 58, 32, w);
				poly(w, 24, 14, 32, 6, 40, 14);
				poly(w, 24, 50, 32, 58, 40, 50);
				poly(w, 14, 24, 6, 32, 14, 40);
				poly(w, 50, 24, 58, 32, 50, 40);
			}
			break;
			case HEART: arcHeart(w); break;
			case HEART_FILLED: {
				fillPoly(heartPoints());
				arcHeart(2f);
			}
			break;
			case SEARCH: {
				ring(27, 27, 18, w + 1);
				line(40, 40, 57, 57, w + 2);
			}
			break;
			case BACK: poly(w + 2, 40, 10, 18, 32, 40, 54); break;
			case FORWARD: poly(w + 2, 24, 10, 46, 32, 24, 54); break;
			case CHEVRON_DOWN: poly(w + 2, 10, 22, 32, 44, 54, 22); break;
			case LIST: {
				for (int i = 0; i < 3; i++) {
					fillRect(6, 10 + i * 18, 8, 8);
					line(22, 14 + i * 18, 58, 14 + i * 18, w);
				}
			}
			break;
			case YOUTUBE: {
				fillRoundRect(2, 12, 60, 40, 12);
				int save = color;
				color = 0xFF131315;
				for (int y = 21; y < 43; y++) {
					float half = (11 - Math.abs(y - 32)) * 1.3f;
					if (half > 0) span(26, 26 + half * 1.6f, y);
				}
				color = save;
			}
			break;
		}
		g = null;
	}

	// ------------------------------------------------------------ primitives (64-grid units)

	private static void line(float x0, float y0, float x1, float y1, float width) {
		capsule(ox + x0 * k, oy + y0 * k, ox + x1 * k, oy + y1 * k, width * k / 2f);
	}

	private static void poly(float width, float... pts) {
		for (int i = 0; i + 3 < pts.length; i += 2) line(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], width);
	}

	private static void box(float x, float y, float w, float h, float r, float width) {
		int t = Math.max(1, Math.round(width * k));
		// Corner radius measured to the stroke centre, so small boxes stay boxes instead of circles.
		RenderUtils.roundedOutline(g, Math.round(ox + x * k), Math.round(oy + y * k), Math.round(w * k), Math.round(h * k), Math.round(r * k + t / 2f), t, color);
	}

	/** Crescent: the part of circle 1 outside circle 2, outlined. */
	private static void crescent(float x1, float y1, float r1, float x2, float y2, float r2, float width) {
		double dx = x2 - x1, dy = y2 - y1, d = Math.hypot(dx, dy);
		double a = (r1 * r1 - r2 * r2 + d * d) / (2 * d), h = Math.sqrt(Math.max(0, r1 * r1 - a * a));
		double mx = x1 + a * dx / d, my = y1 + a * dy / d;
		double px = mx - h * dy / d, py = my + h * dx / d, qx = mx + h * dy / d, qy = my - h * dx / d;
		double p1 = Math.atan2(py - y1, px - x1), q1 = Math.atan2(qy - y1, qx - x1);
		double p2 = Math.atan2(py - y2, px - x2), q2 = Math.atan2(qy - y2, qx - x2);
		// Outer arc: the long way round circle 1 (away from circle 2); inner arc: inside circle 1.
		double outer = q1 - p1;
		while (outer <= 0) outer += 2 * Math.PI;
		if (outer < Math.PI) outer -= 2 * Math.PI;
		double inner = q2 - p2;
		while (inner <= 0) inner += 2 * Math.PI;
		if (inner > Math.PI) inner -= 2 * Math.PI;
		int steps = 28;
		for (int i = 0; i < steps; i++) {
			double t0 = p1 + outer * i / steps, t1 = p1 + outer * (i + 1) / steps;
			line(x1 + (float) (Math.cos(t0) * r1), y1 + (float) (Math.sin(t0) * r1), x1 + (float) (Math.cos(t1) * r1), y1 + (float) (Math.sin(t1) * r1), width);
			double u0 = p2 + inner * i / steps, u1 = p2 + inner * (i + 1) / steps;
			line(x2 + (float) (Math.cos(u0) * r2), y2 + (float) (Math.sin(u0) * r2), x2 + (float) (Math.cos(u1) * r2), y2 + (float) (Math.sin(u1) * r2), width);
		}
	}

	private static void ring(float cx, float cy, float r, float width) {
		int t = Math.max(1, Math.round(width * k));
		int rr = Math.round(r * k + t / 2f);
		RenderUtils.roundedOutline(g, Math.round(ox + cx * k) - rr, Math.round(oy + cy * k) - rr, rr * 2, rr * 2, rr, t, color);
	}

	private static void ellipse(float cx, float cy, float rx, float ry, float width) {
		int steps = 28;
		for (int i = 0; i < steps; i++) {
			double a0 = 2 * Math.PI * i / steps, a1 = 2 * Math.PI * (i + 1) / steps;
			line(cx + (float) Math.cos(a0) * rx, cy + (float) Math.sin(a0) * ry, cx + (float) Math.cos(a1) * rx, cy + (float) Math.sin(a1) * ry, width);
		}
	}

	private static void arcHeart(float width) {
		poly(width, heartPoints());
	}

	/** The classic parametric heart curve as a closed polyline on the 64 grid. */
	private static float[] heartPoints() {
		int steps = 48;
		float[] pts = new float[(steps + 1) * 2];
		for (int i = 0; i <= steps; i++) {
			double t = 2 * Math.PI * i / steps;
			double hx = 16 * Math.pow(Math.sin(t), 3);
			double hy = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
			pts[i * 2] = 32 + (float) hx * 1.7f;
			pts[i * 2 + 1] = 30 - (float) hy * 1.7f;
		}
		return pts;
	}

	/** Even-odd scanline fill of a closed polygon (grid units), one span per pixel row. */
	private static void fillPoly(float[] pts) {
		int n = pts.length / 2;
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			minY = Math.min(minY, oy + pts[i * 2 + 1] * k);
			maxY = Math.max(maxY, oy + pts[i * 2 + 1] * k);
		}
		float[] xs = new float[n];
		for (int row = (int) Math.floor(minY); row < Math.ceil(maxY); row++) {
			float yc = row + 0.5f;
			int c = 0;
			for (int i = 0; i < n; i++) {
				int j = (i + 1) % n;
				float ax = ox + pts[i * 2] * k, ay = oy + pts[i * 2 + 1] * k, bx = ox + pts[j * 2] * k, by = oy + pts[j * 2 + 1] * k;
				if ((ay <= yc && by > yc) || (by <= yc && ay > yc)) xs[c++] = ax + (yc - ay) / (by - ay) * (bx - ax);
			}
			java.util.Arrays.sort(xs, 0, c);
			for (int i = 0; i + 1 < c; i += 2) hspan(xs[i], xs[i + 1], row, row + 1);
		}
	}

	/** Partial ring from {@code a0} to {@code a1} (in turns, 0 = +x, clockwise on screen). */
	private static void arc(float cx, float cy, float r, double a0, double a1, float width) {
		int steps = 24;
		for (int i = 0; i < steps; i++) {
			double t0 = 2 * Math.PI * (a0 + (a1 - a0) * i / steps), t1 = 2 * Math.PI * (a0 + (a1 - a0) * (i + 1) / steps);
			line(cx + (float) Math.cos(t0) * r, cy + (float) Math.sin(t0) * r, cx + (float) Math.cos(t1) * r, cy + (float) Math.sin(t1) * r, width);
		}
	}

	/** A thick anti-aliased line between two points, in pixel space. */
	public static void thickLine(Gfx graphics, float x1, float y1, float x2, float y2, float width, int argb) {
		g = graphics;
		color = argb;
		ox = 0;
		oy = 0;
		k = 1;
		rowMin = -64;
		rowMax = com.tatnat.client.TatnatClient.game().windowHeight() + 64;
		try {
			capsule(x1, y1, x2, y2, width / 2f);
		} finally {
			rowMin = Integer.MIN_VALUE / 2;
			rowMax = Integer.MAX_VALUE / 2;
		}
		g = null;
	}

	/** A filled arrow head at (x, y) pointing at {@code angle} radians, in pixel space. */
	public static void arrow(Gfx graphics, float x, float y, float angle, float size, int argb) {
		g = graphics;
		color = argb;
		ox = 0;
		oy = 0;
		k = 1;
		float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
		float[] tri = {x + c * size, y + s * size, x - c * size * 0.6f - s * size * 0.7f, y - s * size * 0.6f + c * size * 0.7f,
				x - c * size * 0.6f + s * size * 0.7f, y - s * size * 0.6f - c * size * 0.7f};
		int save = color;
		color = 0xA0000000;
		fillPoly(new float[] {tri[0] + 1, tri[1] + 1, tri[2] + 1, tri[3] + 1, tri[4] + 1, tri[5] + 1});
		color = save;
		fillPoly(tri);
		g = null;
	}

	private static void disc(float cx, float cy, float r) {
		int rr = Math.round(r * k);
		RenderUtils.circle(g, Math.round(ox + cx * k), Math.round(oy + cy * k), rr, color);
	}

	private static void fillRect(float x, float y, float w, float h) {
		RenderUtils.rect(g, Math.round(ox + x * k), Math.round(oy + y * k), Math.round(ox + (x + w) * k), Math.round(oy + (y + h) * k), color);
	}

	private static void fillRoundRect(float x, float y, float w, float h, float r) {
		RenderUtils.roundedRect(g, Math.round(ox + x * k), Math.round(oy + y * k), Math.round(w * k), Math.round(h * k), Math.round(r * k), color);
	}

	/** Horizontal span on the 64 grid at grid row y (used for filled shapes). */
	private static void span(float x0, float x1, float y) {
		int py0 = Math.round(oy + y * k), py1 = Math.round(oy + (y + 1) * k);
		if (py1 <= py0) return;
		hspan(ox + x0 * k, ox + x1 * k, py0, py1);
	}

	private static void text(String s, float cx, float cy, float size) {
		// Uses the menu font at the closest size; good enough for a 3-letter label.
		UIFont f = size * k >= 22 ? UIFont.HEADER : size * k >= 17 ? UIFont.TITLE : size * k >= 13 ? UIFont.SMALL : UIFont.TINY;
		f.drawCentered(g, s, Math.round(ox + cx * k), Math.round(oy + cy * k) - f.size() / 2, color);
	}

	/** A thick line with round caps, drawn as one horizontal span per pixel row. */
	private static void capsule(float ax, float ay, float bx, float by, float h) {
		float dx = bx - ax, dy = by - ay;
		float len2 = dx * dx + dy * dy;
		int top = (int) Math.floor(Math.min(ay, by) - h), bottom = (int) Math.ceil(Math.max(ay, by) + h);
		// Lines that run far off screen (a waypoint beam right next to the camera) only draw visible rows.
		top = Math.max(top, rowMin);
		bottom = Math.min(bottom, rowMax);
		for (int row = top; row < bottom; row++) {
			float yc = row + 0.5f;
			float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
			// Round caps.
			for (int e = 0; e < 2; e++) {
				float cx = e == 0 ? ax : bx, cy = e == 0 ? ay : by;
				float dd = h * h - (yc - cy) * (yc - cy);
				if (dd >= 0) {
					float r = (float) Math.sqrt(dd);
					lo = Math.min(lo, cx - r);
					hi = Math.max(hi, cx + r);
				}
			}
			// The straight body: within distance h of the line and between the two ends.
			if (len2 > 1e-6f) {
				float len = (float) Math.sqrt(len2);
				float sLo, sHi;
				if (Math.abs(dy) < 1e-6f) {
					if (Math.abs(yc - ay) <= h) {
						sLo = Math.min(ax, bx);
						sHi = Math.max(ax, bx);
					} else {
						sLo = Float.MAX_VALUE;
						sHi = -Float.MAX_VALUE;
					}
				} else {
					float c1 = ax + ((yc - ay) * dx - h * len) / dy, c2 = ax + ((yc - ay) * dx + h * len) / dy;
					sLo = Math.min(c1, c2);
					sHi = Math.max(c1, c2);
					if (Math.abs(dx) > 1e-6f) {
						float d1 = ax - (yc - ay) * dy / dx, d2 = ax + (len2 - (yc - ay) * dy) / dx;
						sLo = Math.max(sLo, Math.min(d1, d2));
						sHi = Math.min(sHi, Math.max(d1, d2));
					} else if ((yc - ay) * dy < 0 || (yc - ay) * dy > len2) {
						sLo = Float.MAX_VALUE;
						sHi = -Float.MAX_VALUE;
					}
				}
				if (sLo <= sHi) {
					lo = Math.min(lo, sLo);
					hi = Math.max(hi, sHi);
				}
			}
			if (lo <= hi) hspan(lo, hi, row, row + 1);
		}
	}

	/** Fills [x0, x1) on rows [y0, y1) with soft (partial-alpha) end pixels. */
	private static void hspan(float x0, float x1, int y0, int y1) {
		int a = (int) Math.ceil(x0), b = (int) Math.floor(x1);
		if (b > a) RenderUtils.rect(g, a, y0, b, y1, color);
		if (b >= a) {
			float left = a - x0, right = x1 - b;
			if (left > 0.02f) RenderUtils.rect(g, a - 1, y0, a, y1, Colors.fade(color, left));
			if (right > 0.02f) RenderUtils.rect(g, b, y0, b + 1, y1, Colors.fade(color, right));
		} else {
			// Narrower than one pixel.
			RenderUtils.rect(g, b, y0, b + 1, y1, Colors.fade(color, x1 - x0));
		}
	}
}
