package com.tatnat.client.plus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.impl.cosmetic.PlusCapes;

/**
 * The latest picture of each Plus cape. Capes are painted on background threads, and only while
 * someone wearing them is being drawn: animated ones {@link PlusCapes#FPS} times a second, the single
 * colours once. The render thread just picks up the newest finished frame.
 */
public final class CapeFrames {
	/**
	 * A finished frame: W x H ARGB pixels and a number that changes with every new frame. Copy what you
	 * need right away: the buffer is painted over again two frames later.
	 */
	public static final class Frame {
		public final int[] pixels;
		public final int revision;

		Frame(int[] pixels, int revision) {
			this.pixels = pixels;
			this.revision = revision;
		}
	}

	private static final Map<String, Frame> latest = new ConcurrentHashMap<>();
	private static final Map<String, Long> wanted = new ConcurrentHashMap<>();
	private static final Map<String, Boolean> busy = new ConcurrentHashMap<>();
	/** Three buffers per cape, used in turn: one being shown, one being painted, one spare. */
	private static final Map<String, int[][]> buffers = new ConcurrentHashMap<>();
	private static final Map<String, Integer> nextBuffer = new ConcurrentHashMap<>();
	private static volatile ExecutorService painters;
	private static int revisions;

	private CapeFrames() {
	}

	/** The newest frame of cape {@code id} (null until the first one is painted), and keep it animating. */
	public static Frame get(String id) {
		if (id == null || !PlusCapes.isPlus(id)) return null;
		wanted.put(id, System.currentTimeMillis());
		if (painters == null) start();
		return latest.get(id);
	}

	private static synchronized void start() {
		if (painters != null) return;
		// Each frame is itself split across cores by PlusCapes; this only queues whole frames.
		painters = Executors.newFixedThreadPool(2, r -> {
			Thread t = new Thread(r, "Eclipse capes");
			t.setDaemon(true);
			t.setPriority(Thread.MIN_PRIORITY + 1);
			return t;
		});
		Thread clock = new Thread(CapeFrames::tick, "Eclipse cape clock");
		clock.setDaemon(true);
		clock.start();
	}

	private static void tick() {
		long frame = 1000L / PlusCapes.FPS;
		while (true) {
			long now = System.currentTimeMillis();
			for (Map.Entry<String, Long> e : wanted.entrySet()) {
				String id = e.getKey();
				// Nobody wearing it on screen for 3 seconds: stop painting it.
				if (now - e.getValue() > 3000) {
					wanted.remove(id);
					continue;
				}
				if (latest.containsKey(id) && !PlusCapes.animated(id)) continue;
				if (busy.putIfAbsent(id, Boolean.TRUE) != null) continue; // still painting the last frame: skip one
				painters.execute(() -> paint(id));
			}
			try {
				Thread.sleep(Math.max(5, frame - (System.currentTimeMillis() - now)));
			} catch (InterruptedException ex) {
				return;
			}
		}
	}

	private static void paint(String id) {
		try {
			int[][] ring = buffers.computeIfAbsent(id, k -> new int[3][]);
			int n = nextBuffer.getOrDefault(id, 0);
			boolean fresh = ring[n] == null;
			if (fresh) ring[n] = new int[PlusCapes.W * PlusCapes.H];
			int[] px = ring[n];
			PlusCapes.draw(id, System.currentTimeMillis(), px, fresh);
			nextBuffer.put(id, (n + 1) % 3);
			int rev;
			synchronized (CapeFrames.class) {
				rev = ++revisions;
			}
			latest.put(id, new Frame(px, rev));
		} catch (Throwable t) {
			TatnatClient.LOG.warn("Could not paint cape {}: {}", id, t.toString());
		} finally {
			busy.remove(id);
		}
	}
}
