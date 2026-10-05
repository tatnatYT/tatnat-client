package com.tatnat.client.util;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.CameraInfo;
import com.tatnat.client.platform.Game;

/**
 * Projects world positions onto the screen from the camera's position, yaw, pitch and the FOV
 * the game actually rendered with. Pure maths, so it works identically on every version.
 */
public final class WorldProjector {
	private WorldProjector() {
	}

	/**
	 * Returns {x, y, onScreen (1/0), depth} in GUI-scaled coordinates. Points behind you still
	 * get a direction (x, y relative to the centre) so callers can draw an edge arrow.
	 */
	public static double[] project(double wx, double wy, double wz) {
		Game game = TatnatClient.game();
		CameraInfo cam = game.camera();
		double yaw = Math.toRadians(cam.yaw), pitch = Math.toRadians(cam.pitch);
		// Minecraft: yaw 0 looks towards +Z, positive pitch looks down.
		double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
		double lx = Math.cos(yaw), ly = 0, lz = Math.sin(yaw);
		// up = forward x left
		double ux = fy * lz - fz * ly, uy = fz * lx - fx * lz, uz = fx * ly - fy * lx;

		double rx = wx - cam.x, ry = wy - cam.y, rz = wz - cam.z;
		double z = rx * fx + ry * fy + rz * fz;
		double x = -(rx * lx + ry * ly + rz * lz);
		double y = rx * ux + ry * uy + rz * uz;

		int w = game.guiWidth(), h = game.guiHeight();
		double tanY = Math.tan(Math.toRadians(cam.fov) / 2);
		double aspect = game.windowWidth() / (double) Math.max(1, game.windowHeight());
		if (z > 0.05) {
			double ndcX = x / (z * tanY * aspect), ndcY = y / (z * tanY);
			boolean on = Math.abs(ndcX) <= 1 && Math.abs(ndcY) <= 1;
			return new double[] {w / 2.0 + ndcX * w / 2.0, h / 2.0 - ndcY * h / 2.0, on ? 1 : 0, z};
		}
		// Behind the camera: only the direction matters.
		return new double[] {w / 2.0 + x * 1000, h / 2.0 - y * 1000, 0, z};
	}
}
