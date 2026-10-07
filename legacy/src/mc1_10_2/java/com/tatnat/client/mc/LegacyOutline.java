package com.tatnat.client.mc;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/**
 * Block Overlay geometry: thick edge bars and a tinted face, as coloured quads. Lives outside the
 * mixin package (an enum switch there makes the compiler add a helper class mixins can't load).
 */
public final class LegacyOutline {
	private LegacyOutline() {
	}

	public static void edges(BufferBuilder v, Box b, float t, int argb) {
		float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
		float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
		float h = t / 2f;
		for (float y : new float[] {y0, y1}) for (float z : new float[] {z0, z1}) bar(v, x0 - h, y - h, z - h, x1 + h, y + h, z + h, argb);
		for (float x : new float[] {x0, x1}) for (float z : new float[] {z0, z1}) bar(v, x - h, y0 - h, z - h, x + h, y1 + h, z + h, argb);
		for (float x : new float[] {x0, x1}) for (float y : new float[] {y0, y1}) bar(v, x - h, y - h, z0 - h, x + h, y + h, z1 + h, argb);
	}

	private static void bar(BufferBuilder v, float x0, float y0, float z0, float x1, float y1, float z1, int c) {
		quad(v, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		quad(v, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		quad(v, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		quad(v, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		quad(v, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		quad(v, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
	}

	public static void face(BufferBuilder v, Box b, Direction side, int argb) {
		final float e = 0.002f;
		float x0 = (float) b.minX - e, y0 = (float) b.minY - e, z0 = (float) b.minZ - e;
		float x1 = (float) b.maxX + e, y1 = (float) b.maxY + e, z1 = (float) b.maxZ + e;
		switch (side) {
			case UP: quad(v, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); break;
			case DOWN: quad(v, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); break;
			case NORTH: quad(v, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); break;
			case SOUTH: quad(v, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); break;
			case WEST: quad(v, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); break;
			default: quad(v, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); break;
		}
	}

	private static void quad(BufferBuilder v, int argb, float... p) {
		for (int i = 0; i < 12; i += 3) v.vertex(p[i], p[i + 1], p[i + 2]).color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24).next();
	}
}
