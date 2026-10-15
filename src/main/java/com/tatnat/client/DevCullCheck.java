package com.tatnat.client;

import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.perf.Optimizations;

/**
 * Dev-only check of the built-in culling ({@code -Dtatnat.devtest=allcheck-cull}): a stone wall with 
 * pigs and 40 chests behind it. Logs FPS and how many draws were skipped with culling on and off,
 * then with the wall turned to glass (nothing may be skipped) and with the wall gone. Screenshots
 * each stage. Ends with "[cullcheck] done".
 */
public final class DevCullCheck {
	private int ticks = -1, step;
	private final List<Runnable> steps = new ArrayList<>();
	private final List<Integer> waits = new ArrayList<>();
	private final List<Integer> fps = new ArrayList<>();

	public static void init() {
		if ("allcheck-cull".equals(System.getProperty("tatnat.devtest"))) TatnatClient.EVENTS.register(new DevCullCheck());
	}

	private DevCullCheck() {
		add(60, () -> {
			for (String c : new String[] {"time set 6000", "weather clear", "gamemode creative", "gamerule doMobSpawning false",
					"tp @s 0 120 0 0 0"}) cmd(c);
		});
		add(40, () -> {
			for (String c : new String[] {"fill -16 115 -4 16 135 20 air", "fill -16 115 21 16 135 44 air", "fill -16 114 -4 16 114 44 stone"}) cmd(c);
		});
		add(20, () -> cmd("fill -16 115 6 16 135 6 stone")); // the wall, 6 blocks in front (you look south, +Z)
		for (int row = 0; row < 20; row++) {
			final int r = row;
			add(2, () -> {
				for (int i = 0; i < 15; i++) cmd("summon pig " + (-14 + i * 2) + " 115 " + (12 + r) + " {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
			});
		}
		add(2, () -> {
			for (int i = 0; i < 40; i++) cmd("setblock " + (-10 + (i % 10) * 2) + " " + (115 + i / 10) + " 10 chest");
			cmd("tp @s 0 115 0 0 0");
			cmd("effect give @s slowness 9999 255 true");
		});
		add(100, () -> { sample("warmup"); stay(); });
		add(100, () -> sample("culling on (wall)"));
		add(1, () -> shot("cull-01-wall-on"));
		add(2, () -> Optimizations.devOff = true);
		add(100, () -> { sample("warmup off"); stay(); });
		add(100, () -> sample("culling off (wall)"));
		add(1, () -> shot("cull-02-wall-off"));
		add(2, () -> {
			Optimizations.devOff = false;
			cmd("fill -16 115 6 16 135 6 glass");
		});
		add(100, () -> { sample("warmup glass"); stay(); });
		add(100, () -> sample("glass wall (must skip 0)"));
		add(1, () -> shot("cull-03-glass"));
		add(2, () -> { cmd("fill -16 115 6 16 135 6 air"); stay(); });
		add(100, () -> sample("no wall (must skip 0)"));
		add(1, () -> shot("cull-04-open"));
		add(2, () -> { cmd("fill -16 115 6 16 135 6 stone"); stay(); });
		add(100, () -> sample("wall back"));
		add(5, () -> log("done"));
		add(40, () -> System.exit(0));
	}

	private void sample(String what) {
		long[] s = Optimizations.takeStats();
		log(what + ": fps=" + TatnatClient.game().fps() + " skippedEntityDraws=" + s[0] + " skippedBlockEntityDraws=" + s[1]
				+ " checkTimeMs=" + s[2] / 1000.0);
	}

	/** Back on the spot, looking straight at the wall (the same view for every sample). */
	private static void stay() {
		cmd("tp @s 0 115 0 0 0");
	}

	private void add(int wait, Runnable r) {
		waits.add(wait);
		steps.add(r);
	}

	private static void cmd(String c) {
		try {
			TatnatClient.game().sendCommand(c);
		} catch (Throwable t) {
			TatnatClient.LOG.warn("[cullcheck] command {} failed: {}", c, t.toString());
		}
	}

	private static void log(String s) {
		TatnatClient.LOG.info("[cullcheck] {}", s);
	}

	private static void shot(String name) {
		try {
			java.lang.reflect.Method m = Class.forName("com.tatnat.client.mc.DevTest").getMethod("shot", String.class);
			m.setAccessible(true);
			m.invoke(null, name);
		} catch (Throwable t) {
			TatnatClient.LOG.warn("[cullcheck] screenshot failed: {}", t.toString());
		}
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (ticks < 0) {
			if (!TatnatClient.game().inWorld()) return;
			ticks = 0;
		}
		if (step >= steps.size()) return;
		if (++ticks < waits.get(step)) return;
		ticks = 0;
		Runnable r = steps.get(step++);
		try {
			r.run();
		} catch (Throwable t) {
			TatnatClient.LOG.error("[cullcheck] step " + step + " FAILED", t);
		}
	}
}
