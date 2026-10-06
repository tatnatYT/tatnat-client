package com.tatnat.client.mc;

import com.mojang.math.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.phys.AABB;

/**
 * A thick wireframe box made of filled quads: each of the 12 edges is a thin square bar. Used for
 * Block Overlay on versions whose line rendering has a fixed width.
 */
public final class EdgeBox {
	private EdgeBox() {
	}

	/** {@code b} is camera-relative; {@code t} is the bar thickness in blocks. */
	public static void draw(VertexConsumer v, Matrix4f m, AABB b, float t, int argb) {
		float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
		float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
		float h = t / 2f;
		// 4 edges along X, 4 along Y, 4 along Z.
		for (float y : new float[] {y0, y1}) for (float z : new float[] {z0, z1}) bar(v, m, x0 - h, y - h, z - h, x1 + h, y + h, z + h, argb);
		for (float x : new float[] {x0, x1}) for (float z : new float[] {z0, z1}) bar(v, m, x - h, y0 - h, z - h, x + h, y1 + h, z + h, argb);
		for (float x : new float[] {x0, x1}) for (float y : new float[] {y0, y1}) bar(v, m, x - h, y - h, z0 - h, x + h, y + h, z1 + h, argb);
	}

	private static void bar(VertexConsumer v, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, int c) {
		quad(v, m, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		quad(v, m, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		quad(v, m, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		quad(v, m, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		quad(v, m, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		quad(v, m, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
	}

	public static void quad(VertexConsumer v, Matrix4f m, int argb, float... p) {
		for (int i = 0; i < 12; i += 3) v.vertex(m, p[i], p[i + 1], p[i + 2]).color(argb).endVertex();
	}
}
