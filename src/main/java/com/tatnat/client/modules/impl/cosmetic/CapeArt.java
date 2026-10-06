package com.tatnat.client.modules.impl.cosmetic;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.tatnat.client.TatnatClient;

/**
 * Built-in capes, painted onto a high-resolution 512x256 cape texture as ARGB (row-major). Minecraft
 * maps the cape by proportion, so this is the vanilla 64x32 layout at 8x the resolution (sizes below
 * are in 128x64 units, times K):
 * <ul>
 * <li>outside (what people see behind you): 20x32 at (2, 2)</li>
 * <li>inside (towards your back): 20x32 at (24, 2)</li>
 * <li>side edges: 2x32 at (0, 2) and (22, 2); top face 20x2 at (2, 0); bottom face 20x2 at (22, 0)</li>
 * <li>the rest is the elytra, which gets the base colour</li>
 * </ul>
 * Style rules: flat, matte colours (greys from #121214 to #e0e0e0), one bright accent, no noise,
 * and anti-aliased shapes (each pixel is sampled 4x4).
 */
public final class CapeArt {
	public static final int W = 512, H = 256;
	/** Pixels per 128x64 unit: the designs are laid out on that grid and drawn K times finer. */
	private static final int K = W / 128;

	/** Outside face. */
	private static final int OX = 2 * K, OY = 2 * K, FW = 20 * K, FH = 32 * K;
	/** Inside face. */
	private static final int IX = 24 * K;

	public static final String[] DESIGNS = {"tatnat", "Signature", "Aura", "Chroma", "Split", "Custom"};
	/** Designs that change over time (re-drawn a few times a second). */
	public static boolean animated(String design, boolean customChroma) {
		return design.equals("Chroma") || (design.equals("Custom") && customChroma);
	}

	private static final int CHARCOAL = 0xFF1E1E22, CYAN = 0xFF4EB1FF, WHITE = 0xFFE0E0E0, TOP = 0xFF2A2A30, BOTTOM = 0xFF121214;

	private CapeArt() {
	}

	/**
	 * @param main   Custom: main colour; @param accent Custom: accent colour
	 * @param timeMs animation clock (Chroma)
	 */
	public static int[] draw(String design, int main, int accent, long timeMs) {
		int[] img = new int[W * H];
		switch (design) {
			case "Signature": signature(img); break;
			case "Aura": aura(img); break;
			case "Chroma": chroma(img, timeMs); break;
			case "Split": split(img); break;
			case "Custom": custom(img, main | 0xFF000000, accent | 0xFF000000); break;
			default: classic(img); break;
		}
		return img;
	}

	// ------------------------------------------------------------------ designs

	/** Charcoal with a minimalist anti-aliased "t" monogram in the accent, dead centre. */
	private static void signature(int[] img) {
		base(img, CHARCOAL);
		monogram(img, CYAN, 1f);
	}

	/** Grey-to-black gradient with a glowing accent edge (left, right, bottom) that blooms inward. */
	private static void aura(int[] img) {
		base(img, BOTTOM);
		for (int y = 0; y < FH; y++) {
			int g = lerp(TOP, BOTTOM, y / (float) (FH - 1));
			for (int x = 0; x < FW; x++) {
				// Distance to the glowing edges (left, right, bottom) in pixels.
				float d = Math.min(Math.min(x + 0.5f, FW - x - 0.5f), FH - y - 0.5f) / K;
				int c = g;
				// 2px solid edge, then a smooth exponential bloom into the dark base.
				if (d < 2) c = CYAN;
				else c = lerp(g, CYAN, (float) Math.exp(-(d - 2) / 1.6f) * 0.5f);
				set(img, OX + x, OY + y, c);
			}
		}
		inside(img, 0.75f);
	}

