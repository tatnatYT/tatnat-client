package com.tatnat.client.ui.clickgui;

import com.tatnat.client.ui.render.Animation;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Gfx;

/** Shared look for the small controls used across the menu (pixel space, scaled by {@link Ui}). */
public final class Widgets {
	private Widgets() {
	}

	public static int toggleW() {
		return Ui.px(44);
	}

	public static int toggleH() {
		return Ui.px(22);
	}

	/**
	 * Feather-style switch: red track with a white knob when on, dark track with a grey knob when
	 * off. {@code anim} runs 0 (off) .. 1 (on) over 150ms; knob, track and knob colour all glide.
	 */
	public static void toggle(Gfx g, int x, int y, Animation anim, boolean hovered) {
		float t = anim.get();
		int w = toggleW(), h = toggleH();
		int track = Colors.lerp(Theme.TRACK, Theme.ACCENT, t);
		if (hovered) track = Colors.shade(track, 1.15f);
		RenderUtils.roundedRect(g, x, y, w, h, h / 2, track);
		int pad = Math.max(2, Ui.px(3));
		int knob = h - pad * 2;
		int kx = Math.round(x + pad + (w - knob - pad * 2) * t);
		RenderUtils.circle(g, kx + knob / 2, y + pad + knob / 2, knob / 2, Colors.lerp(Theme.KNOB_OFF, 0xFFFFFFFF, t));
	}

	/** A rounded button with centred text. */
	public static void button(Gfx g, int x, int y, int w, int h, String text, UIFont font, int bg, int fg, boolean hovered) {
		RenderUtils.roundedRect(g, x, y, w, h, Ui.px(Theme.RADIUS), hovered ? Colors.shade(bg, 1.18f) : bg);
		font.drawCentered(g, text, x + w / 2, y + (h - font.size()) / 2, fg);
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	/** The mouse position in real window pixels (sub-GUI-pixel precise). */
	public static double mouseX() {
		return TatnatClient.game().mouseX();
	}

	public static double mouseY() {
		return TatnatClient.game().mouseY();
	}
}
