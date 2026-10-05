package com.tatnat.client.util;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;

/**
 * Counts in-game mouse clicks over the last second. Always registered (it's a few longs per
 * click), so Keystrokes and the CPS counter agree and are correct the moment they're enabled.
 */
public final class CpsTracker {
	public static final CpsTracker INSTANCE = new CpsTracker();

	private final long[][] clicks = {new long[256], new long[256]};
	private final int[] head = new int[2];
	private final int[] count = new int[2];

	private CpsTracker() {
	}

	@Subscribe
	public void onMouse(Events.MouseButton e) {
		if (!e.inGame || e.action != 1 || e.button > 1) return;
		int b = e.button;
		clicks[b][head[b]] = System.currentTimeMillis();
		head[b] = (head[b] + 1) % clicks[b].length;
		count[b] = Math.min(count[b] + 1, clicks[b].length);
	}

	/** Clicks of {@code button} (0 = left, 1 = right) during the last 1000ms. */
	public int cps(int button) {
		long cutoff = System.currentTimeMillis() - 1000;
		int n = 0;
		long[] ring = clicks[button];
		for (int i = 1; i <= count[button]; i++) {
			long t = ring[Math.floorMod(head[button] - i, ring.length)];
			if (t < cutoff) break;
			n++;
		}
		return n;
	}
}
