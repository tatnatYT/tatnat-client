package com.tatnat.client.modules.impl.cosmetic;

import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;
import com.tatnat.client.TatnatClient;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Built-in cape designs, painted pixel by pixel onto the standard 64x32 cape layout. The part
 * other players see (the outside of the cape) is the 10x16 area at (1, 1); the inside is at
 * (12, 1); the edges fill the rest.
 */
final class CapeArt {
	private CapeArt() {
	}

	static NativeImage draw(String design) {
		NativeImage img = new NativeImage(64, 32, true);
		int[] c = switch (design) {
			case "Crimson" -> new int[] {0xFF8E1621, 0xFFE5323E, 0xFF3A070C};
			case "Midnight" -> new int[] {0xFF15151C, 0xFF6B4EE6, 0xFF07070A};
			case "Ocean" -> new int[] {0xFF0E3B6B, 0xFF4EB1FF, 0xFF06203D};
			case "Forest" -> new int[] {0xFF1D4A24, 0xFF62C462, 0xFF0D2611};
			case "Gold" -> new int[] {0xFF7A5A12, 0xFFFFC83D, 0xFF3D2C06};
			default -> new int[] {0xFF19191C, 0xFFE5323E, 0xFF0C0C0E};
		};
		// Edges + inside in the dark shade, outside as a vertical gradient with an accent trim.
		for (int y = 0; y < 17; y++) for (int x = 0; x < 22; x++) img.setPixel(x, y, c[2]);
		for (int y = 1; y < 17; y++) {
			float t = (y - 1) / 15f;
			for (int x = 1; x < 11; x++) img.setPixel(x, y, lerp(c[0], c[2], t * 0.7f));
		}
		for (int x = 1; x < 11; x++) {
			img.setPixel(x, 15, c[1]);
			img.setPixel(x, 16, c[1]);
		}
		if (design.equals("tatnat")) paintLogo(img, c[1]);
		else {
			// A small diamond emblem.
			int[][] d = {{5, 4}, {6, 4}, {4, 5}, {5, 5}, {6, 5}, {7, 5}, {4, 6}, {5, 6}, {6, 6}, {7, 6}, {5, 7}, {6, 7}};
			for (int[] p : d) img.setPixel(p[0], p[1], c[1]);
		}
		return img;
	}

	/** Paints the tatnat head (the mod's logo, an 8x8 face) onto the cape's outside. */
	private static void paintLogo(NativeImage img, int trim) {
		try (InputStream in = Minecraft.getInstance().getResourceManager()
				.open(Identifier.fromNamespaceAndPath(TatnatClient.ID, "logo.png"))) {
			NativeImage logo = NativeImage.read(in);
			int step = logo.getWidth() / 8;
			for (int y = 0; y < 8; y++) {
				for (int x = 0; x < 8; x++) img.setPixel(2 + x, 3 + y, logo.getPixel(x * step + step / 2, y * step + step / 2) | 0xFF000000);
			}
			logo.close();
		} catch (Exception e) {
			TatnatClient.LOG.warn("Cape logo unavailable", e);
		}
		for (int x = 1; x < 11; x++) img.setPixel(x, 1, trim);
	}

	private static int lerp(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}
}
