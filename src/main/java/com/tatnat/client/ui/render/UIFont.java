package com.tatnat.client.ui.render;

import java.util.HashMap;
import java.util.Map;

import com.tatnat.client.ui.theme.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * The menu typeface: Inter, rasterised by Minecraft's own TrueType loader (stb_truetype, fully
 * anti-aliased).
 *
 * Menus are laid out for a 1920x1080 "design" screen and scaled by {@link Ui#s}. Scaling a
 * rasterised font would blur it, so instead every whole pixel size from 8 to 56 has its own font
 * definition (assets/tatnatclient/font/w500_N.json, w700_N.json, oversample 1): each style below
 * picks the size that matches the current scale and draws it 1:1 on screen pixels. Always draw
 * in pixel space (see {@link RenderUtils#beginPixels}).
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
	private static final Map<Integer, Style> STYLES = new HashMap<>();

	UIFont(int weight, int designSize) {
		this.weight = weight;
		this.designSize = designSize;
	}

	/** Pixel size at the current UI scale. */
	public int size() {
		return Math.max(8, Math.min(56, Math.round(designSize * Ui.s)));
	}

	private Style style() {
		int px = size();
		return STYLES.computeIfAbsent(weight * 100 + px, k -> Style.EMPTY.withFont(
				new FontDescription.Resource(Identifier.fromNamespaceAndPath("tatnatclient", "w" + weight + "_" + px))));
	}

	public Component text(String s) {
		return Component.literal(s).withStyle(style());
	}

	public int width(String s) {
		return Minecraft.getInstance().font.width(text(s));
	}

	/**
	 * Draws {@code s} with its em box's top-left at (x, y), in pixel space.
	 */
	public void draw(GuiGraphics g, String s, int x, int y, int color) {
		int c = Colors.fade(color, RenderUtils.alpha);
		// Near-transparent text is skipped: Minecraft treats very low alpha as fully opaque.
		if (Colors.alpha(c) < 8) return;
		// Minecraft puts every font's baseline 7px below the given y; move it to ~80% of the em box.
		g.drawString(Minecraft.getInstance().font, text(s), x, y + Math.round(size() * 0.8f) - 7, c, false);
	}

	/** Draws {@code s} vertically centred on {@code cy}. */
	public void drawMid(GuiGraphics g, String s, int x, int cy, int color) {
		draw(g, s, x, cy - size() / 2, color);
	}

	public void drawCentered(GuiGraphics g, String s, int cx, int y, int color) {
		draw(g, s, cx - width(s) / 2, y, color);
	}

	public void drawRight(GuiGraphics g, String s, int right, int y, int color) {
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
