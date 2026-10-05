package com.tatnat.client.ui.render;

import net.minecraft.client.Minecraft;

/**
 * UI scale for the menus. Layouts are written in "design pixels" for a 1920x1080 window;
 * {@link #px} converts to real pixels for the current window so the menu covers the same share
 * of the screen at any resolution, like Feather's.
 */
public final class Ui {
	private Ui() {
	}

	/** Real pixels per design pixel. Updated once per frame by {@link #update()}. */
	public static float s = 1f;

	public static void update() {
		Minecraft mc = Minecraft.getInstance();
		float w = mc.getWindow().getWidth() / 1920f, h = mc.getWindow().getHeight() / 1080f;
		s = Math.max(0.55f, Math.min(w, h));
	}

	public static int px(double design) {
		return (int) Math.round(design * s);
	}
}
