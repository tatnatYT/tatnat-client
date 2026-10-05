package com.tatnat.client.ui.render;

/** Easing curves. All take and return 0..1. */
public final class Easing {
	private Easing() {
	}

	public static float linear(float t) {
		return t;
	}

	/** The default for every menu animation: slow start, fast middle, slow finish. */
	public static float inOutCubic(float t) {
		return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
	}

	public static float outCubic(float t) {
		return 1f - (float) Math.pow(1f - t, 3);
	}

	/** Overshoots slightly before settling -- used for little "pop" effects. */
	public static float outBack(float t) {
		float c1 = 1.70158f, c3 = c1 + 1f;
		return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
	}
}
