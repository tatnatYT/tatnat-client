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
	public static final int TEXT_MUTED = 0xFFA9A9B0;
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
	public static final int ICON = 0xFFE5323E; // mod icons: the accent red
	/** Mod icons while the mod is off: a dark red. */
	public static final int ICON_OFF = 0xFF6A2328;

	/** The menu window: a deep charcoal gradient, barely see-through. */
	public static final int WINDOW_TOP = 0xF61A1C22;
	public static final int WINDOW_BOTTOM = 0xF60D0E11;
	/** The sidebar strip inside the window (drawn over the window). */
	public static final int RAIL = 0x40000000;
	/** Hairline border around the window and on cards. */
	public static final int BORDER = 0x1CFFFFFF;
	public static final int BORDER_HOVER = 0x3DFFFFFF;
	/** Cards and rows: a subtle top-lit gradient. */
	public static final int CARD_TOP = 0xFF1F2128;
	public static final int CARD_BOTTOM = 0xFF17181D;
	public static final int CARD_HOVER_TOP = 0xFF272A32;
	public static final int CARD_HOVER_BOTTOM = 0xFF1C1E24;
	/** Enabled cards: lit red from the top. */
	public static final int CARD_ON_TOP = 0xFF3A1C22;
	public static final int CARD_ON_BOTTOM = 0xFF1C1619;
	/** The accent at low strength: active nav item, selected pills, the wash on enabled cards. */
	public static final int ACCENT_SOFT = 0x2EE5323E;
	/** Glow behind enabled icons and switches. */
	public static final int ACCENT_GLOW = 0x55E5323E;
	/** Lighter accent for the top of gradients. */
	public static final int ACCENT_LIGHT = 0xFFF0505A;

	/** Corner radii in design pixels. */
	public static final int RADIUS_SMALL = 4;
	public static final int RADIUS = 7;
	public static final int RADIUS_LARGE = 10;
	public static final int RADIUS_CARD = 12;
	public static final int RADIUS_WINDOW = 18;
}
