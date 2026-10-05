package com.tatnat.client.platform;

/** Where the camera is and where it looks (degrees), plus the vertical FOV of the last frame. */
public final class CameraInfo {
	public final double x, y, z;
	public final float yaw, pitch, fov;

	public CameraInfo(double x, double y, double z, float yaw, float pitch, float fov) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.fov = fov;
	}
}