	/** Charcoal with a diagonal stripe whose colour flows cyan, blue, purple, magenta over time. */
	private static void chroma(int[] img, long timeMs) {
		base(img, CHARCOAL);
		float phase = (timeMs % 4000) / 4000f;
		float len = (float) Math.hypot(FW, FH);
		for (int y = 0; y < FH; y++) {
			for (int x = 0; x < FW; x++) {
				// Stripe along the top-left to bottom-right diagonal, 7px wide.
				float cov = cover(x, y, (px, py) -> Math.abs(px * FH - py * FW) / len < 3.5f * K);
				if (cov <= 0) continue;
				float along = (x * FW + y * FH) / (len * len);
				int c = chromaColour(along - phase);
				set(img, OX + x, OY + y, lerp(CHARCOAL, c, cov));
			}
		}
		inside(img, 0.75f);
	}

	/** Split down the middle: charcoal left, soft white right, a 2px accent seam. */
	private static void split(int[] img) {
		base(img, CHARCOAL);
		for (int y = 0; y < FH; y++) {
			for (int x = 0; x < FW; x++) {
				int c = x < FW / 2 ? CHARCOAL : WHITE;
				if (Math.abs(x + 0.5f - FW / 2f) < K) c = CYAN;
				set(img, OX + x, OY + y, c);
				// Inside mirrored, slightly darker.
				int m = (FW - 1 - x) < FW / 2 ? CHARCOAL : WHITE;
				if (Math.abs(x + 0.5f - FW / 2f) < K) m = CYAN;
				set(img, IX + x, OY + y, shade(m, 0.8f));
			}
		}
	}

	/** Your colour: a soft vertical gradient, a thin accent hem and the monogram in the accent. */
	private static void custom(int[] img, int main, int accent) {
		base(img, shade(main, 0.7f));
		for (int y = 0; y < FH; y++) {
			int c = lerp(shade(main, 1.08f), shade(main, 0.72f), y / (float) (FH - 1));
			for (int x = 0; x < FW; x++) set(img, OX + x, OY + y, c);
		}
		for (int y = FH - 2 * K; y < FH; y++) for (int x = 0; x < FW; x++) set(img, OX + x, OY + y, accent);
		monogram(img, accent, 1f);
		inside(img, 0.75f);
	}

