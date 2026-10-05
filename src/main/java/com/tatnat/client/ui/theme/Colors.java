package com.tatnat.client.ui.theme;

/** ARGB helpers: channel access, blending, HSV and the chroma (rainbow) cycle. */
public final class Colors {
	private Colors() {
	}

	/** One full rainbow cycle takes this long. */
	private static final double CHROMA_PERIOD_MS = 4000.0;

	public static int alpha(int argb) {
		return argb >>> 24;
	}

	public static int red(int argb) {
		return argb >> 16 & 0xFF;
	}

	public static int green(int argb) {
		return argb >> 8 & 0xFF;
	}

	public static int blue(int argb) {
		return argb & 0xFF;
	}

	public static int argb(int a, int r, int g, int b) {
		return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
	}

	public static int withAlpha(int argb, int alpha) {
		return (argb & 0x00FFFFFF) | (clamp(alpha) << 24);
	}

	/** Multiplies the existing alpha by {@code factor} (0..1) -- used for fades. */
	public static int fade(int argb, float factor) {
		return withAlpha(argb, Math.round(alpha(argb) * Math.max(0f, Math.min(1f, factor))));
	}

	/** Linear blend from {@code a} (t=0) to {@code b} (t=1), alpha included. */
	public static int lerp(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		return argb(
				Math.round(alpha(a) + (alpha(b) - alpha(a)) * t),
				Math.round(red(a) + (red(b) - red(a)) * t),
				Math.round(green(a) + (green(b) - green(a)) * t),
				Math.round(blue(a) + (blue(b) - blue(a)) * t));
	}

	/** Brightens (factor > 1) or darkens (factor < 1) the RGB channels. */
	public static int shade(int argb, float factor) {
		return argb(alpha(argb), Math.round(red(argb) * factor), Math.round(green(argb) * factor), Math.round(blue(argb) * factor));
	}

	/** h, s, v in 0..1 -> opaque ARGB. */
	public static int hsv(float h, float s, float v) {
		h = (h % 1f + 1f) % 1f;
		int i = (int) (h * 6f);
		float f = h * 6f - i;
		float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
		float r, g, b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return argb(255, Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
	}

	/** ARGB -> {h, s, v} in 0..1. */
	public static float[] toHsv(int argb) {
		float r = red(argb) / 255f, g = green(argb) / 255f, b = blue(argb) / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h;
		if (d == 0) h = 0;
		else if (max == r) h = ((g - b) / d % 6f) / 6f;
		else if (max == g) h = ((b - r) / d + 2f) / 6f;
		else h = ((r - g) / d + 4f) / 6f;
		if (h < 0) h += 1f;
		return new float[] {h, max == 0 ? 0 : d / max, max};
	}

	/**
	 * The current rainbow colour. {@code offset} is in "cycles" -- pass e.g. x / 200.0 so a row
	 * of elements shows a travelling wave instead of all flashing the same colour.
	 */
	public static int chroma(double offset) {
		double t = (System.currentTimeMillis() % (long) CHROMA_PERIOD_MS) / CHROMA_PERIOD_MS;
		return hsv((float) (t - offset), 0.75f, 1f);
	}

	private static int clamp(int c) {
		return Math.max(0, Math.min(255, c));
	}
}
