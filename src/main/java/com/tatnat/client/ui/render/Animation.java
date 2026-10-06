package com.tatnat.client.ui.render;

/**
 * A value that glides to its target over a fixed time using {@link Easing#inOutCubic}.
 *
 * Time-based rather than frame-based, so a toggle takes 150ms whether the game runs at 30 or 500
 * FPS. Retargeting mid-animation starts from wherever the value currently is, so rapid clicking
 * never makes anything jump.
 */
public final class Animation {
	private final long durationMs;
	private float from, to;
	private long start;

	public Animation(long durationMs, float initial) {
		this.durationMs = durationMs;
		this.from = this.to = initial;
		this.start = 0;
	}

	public void animateTo(float target) {
		if (target == to) return;
		from = get();
		to = target;
		start = System.currentTimeMillis();
	}

	/** Jumps straight to {@code value} with no animation. */
	public void snap(float value) {
		from = to = value;
		start = 0;
	}

	public float get() {
		// Performance tab: with menu animations off everything lands straight away.
		if (!com.tatnat.client.modules.Performance.animations()) return to;
		long elapsed = System.currentTimeMillis() - start;
		if (elapsed >= durationMs) return to;
		float t = elapsed / (float) durationMs;
		return from + (to - from) * Easing.inOutCubic(t);
	}

	public float target() {
		return to;
	}

	public boolean isDone() {
		return !com.tatnat.client.modules.Performance.animations() || System.currentTimeMillis() - start >= durationMs;
	}
}
