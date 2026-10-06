package com.tatnat.client.mc;

import java.util.ArrayDeque;

import com.mojang.blaze3d.platform.GlStateManager;

/**
 * 1.14 has no PoseStack. GUI code here only ever translates and scales, so a 2D
 * scale-then-offset stack is all it needs; {@link #apply()} pushes it onto the GL matrix.
 */
public final class Pose2D {
	private float sx = 1f, sy = 1f, tx, ty;
	private final ArrayDeque<float[]> stack = new ArrayDeque<>();

	public void pushPose() {
		stack.push(new float[] {sx, sy, tx, ty});
	}

	public void popPose() {
		float[] s = stack.pop();
		sx = s[0];
		sy = s[1];
		tx = s[2];
		ty = s[3];
	}

	public void translate(float x, float y, float z) {
		tx += x * sx;
		ty += y * sy;
	}

	public void scale(float x, float y, float z) {
		sx *= x;
		sy *= y;
	}

	public float mapX(float x) {
		return tx + x * sx;
	}

	public float mapY(float y) {
		return ty + y * sy;
	}

	/** Applies this pose on top of the current GL matrix (caller pushes/pops). */
	public void apply() {
		GlStateManager.translatef(tx, ty, 0f);
		GlStateManager.scalef(sx, sy, 1f);
	}
}
