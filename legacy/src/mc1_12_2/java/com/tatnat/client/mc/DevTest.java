package com.tatnat.client.mc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotUtils;

/**
 * Development-only scripted test run. Does nothing unless the game is started with
 * {@code -Dtatnat.devtest=<steps>} (only the dev Gradle run does that), so it never runs for
 * players. Each step is "wait N ticks, then do something"; screenshots land in
 * {@code run/screenshots}. The actual steps live in {@link DevTestSteps}.
 */
public final class DevTest {
	private static final class Step {
		final int delayTicks;
		final String name;
		final Runnable action;

		Step(int delayTicks, String name, Runnable action) {
			this.delayTicks = delayTicks;
			this.name = name;
			this.action = action;
		}
	}

	private static final List<Step> STEPS = new ArrayList<>();
	private static int index, wait = -1;
	private static boolean started;

	private DevTest() {
	}

	public static boolean enabled() {
		String p = System.getProperty("tatnat.devtest", "");
		return !p.isEmpty();
	}

	public static void init() {
		if (!enabled()) return;
		TatnatClient.LOG.info("[devtest] enabled: {}", System.getProperty("tatnat.devtest"));
		// "allcheck" is driven by the shared core (DevAllCheck); this class only joins the world.
		if (!"allcheck".equals(System.getProperty("tatnat.devtest"))) DevTestSteps.build(System.getProperty("tatnat.devtest"));
		TatnatClient.EVENTS.register(new DevTest());
	}

	public static void step(int delayTicks, String name, Runnable action) {
		STEPS.add(new Step(delayTicks, name, action));
	}

	public static void shot(String name) {
		MinecraftClient mc = MinecraftClient.getInstance();
		TatnatClient.LOG.info("[devtest] screenshot {}: {}", name,
				ScreenshotUtils.method_12154(mc.runDirectory, "devtest-" + name + ".png", mc.width, mc.height, mc.getFramebuffer()).asUnformattedString());
	}

	private int idle;
	private boolean joining;

	@Subscribe
	public void onTick(Events.Tick e) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!started) {
			// Begin once we're in a world.
			if (mc.player == null || mc.world == null) {
				String join = System.getProperty("tatnat.join");
				if (join != null && !joining && mc.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen) {
					joining = true;
					String[] hp = join.split(":");
					mc.setScreen(new net.minecraft.client.gui.screen.ConnectScreen(mc.currentScreen, mc, hp[0], Integer.parseInt(hp[1])));
				}
				if (++idle % 100 == 0) TatnatClient.LOG.info("[devtest] waiting for a world, screen: {}", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName());
				return;
			}
			started = true;
			wait = STEPS.isEmpty() ? -1 : STEPS.get(0).delayTicks;
		}
		if (index >= STEPS.size()) return;
		if (wait-- > 0) return;
		Step s = STEPS.get(index++);
		TatnatClient.LOG.info("[devtest] step {}: {}", index, s.name);
		try {
			s.action.run();
		} catch (Throwable t) {
			TatnatClient.LOG.error("[devtest] step " + s.name + " FAILED", t);
		}
		wait = index < STEPS.size() ? STEPS.get(index).delayTicks : -1;
	}

	/** Where screenshots go, for the log. */
	public static File dir() {
		return new File(MinecraftClient.getInstance().runDirectory, "screenshots");
	}
}
