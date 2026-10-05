package com.tatnat.client.modules;

import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;

import com.tatnat.client.platform.Gfx;

/**
 * A HUD element that shows a single line like {@code [FPS: 240]}. Handles the shared settings
 * (colour, chroma, background, shadow, brackets) so each counter only supplies its text.
 */
public abstract class TextHudModule extends HudModule {
	public final ColorSetting textColor = add(new ColorSetting("Text Color", "Colour of the text", 0xFFFFFFFF, true));
	public final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the text", true));
	public final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the text", true));
	public final BooleanSetting brackets = add(new BooleanSetting("Brackets", "Wrap the text in [ ], like [FPS: 240]", false));

	/** Fixed box width so the element doesn't jitter as numbers change. 0 = fit the text. */
	private static final int PAD_X = 5;
	private static final int BOX_H = 16;

	protected TextHudModule(String name, String description, boolean enabledByDefault, double x, double y) {
		super(name, description, enabledByDefault, x, y);
	}

	/** The label part, e.g. "FPS". Empty for elements that show only a value (like Clock). */
	protected abstract String label();

	/** The value part, e.g. "240". */
	protected abstract String value(boolean preview);

	/** Colour for the value, or 0 to use the text colour (lets FPS/Ping colour-code themselves). */
	protected int valueColor(boolean preview) {
		return 0;
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		String label = label();
		String value = value(preview);
		String open = brackets.on() ? "[" : "";
		String close = brackets.on() ? "]" : "";
		String head = open + (label.isEmpty() ? "" : label + ": ");
		String full = head + value + close;

		int textW = g.mcTextWidth(full, false);
		int w = background.on() ? textW + PAD_X * 2 : textW;
		int h = background.on() ? BOX_H : 9;
		if (background.on()) g.rect(0, 0, w, h, 0x6F000000);

		int x = background.on() ? PAD_X : 0;
		int y = background.on() ? (BOX_H - 8) / 2 : 0;
		int base = textColor.color(0);
		int vc = valueColor(preview);
		g.mcText(head, x, y, base, shadow.on(), false);
		x += g.mcTextWidth(head, false);
		g.mcText(value, x, y, vc != 0 ? vc : textColor.color(0.15), shadow.on(), false);
		x += g.mcTextWidth(value, false);
		if (!close.isEmpty()) g.mcText(close, x, y, textColor.color(0.3), shadow.on(), false);
		return size(w, h);
	}
}
