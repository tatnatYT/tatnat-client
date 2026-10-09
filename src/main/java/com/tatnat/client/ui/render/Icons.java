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
		RAM, STACK, TOTEM, SKULL, PALETTE, WAVE, APPLE, BED, HORSESHOE, BURST, DAYNIGHT, TRASH, HEALTHBAR, CREEPER, ALERT, UTURN, SWAP, EYE_OFF, VIDEOCAM, BOSSBAR, BREAD, CAPTIONS, HAND, BULB, TITLE, EXPAND, CHEST, BUG, SPEEDLINES, WHEEL, SHIELD, DROPITEM, PLANE, TROPHY, SAVE, FILTER, LOCK, STAR, SPEAKER, BROADCAST, MEDAL, FOLDER, KEYSEARCH, SHULKER, ANIMATION, MIC,
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
			case RAM: {
				box(6, 20, 52, 22, 3, w);
				for (int i = 0; i < 4; i++) fillRect(12 + i * 11, 25, 7, 11);
				for (int i = 0; i < 6; i++) line(10 + i * 9, 42, 10 + i * 9, 48, 3f);
			}
			break;
			case STACK: {
				box(6, 34, 23, 22, 3, w);
				box(35, 34, 23, 22, 3, w);
				box(20, 8, 24, 22, 3, w);
			}
			break;
			case TOTEM: {
				box(19, 12, 26, 44, 6, w);
				fillRect(25, 22, 5, 5);
				fillRect(34, 22, 5, 5);
				line(26, 40, 38, 40, w);
				poly(w, 19, 28, 6, 22, 8, 40, 19, 40);
				poly(w, 45, 28, 58, 22, 56, 40, 45, 40);
			}
			break;
			case SKULL: {
				poly(w, 12, 30, 14, 14, 32, 6, 50, 14, 52, 30, 46, 40, 46, 54, 18, 54, 18, 40, 12, 30);
				disc(24, 30, 5.5f);
				disc(40, 30, 5.5f);
				line(28, 54, 28, 47, 3f);
				line(36, 54, 36, 47, 3f);
			}
			break;
			case PALETTE: {
				poly(w, 32, 6, 50, 10, 58, 26, 54, 42, 40, 44, 38, 52, 30, 58, 14, 52, 6, 36, 10, 18, 32, 6);
				disc(22, 22, 4f);
				disc(34, 16, 4f);
				disc(46, 24, 4f);
				disc(20, 38, 4f);
			}
			break;
			case WAVE: {
				line(6, 32, 58, 32, 2.4f);
				poly(w, 6, 32, 13, 32, 19, 12, 26, 52, 33, 18, 39, 46, 45, 26, 50, 32, 58, 32);
			}
			break;
			case APPLE: {
				poly(w, 32, 18, 22, 12, 10, 20, 10, 38, 20, 56, 32, 52, 44, 56, 54, 38, 54, 20, 42, 12, 32, 18);
				line(32, 18, 34, 6, w);
				poly(3.4f, 36, 11, 44, 6, 48, 11, 40, 14, 36, 11);
			}
			break;
			case BED: {
				line(6, 22, 6, 56, w);
				line(58, 38, 58, 56, w);
				box(6, 36, 52, 12, 2, w);
				fillRoundRect(11, 27, 14, 8, 3);
			}
			break;
			case HORSESHOE: {
				arc(32, 30, 18, 0.5, 1.0, w * 1.5f);
				line(14, 30, 16, 56, w * 1.5f);
				line(50, 30, 48, 56, w * 1.5f);
				disc(21, 20, 2f);
				disc(43, 20, 2f);
				disc(17, 38, 2f);
				disc(47, 38, 2f);
			}
			break;
			case BURST: {
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * 2 * i / 8;
					float r0 = 12, r1 = i % 2 == 0 ? 28 : 20;
					line(32 + (float) Math.cos(a) * r0, 32 + (float) Math.sin(a) * r0, 32 + (float) Math.cos(a) * r1, 32 + (float) Math.sin(a) * r1, w);
				}
				disc(32, 32, 6f);
			}
			break;
			case DAYNIGHT: {
				ring(20, 20, 8, w);
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * 2 * i / 8;
					line(20 + (float) Math.cos(a) * 12, 20 + (float) Math.sin(a) * 12, 20 + (float) Math.cos(a) * 16, 20 + (float) Math.sin(a) * 16, 3f);
				}
				crescent(42, 42, 15, 50, 35, 13, w);
			}
			break;
			case TRASH: {
				box(14, 18, 36, 40, 4, w);
				line(8, 14, 56, 14, w);
				poly(w, 26, 14, 26, 8, 38, 8, 38, 14);
				line(26, 26, 26, 48, 3f);
				line(38, 26, 38, 48, 3f);
			}
			break;
			case HEALTHBAR: {
				box(6, 24, 52, 16, 4, w);
				fillRoundRect(10, 28, 28, 8, 2);
			}
			break;
			case CREEPER: {
				box(8, 8, 48, 48, 4, w);
				fillRect(18, 20, 9, 9);
				fillRect(37, 20, 9, 9);
				fillRect(28, 30, 8, 10);
				fillRect(22, 36, 6, 12);
				fillRect(36, 36, 6, 12);
			}
			break;
			case ALERT: {
				poly(w, 32, 6, 58, 54, 6, 54, 32, 6);
				line(32, 22, 32, 38, w);
				disc(32, 46, 3f);
			}
			break;
			case UTURN: {
				line(18, 58, 18, 28, w);
				arc(31, 28, 13, 0.5, 1.0, w);
				line(44, 28, 44, 42, w);
				poly(w, 36, 34, 44, 44, 52, 34);
			}
			break;
			case SWAP: {
				line(10, 22, 52, 22, w);
				poly(w, 42, 12, 52, 22, 42, 32);
				line(54, 42, 12, 42, w);
				poly(w, 22, 32, 12, 42, 22, 52);
			}
			break;
			case EYE_OFF: {
				ellipse(32, 32, 27, 15, w);
				ring(32, 32, 8, w);
				line(10, 54, 54, 10, w);
			}
			break;
			case VIDEOCAM: {
				box(6, 18, 36, 28, 5, w);
				poly(w, 42, 27, 58, 18, 58, 46, 42, 37);
			}
			break;
			case BOSSBAR: {
				box(6, 34, 52, 12, 3, w);
				fillRect(9, 37, 26, 6);
				ring(32, 16, 8, 3.4f);
				disc(29, 15, 1.8f);
				disc(35, 15, 1.8f);
			}
			break;
			case BREAD: {
				poly(w, 14, 56, 14, 26, 10, 22, 10, 14, 16, 8, 48, 8, 54, 14, 54, 22, 50, 26, 50, 56, 14, 56);
			}
			break;
			case CAPTIONS: {
				box(6, 14, 52, 36, 6, w);
				arc(23, 32, 7, 0.15, 0.85, 3.4f);
				arc(41, 32, 7, 0.15, 0.85, 3.4f);
			}
			break;
			case HAND: {
				box(16, 30, 32, 28, 9, w);
				line(22, 30, 22, 12, w);
				line(30, 30, 30, 7, w);
				line(38, 30, 38, 9, w);
				line(46, 34, 46, 17, w);
				line(16, 42, 8, 32, w);
			}
			break;
			case BULB: {
				arc(32, 24, 16, 0.35, 1.15, w);
				line(22.6f, 36.9f, 25, 46, w);
				line(41.4f, 36.9f, 39, 46, w);
				line(24, 46, 40, 46, w);
				line(26, 52, 38, 52, w);
				line(29, 58, 35, 58, w);
			}
			break;
			case TITLE: {
				line(12, 12, 52, 12, w * 1.5f);
				line(32, 12, 32, 54, w * 1.5f);
			}
			break;
			case EXPAND: {
				poly(w, 8, 22, 8, 8, 22, 8);
				line(8, 8, 24, 24, w);
				poly(w, 42, 8, 56, 8, 56, 22);
				line(56, 8, 40, 24, w);
				poly(w, 8, 42, 8, 56, 22, 56);
				line(8, 56, 24, 40, w);
				poly(w, 56, 42, 56, 56, 42, 56);
				line(56, 56, 40, 40, w);
			}
			break;
			case CHEST: {
				box(6, 16, 52, 40, 4, w);
				line(6, 30, 58, 30, w);
				fillRect(28, 26, 8, 10);
			}
			break;
			case BUG: {
				ellipse(32, 37, 13, 18, w);
				line(32, 24, 32, 54, 3f);
				line(19, 31, 8, 25, w);
				line(19, 41, 8, 44, w);
				line(45, 31, 56, 25, w);
				line(45, 41, 56, 44, w);
				disc(32, 16, 6.5f);
				line(29, 11, 23, 4, 3f);
				line(35, 11, 41, 4, 3f);
			}
			break;
			case SPEEDLINES: {
				line(6, 20, 40, 20, w);
				line(16, 32, 58, 32, w);
				line(6, 44, 40, 44, w);
				disc(50, 20, 2.5f);
				disc(50, 44, 2.5f);
			}
			break;
			case WHEEL: {
				ring(32, 32, 26, w);
				for (int i = 0; i < 6; i++) {
					double a = Math.PI * 2 * i / 6;
					line(32, 32, 32 + (float) Math.cos(a) * 26, 32 + (float) Math.sin(a) * 26, 3f);
				}
				disc(32, 32, 5f);
			}
			break;
			case SHIELD: {
				poly(w, 32, 6, 54, 14, 52, 36, 32, 58, 12, 36, 10, 14, 32, 6);
				line(32, 14, 32, 50, 3f);
			}
			break;
			case DROPITEM: {
				box(20, 6, 24, 24, 3, w);
				line(14, 38, 14, 48, 3f);
				line(32, 36, 32, 50, 3f);
				line(50, 38, 50, 48, 3f);
				line(6, 58, 58, 58, w);
			}
			break;
			case PLANE: {
				poly(w, 6, 30, 58, 8, 40, 56, 30, 36, 6, 30);
				line(30, 36, 58, 8, 3f);
			}
			break;
			case TROPHY: {
				poly(w, 18, 8, 46, 8, 44, 28, 32, 36, 20, 28, 18, 8);
				arc(15, 17, 7, 0.25, 0.75, 3.4f);
				arc(49, 17, 7, 0.75, 1.25, 3.4f);
				line(32, 36, 32, 46, w);
				line(24, 46, 40, 46, w);
				line(20, 55, 44, 55, w);
			}
			break;
			case SAVE: {
				poly(w, 8, 8, 46, 8, 56, 18, 56, 56, 8, 56, 8, 8);
				box(18, 8, 22, 14, 1, 3f);
				box(16, 34, 32, 22, 2, 3f);
			}
			break;
			case FILTER: {
				poly(w, 6, 8, 58, 8, 38, 32, 38, 52, 26, 58, 26, 32, 6, 8);
			}
			break;
			case LOCK: {
				box(12, 28, 40, 30, 5, w);
				arc(32, 28, 12, 0.5, 1.0, w);
				disc(32, 40, 4f);
				line(32, 42, 32, 50, w);
			}
			break;
			case STAR: {
				poly(w, 32, 6, 38.5f, 23.1f, 56.7f, 24, 42.4f, 35.4f, 47.3f, 53, 32, 43, 16.7f, 53, 21.6f, 35.4f, 7.3f, 24, 25.5f, 23.1f, 32, 6);
			}
			break;
			case SPEAKER: {
				poly(w, 8, 24, 18, 24, 32, 12, 32, 52, 18, 40, 8, 40, 8, 24);
				arc(32, 32, 12, 0.88, 1.12, w);
				arc(32, 32, 22, 0.86, 1.14, w);
			}
			break;
			case BROADCAST: {
				disc(32, 32, 5f);
				arc(32, 32, 14, 0.38, 0.62, w);
				arc(32, 32, 14, 0.88, 1.12, w);
				arc(32, 32, 25, 0.38, 0.62, w);
				arc(32, 32, 25, 0.88, 1.12, w);
			}
			break;
			case MEDAL: {
				ring(32, 42, 14, w);
				poly(w, 23, 31, 14, 6, 26, 6, 32, 22);
				poly(w, 41, 31, 50, 6, 38, 6, 32, 22);
				disc(32, 42, 4f);
			}
			break;
			case FOLDER: {
				poly(w, 6, 14, 24, 14, 30, 20, 58, 20, 58, 54, 6, 54, 6, 14);
				line(6, 27, 58, 27, 3f);
			}
			break;
			case KEYSEARCH: {
				box(4, 10, 40, 26, 4, w);
				fillRect(10, 16, 5, 5);
				fillRect(19, 16, 5, 5);
				fillRect(28, 16, 5, 5);
				fillRect(12, 26, 18, 4);
				ring(44, 44, 9, w);
				line(51, 51, 58, 58, w);
			}
			break;
			case SHULKER: {
				box(8, 26, 48, 30, 4, w);
				poly(w, 8, 26, 12, 8, 52, 8, 56, 26);
				disc(32, 40, 5f);
			}
			break;
			case ANIMATION: {
				line(16, 50, 46, 18, w);
				line(10, 44, 22, 56, w);
				line(12, 54, 6, 60, w);
				arc(30, 42, 26, 0.62, 0.9, 3f);
			}
			break;
			case MIC: {
				box(22, 6, 20, 32, 10, w);
				arc(32, 28, 16, 0.0, 0.5, w);
				line(32, 44, 32, 56, w);
				line(22, 56, 42, 56, w);
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
