package com.tatnat.client.util;

import org.joml.Vector3fc;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Projects world positions onto the screen using the current camera and the FOV the game
 * actually rendered with (captured each frame in {@code GameRendererMixin}).
 */
public final class WorldProjector {
	private WorldProjector() {
	}

	/** Vertical FOV in degrees of the last rendered frame (zoom and FOV mods included). */
	public static volatile float lastFov = 70f;

	/**
	 * Returns {x, y, onScreen, depth} in GUI-scaled coordinates. Points behind you still get a
	 * direction (x, y relative to the centre) so callers can draw an edge arrow.
	 */
	public static double[] project(Vec3 world) {
		Minecraft mc = Minecraft.getInstance();
		Camera cam = mc.gameRenderer.getMainCamera();
		Vec3 rel = world.subtract(cam.position());
		Vector3fc f = cam.forwardVector(), u = cam.upVector(), l = cam.leftVector();
		double z = rel.x * f.x() + rel.y * f.y() + rel.z * f.z();
		double x = -(rel.x * l.x() + rel.y * l.y() + rel.z * l.z());
		double y = rel.x * u.x() + rel.y * u.y() + rel.z * u.z();

		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		double tanY = Math.tan(Math.toRadians(lastFov) / 2);
		double aspect = mc.getWindow().getWidth() / (double) Math.max(1, mc.getWindow().getHeight());
		if (z > 0.05) {
			double ndcX = x / (z * tanY * aspect), ndcY = y / (z * tanY);
			boolean on = Math.abs(ndcX) <= 1 && Math.abs(ndcY) <= 1;
			return new double[] {w / 2.0 + ndcX * w / 2.0, h / 2.0 - ndcY * h / 2.0, on ? 1 : 0, z};
		}
		// Behind the camera: only the direction matters.
		return new double[] {w / 2.0 + x * 1000, h / 2.0 - y * 1000, 0, z};
	}
}
