package com.tatnat.client.modules.impl.cosmetic;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.tatnat.client.TatnatClient;

/**
 * Built-in cape designs, painted pixel by pixel onto the standard 64x32 cape layout as ARGB
 * (row-major, 64 * 32). The part other players see (the outside of the cape) is the 10x16 area at
 * (1, 1); the inside is at (12, 1); the edges fill the rest. Each platform turns the pixels into
 * a texture its own way.
 */
public final class CapeArt {
	public static final int W = 64, H = 32;

	private CapeArt() {
	}

	public static int[] draw(String design) {
		int[] img = new int[W * H];
		int[] c;
		switch (design) {
			case "Crimson": c = new int[] {0xFF8E1621, 0xFFE5323E, 0xFF3A070C}; break;
			case "Midnight": c = new int[] {0xFF15151C, 0xFF6B4EE6, 0xFF07070A}; break;
			case "Ocean": c = new int[] {0xFF0E3B6B, 0xFF4EB1FF, 0xFF06203D}; break;
			case "Forest": c = new int[] {0xFF1D4A24, 0xFF62C462, 0xFF0D2611}; break;
			case "Gold": c = new int[] {0xFF7A5A12, 0xFFFFC83D, 0xFF3D2C06}; break;
			default: c = new int[] {0xFF19191C, 0xFFE5323E, 0xFF0C0C0E}; break;
		}
		// Edges + inside in the dark shade, outside as a vertical gradient with an accent trim.
		for (int y = 0; y < 17; y++) for (int x = 0; x < 22; x++) set(img, x, y, c[2]);
		for (int y = 1; y < 17; y++) {
			float t = (y - 1) / 15f;
			for (int x = 1; x < 11; x++) set(img, x, y, lerp(c[0], c[2], t * 0.7f));
		}
		for (int x = 1; x < 11; x++) {
			set(img, x, 15, c[1]);
			set(img, x, 16, c[1]);
		}
		if (design.equals("tatnat")) {
			paintLogo(img, c[1]);
		} else {
			// A small diamond emblem.
			int[][] d = {{5, 4}, {6, 4}, {4, 5}, {5, 5}, {6, 5}, {7, 5}, {4, 6}, {5, 6}, {6, 6}, {7, 6}, {5, 7}, {6, 7}};
			for (int[] p : d) set(img, p[0], p[1], c[1]);
		}
		return img;
	}

	/** Reads a cape PNG; old 22x17 capes are placed onto a 64x32 canvas. */
	public static int[] fromImage(BufferedImage src) {
		int[] img = new int[W * H];
		int sx = src.getWidth() >= 64 ? src.getWidth() / 64 : 1;
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int px = x * sx, py = y * sx;
				if (px < src.getWidth() && py < src.getHeight()) img[y * W + x] = src.getRGB(px, py);
			}
		}
		return img;
	}

	/** Paints the tatnat head (the mod's logo, an 8x8 face) onto the cape's outside. */
	private static void paintLogo(int[] img, int trim) {
		try (InputStream in = CapeArt.class.getResourceAsStream("/assets/tatnatclient/logo.png")) {
			BufferedImage logo = ImageIO.read(in);
			int step = logo.getWidth() / 8;
			for (int y = 0; y < 8; y++) {
				for (int x = 0; x < 8; x++) set(img, 2 + x, 3 + y, logo.getRGB(x * step + step / 2, y * step + step / 2) | 0xFF000000);
			}
		} catch (Exception e) {
			TatnatClient.LOG.warn("Cape logo unavailable: {}", e.toString());
		}
		for (int x = 1; x < 11; x++) set(img, x, 1, trim);
	}

	private static void set(int[] img, int x, int y, int argb) {
		img[y * W + x] = argb;
	}

	private static int lerp(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}
}
