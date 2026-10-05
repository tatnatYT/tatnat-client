package com.tatnat.client.ui.theme;

/**
 * The menu palette, matched to Feather's mod menu: near-black translucent panels, dark grey
 * cards and a red accent. Every colour in the menus comes from here so the look can be retuned
 * in one place. Values are ARGB.
 */
public final class Theme {
	private Theme() {
	}

	/** Main panel and sidebar (slightly see-through, the world shows faintly behind). */
	public static final int BACKGROUND = 0xEB131315;
	/** Mod cards, setting rows, inputs. */
	public static final int PANEL = 0xFF202023;
	/** Hovered card / row. */
	public static final int HOVER = 0xFF2A2A2E;
	/** Accent red (tab, active toggles, selections). */
	public static final int ACCENT = 0xFFE5323E;
	/** Text on top of the accent. */
	public static final int ON_ACCENT = 0xFFFFFFFF;
	/** Primary text. */
	public static final int TEXT = 0xFFE6E6E6;
	/** Secondary text and inactive icons. */
	public static final int TEXT_MUTED = 0xFF8E8E93;
	/** On / success. */
	public static final int SUCCESS = 0xFF4CAF50;
	/** Off / danger. */
	public static final int DANGER = 0xFFEF5350;

	/** HUD editor snap guides. */
	public static final int GUIDE = 0xFFFFFF00;
	/** Thin separators. */
	public static final int DIVIDER = 0xFF2C2C30;
	/** Off toggle track, slider track, unselected pills. */
	public static final int TRACK = 0xFF35353A;
	/** Knob of an off toggle. */
	public static final int KNOB_OFF = 0xFF6B6B70;
	/** Large mod icons on cards. */
	public static final int ICON = 0xFFD8D8D8;

	/** Corner radii in design pixels. */
	public static final int RADIUS_SMALL = 4;
	public static final int RADIUS = 7;
	public static final int RADIUS_LARGE = 10;
}
