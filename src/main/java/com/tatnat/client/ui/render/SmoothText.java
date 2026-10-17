package com.tatnat.client.ui.render;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Gfx;

/**
 * Smooth menu text. Minecraft draws TrueType fonts hard-edged (hinted, nearest-filtered), which looks
 * pixelated at menu sizes; here each string is rasterised once with Java's anti-aliased text renderer
 * (Inter, fractional metrics), cached as a small white image and drawn as one tinted quad. Strings with
 * characters Inter lacks, and anything past the cache size, fall back to the game's font.
 */
public final class SmoothText {
	/** At most this many different strings get their own texture (textures are kept for the session). */
	private static final int MAX_STRINGS = 12000;
	/** Strings are drawn this many times bigger, then scaled down: smoother edges than drawing at size. */
	private static final int SS = 3;

	private static final class Entry {
		final int w, h, ascent, advance;
		final BufferedImage img;

		Entry(int w, int h, int ascent, int advance, BufferedImage img) {
			this.w = w;
			this.h = h;
			this.ascent = ascent;
			this.advance = advance;
			this.img = img;
		}

		byte[] png() {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(img, "png", out);
				return out.toByteArray();
			} catch (Exception e) {
				return null;
			}
		}
	}

	private static final Map<Integer, Font> FONTS = new HashMap<>();
	private static final Map<String, Entry> ENTRIES = new HashMap<>();
	/** Widths of strings measured but not (yet) drawn, so measuring doesn't use up the texture budget. */
	private static final Map<String, Integer> WIDTHS = new LinkedHashMap<String, Integer>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Integer> e) {
			return size() > 4000;
		}
	};
	private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
	private static boolean broken;

	private SmoothText() {
	}

	/** Draws {@code s} like {@link Gfx#uiText}; false if the caller should use the game's font instead. */
	public static boolean draw(Gfx g, int weight, int px, String s, int x, int y, int argb) {
		if (s == null || s.isEmpty()) return true;
		Entry e = entry(weight, px, s);
		if (e == null) return false;
		// Same baseline as the game font path: y + 0.8 px.
		g.picture("smooth/" + weight + "/" + px + "/" + s, e::png, x - 1, y + Math.round(px * 0.8f) - e.ascent, e.w, e.h, argb);
		return true;
	}

	/** Width of {@code s} in pixels as drawn by {@link #draw}, or -1 when the game's font is used. */
	public static int width(int weight, int px, String s) {
		if (s == null || s.isEmpty()) return 0;
		Font f = font(weight, px);
		if (f == null || f.canDisplayUpTo(s) != -1) return -1;
		String key = weight + "/" + px + "/" + s;
		Entry e = ENTRIES.get(key);
		if (e != null) return e.advance;
		Integer w = WIDTHS.get(key);
		if (w == null) {
			w = (int) Math.ceil(f.getStringBounds(s, FRC).getWidth());
			WIDTHS.put(key, w);
		}
		return w;
	}

	private static Entry entry(int weight, int px, String s) {
		String key = weight + "/" + px + "/" + s;
		Entry e = ENTRIES.get(key);
		if (e != null) return e;
		if (ENTRIES.size() >= MAX_STRINGS) return null;
		Font f = font(weight, px);
		if (f == null || f.canDisplayUpTo(s) != -1) return null;
		LineMetrics lm = f.getLineMetrics(s, FRC);
		int ascent = (int) Math.ceil(lm.getAscent()) + 1, descent = (int) Math.ceil(lm.getDescent()) + 1;
		int advance = (int) Math.ceil(f.getStringBounds(s, FRC).getWidth());
		int w = advance + 3, h = ascent + descent;
		Font big = f.deriveFont(f.getSize2D() * SS);
		BufferedImage hi = new BufferedImage(w * SS, h * SS, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = hi.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setFont(big);
		g.setColor(java.awt.Color.WHITE);
		g.drawString(s, SS, ascent * SS);
		g.dispose();
		// Average each SS x SS block into one pixel (white, coverage in alpha), slightly boosted so thin
		// strokes stay solid.
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] src = hi.getRGB(0, 0, w * SS, h * SS, null, 0, w * SS);
		int[] dst = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int sum = 0;
				for (int j = 0; j < SS; j++) {
					int row = (y * SS + j) * w * SS + x * SS;
					for (int i = 0; i < SS; i++) sum += src[row + i] >>> 24;
				}
				float a = sum / (255f * SS * SS);
				int out = Math.round((float) Math.pow(a, 0.85) * 255);
				dst[y * w + x] = out << 24 | 0xFFFFFF;
			}
		}
		img.setRGB(0, 0, w, h, dst, 0, w);
		e = new Entry(w, h, ascent, advance, img);
		ENTRIES.put(key, e);
		return e;
	}

	/** A file inside the mod jar. Mod loaders differ in which lookup works (NeoForge's module system), so try each. */
	private static InputStream open(String path) {
		InputStream in = SmoothText.class.getResourceAsStream("/" + path);
		if (in == null && SmoothText.class.getClassLoader() != null) in = SmoothText.class.getClassLoader().getResourceAsStream(path);
		if (in == null && Thread.currentThread().getContextClassLoader() != null) in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		return in;
	}

	private static Font font(int weight, int px) {
		if (broken) return null;
		int k = weight * 1000 + px;
		Font f = FONTS.get(k);
		if (f != null) return f;
		Font base = FONTS.get(-weight);
		if (base == null) {
			try (InputStream in = open("assets/tatnatclient/font/inter-" + weight + ".ttf")) {
				if (in == null) throw new java.io.FileNotFoundException("inter-" + weight + ".ttf");
				base = Font.createFont(Font.TRUETYPE_FONT, in);
			} catch (Throwable t) {
				TatnatClient.LOG.warn("Smooth menu text is off (font unavailable): {}", t.toString());
				broken = true;
				return null;
			}
			FONTS.put(-weight, base);
		}
		// Minecraft's TrueType "size" is the em size in pixels, the same as Java's.
		f = base.deriveFont((float) px);
		FONTS.put(k, f);
		return f;
	}
}
