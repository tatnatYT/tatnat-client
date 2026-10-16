package com.tatnat.client.plus;

/**
 * The badge in front of the names of people playing with Eclipse (name tags and the tab list).
 * 1.14+ draws it with the client font's own glyphs: a crescent tinted in the name's colour, and a purple
 * crescent with a plus for Eclipse Plus. Older versions can't add glyphs, so they use a plain character.
 */
public final class EclipseBadge {
	/** Font glyphs (assets/minecraft/font/default.json). */
	public static final String PLAIN = "", PLUS = "";

	private EclipseBadge() {
	}

	/** The badge glyph as formatted text, for 1.14 - 1.15 name tags, which are plain strings ("" for none). */
	public static String glyphText(int kind) {
		if (kind == EclipseNet.PLUS) return "§f" + PLUS + "§r ";
		if (kind == EclipseNet.ECLIPSE) return PLAIN + " ";
		return "";
	}

	/** The badge as legacy formatted text for 1.8.9 - 1.12.2 ("" for none). */
	public static String legacy(int kind) {
		if (kind == EclipseNet.PLUS) return "§5☾§d+§r ";
		if (kind == EclipseNet.ECLIPSE) return "☾ ";
		return "";
	}
}
