package com.tatnat.client.ui.render;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.ui.theme.Colors;

/**
 * The menu typeface: Inter, fully anti-aliased. Menus are laid out for a 1920x1080 "design"
 * screen and scaled by {@link Ui#s}; scaling rasterised text would blur it, so each style picks
 * the whole pixel size that matches the current scale and the platform draws it 1:1 on screen
 * pixels. Always draw in pixel space (see {@link RenderUtils#beginPixels}).
 *
 * Sizes follow the spec's 12/14/18pt (= 16/19/24px at 96 DPI), plus a tiny and a huge size.
 */
public enum UIFont {
	/** Sidebar labels. */
	TINY(700, 10),
	/** Descriptions, pills, hints (spec: 12pt). */
	SMALL(500, 14),
	/** Body text. */
	BODY(500, 16),
	/** Mod names (spec: 14pt). */
	TITLE(700, 19),
	/** Headers like the "Mod Menu" tab (spec: 18pt). */
	HEADER(700, 25),
	HUGE(700, 40);

	private final int weight;
	private final int designSize;

	UIFont(int weight, int designSize) {
		this.weight = weight;
		this.designSize = designSize;
	}

	/** Pixel size at the current UI scale. */
	public int size() {
		return Math.max(8, Math.min(56, Math.round(designSize * Ui.s)));
	}

	public int width(String s) {
		return TatnatClient.game().uiTextWidth(weight, size(), s);
	}

	/** Draws {@code s} with its em box's top-left at (x, y), in pixel space. */
	public void draw(Gfx g, String s, int x, int y, int color) {
		int c = Colors.fade(color, RenderUtils.alpha);
		// Near-transparent text is skipped: Minecraft draws very low alpha values as fully opaque.
		if (Colors.alpha(c) < 8) return;
		g.uiText(weight, size(), s, x, y, c);
	}

	/** Draws {@code s} vertically centred on {@code cy}. */
	public void drawMid(Gfx g, String s, int x, int cy, int color) {
		draw(g, s, x, cy - size() / 2, color);
	}

	public void drawCentered(Gfx g, String s, int cx, int y, int color) {
		draw(g, s, cx - width(s) / 2, y, color);
	}

	public void drawRight(Gfx g, String s, int right, int y, int color) {
		draw(g, s, right - width(s), y, color);
	}

	/** Shortens {@code s} with an ellipsis until it fits in {@code maxWidth} pixels. */
	public String trim(String s, int maxWidth) {
		if (width(s) <= maxWidth) return s;
		String dots = "...";
		int end = s.length();
		while (end > 0 && width(s.substring(0, end) + dots) > maxWidth) end--;
		return s.substring(0, end) + dots;
	}
}
