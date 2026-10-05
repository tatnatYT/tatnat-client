package com.tatnat.client.platform;

/**
 * Version-specific pieces of individual mods (things that need real game objects: textures,
 * entities, overlays). The platform implements what it supports; anything left at the default
 * simply does nothing on that version.
 */
public interface Features {
	/** Recolour the hurt flash (ARGB where alpha = how much original colour to keep). */
	default void setHurtColor(int argb) {
	}

	/** Enchant Glint: tint the glint textures, or restore vanilla's when {@code argb == 0}. */
	default void tintGlint(int argb) {
	}

	/** Freecam: detach / reattach the camera. */
	default boolean startFreecam() {
		return false;
	}

	default void stopFreecam() {
	}

	/** Freecam: move the detached camera by this much (blocks). */
	default void moveFreecam(double dx, double dy, double dz) {
	}

	/** Yaw of the detached camera (for camera-relative movement). */
	default float freecamYaw() {
		return 0f;
	}

	/** Whether this version can show the given feature (greys out mods that can't work). */
	default boolean supports(String feature) {
		return true;
	}
}
