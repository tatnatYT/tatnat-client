package com.tatnat.client.platform;

/**
 * Drawing, as provided by each Minecraft version's platform layer.
 *
 * Coordinates are in whatever space the current transform says: the HUD starts in GUI-scaled
 * units, and menus switch to real pixels with {@code RenderUtils.beginPixels}. Everything the
 * shared code draws goes through here, so the menu, HUD editor and HUD mods look identical on
 * every version.
 */
public interface Gfx {
	// ------------------------------------------------------------ shapes

	/** Solid rectangle from (x1, y1) to (x2, y2), ARGB. */
	void rect(int x1, int y1, int x2, int y2, int argb);

	/**
	 * {@code n} solid rectangles packed as {x1, y1, x2, y2, argb} in {@code data}, ideally in one
	 * draw call (see RectBatch). The default draws them one by one.
	 */
	default void rects(int[] data, int n) {
		for (int i = 0; i < n; i++) rect(data[i * 5], data[i * 5 + 1], data[i * 5 + 2], data[i * 5 + 3], data[i * 5 + 4]);
	}

	/**
	 * True when {@link #mask} draws from a cached texture (one quad per shape). Platforms without
	 * it draw anti-aliased shapes from many small fills instead.
	 */
	default boolean masks() {
		return false;
	}

	/**
	 * Draws a coverage mask ({@code w * h} alpha bytes, row by row) tinted with {@code argb}, its
	 * top-left at (x, y). {@code key} names the mask; {@code alpha} is only called the first time.
	 */
	default void mask(String key, int w, int h, java.util.function.Supplier<byte[]> alpha, int x, int y, int argb) {
	}

	/** Top-to-bottom gradient. */
	void gradient(int x1, int y1, int x2, int y2, int top, int bottom);

	// ------------------------------------------------------------ transform / clip

	void push();

	void pop();

	void translate(float x, float y);

	void scale(float x, float y);

	/** Clips drawing to the rectangle (in the current transform's coordinates). Nestable. */
	void scissor(int x1, int y1, int x2, int y2);

	void endScissor();

	// ------------------------------------------------------------ menu text (Inter, anti-aliased)

	/**
	 * Draws menu text with the em box's top-left at (x, y). {@code weight} is 500 or 700;
	 * {@code px} is the rasterised pixel size (8..56). Expects pixel space.
	 */
	void uiText(int weight, int px, String s, int x, int y, int argb);

	int uiTextWidth(int weight, int px, String s);

	// ------------------------------------------------------------ HUD text (Minecraft's own font)

	void mcText(String s, int x, int y, int argb, boolean shadow, boolean bold);

	int mcTextWidth(String s, boolean bold);

	// ------------------------------------------------------------ images

	/** The mod's logo (the tatnat head), {@code size} x {@code size}. */
	void logo(int x, int y, int size, int argb);

	/** A game item (opaque platform handle from {@link Game}) at 16x16, with durability bar. */
	void item(Object stack, int x, int y);

	/** A potion effect's icon (opaque platform handle from {@link Game}). */
	void effectIcon(Object effect, int x, int y, int size);
}