	/** The original tatnat cape (pixel head, red trim): pixel art at 64x32, scaled up. */
	private static void classic(int[] img) {
		int[] lo = new int[64 * 32];
		int dark = 0xFF0C0C0E, face = 0xFF19191C, red = 0xFFE5323E;
		java.util.Arrays.fill(lo, dark);
		for (int y = 0; y < 17; y++) for (int x = 0; x < 22; x++) lo[y * 64 + x] = dark;
		for (int x = 1; x < 21; x++) lo[x] = red;
		for (int y = 1; y < 17; y++) {
			float t = (y - 1) / 15f;
			for (int x = 1; x < 11; x++) lo[y * 64 + x] = lerp(face, dark, 0.15f + t * 0.6f);
		}
		for (int x = 1; x < 11; x++) {
			lo[64 + x] = red;
			lo[16 * 64 + x] = red;
		}
		for (int y = 1; y < 17; y++) {
			lo[y * 64 + 1] = red;
			lo[y * 64 + 10] = red;
		}
		try (InputStream in = CapeArt.class.getResourceAsStream("/assets/tatnatclient/logo.png")) {
			BufferedImage logo = ImageIO.read(in);
			int step = logo.getWidth() / 8;
			for (int y = 0; y < 8; y++) {
				for (int x = 0; x < 8; x++) lo[(4 + y) * 64 + 2 + x] = logo.getRGB(x * step + step / 2, y * step + step / 2) | 0xFF000000;
			}
		} catch (Exception e) {
			TatnatClient.LOG.warn("Cape logo unavailable: {}", e.toString());
		}
		for (int x = 4; x < 8; x++) lo[13 * 64 + x] = red;
		for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) img[y * W + x] = lo[(y * 64 / W) * 64 + x * 64 / W];
	}

	// ------------------------------------------------------------------ pieces

	/** Whole texture in one colour; inside and edges a little darker, like the fabric's shadow side. */
	private static void base(int[] img, int c) {
		java.util.Arrays.fill(img, c);
		int dark = shade(c, 0.8f);
		for (int y = OY; y < OY + FH; y++) {
			for (int x = IX; x < IX + FW; x++) set(img, x, y, dark);
			for (int x = 0; x < 2 * K; x++) set(img, x, y, dark);
			for (int x = 22 * K; x < 24 * K; x++) set(img, x, y, dark);
		}
	}

	/** Inside face: the outside's average colour, darkened (it faces your back). */
	private static void inside(int[] img, float k) {
		long r = 0, g = 0, b = 0;
		for (int y = 0; y < FH; y++) {
			for (int x = 0; x < FW; x++) {
				int c = img[(OY + y) * W + OX + x];
				r += (c >> 16) & 255;
				g += (c >> 8) & 255;
				b += c & 255;
			}
		}
		int n = FW * FH;
		int avg = 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
		for (int y = 0; y < FH; y++) for (int x = 0; x < FW; x++) set(img, IX + x, OY + y, shade(avg, k));
	}

	/** A minimalist lowercase "t": stem with a hooked foot and a crossbar, anti-aliased. */
	private static void monogram(int[] img, int colour, float alpha) {
		float[][] strokes = {
				{10, 7, 10, 21, 1.9f}, // stem
				{10, 21, 11.2f, 23.6f, 1.9f}, // foot curve
				{11.2f, 23.6f, 14.2f, 24.6f, 1.9f},
				{6.2f, 11.5f, 14.2f, 11.5f, 1.5f}, // crossbar
		};
		for (int y = 0; y < FH; y++) {
			for (int x = 0; x < FW; x++) {
				float cov = cover(x, y, (px, py) -> {
					for (float[] s : strokes) if (segDist(px / K, py / K, s[0], s[1], s[2], s[3]) < s[4]) return true;
					return false;
				}) * alpha;
				if (cov > 0) set(img, OX + x, OY + y, lerp(img[(OY + y) * W + OX + x], colour, cov));
			}
		}
	}

	private interface Shape {
		boolean in(float x, float y);
	}

	/** Share of pixel (x, y) inside {@code s}, from 4x4 samples. */
	private static float cover(int x, int y, Shape s) {
		int hits = 0;
		for (int j = 0; j < 4; j++) for (int i = 0; i < 4; i++) if (s.in(x + (i + 0.5f) / 4f, y + (j + 0.5f) / 4f)) hits++;
		return hits / 16f;
	}

	private static float segDist(float px, float py, float ax, float ay, float bx, float by) {
		float dx = bx - ax, dy = by - ay;
		float t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
		return (float) Math.hypot(px - ax - t * dx, py - ay - t * dy);
	}

	/** Smooth loop through cyan, blue, purple and magenta (no harsh rainbow). */
	private static int chromaColour(float t) {
		int[] stops = {0xFF4EB1FF, 0xFF3D6BFF, 0xFF8A4DFF, 0xFFE04EDB};
		t = t - (float) Math.floor(t);
		float p = t * stops.length;
		int i = (int) p;
		return lerp(stops[i % stops.length], stops[(i + 1) % stops.length], p - i);
	}

	/** Reads a cape PNG (22x17, 64x32 or any HD size) and scales it to W x H. */
	public static int[] fromImage(BufferedImage src) {
		int[] img = new int[W * H];
		// Old 22x17 capes have no padding: they are a 64x32 layout's top-left corner.
		float scale = (src.getWidth() < 64 ? 64f : src.getWidth()) / W;
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int px = (int) (x * scale), py = (int) (y * scale);
				if (px < src.getWidth() && py < src.getHeight()) img[y * W + x] = src.getRGB(px, py);
			}
		}
		return img;
	}

	private static void set(int[] img, int x, int y, int argb) {
		img[y * W + x] = argb;
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
}
