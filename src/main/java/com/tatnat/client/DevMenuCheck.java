package com.tatnat.client;

import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.ui.menu.MainMenu;

/**
 * Dev-only check of the Eclipse main menu ({@code -Dtatnat.devtest=allcheck-menu}): opens it, swaps
 * the background (screenshot mid-swap and after), then clicks every button, screenshotting the
 * screen each one opens. Ends by clicking Quit Game. Logs "[menucheck] ..." for each step.
 */
public final class DevMenuCheck {
	private int ticks = -1, step;
	private final List<Runnable> steps = new ArrayList<>();
	private final List<Integer> waits = new ArrayList<>();

	public static void init() {
		if ("allcheck-menu".equals(System.getProperty("tatnat.devtest"))) TatnatClient.EVENTS.register(new DevMenuCheck());
	}

	private DevMenuCheck() {
		add(80, () -> TatnatClient.game().openScreen(new MainMenu()));
		add(40, () -> shot("menu-01-open"));
		add(2, () -> MainMenu.last.keyPressed(com.tatnat.client.util.KeyCodes.RIGHT, false));
		add(7, () -> shot("menu-02-swapping"));
		add(30, () -> shot("menu-03-swapped"));
		for (String[] b : new String[][] {{"SINGLEPLAYER", "menu-04-singleplayer"}, {"MULTIPLAYER", "menu-05-multiplayer"},
				{"Options", "menu-06-options"}, {"Accounts", "menu-07-accounts"}, {"ECLIPSE MODS", "menu-08-eclipse-mods"}}) {
			add(3, () -> TatnatClient.game().openScreen(MainMenu.last == null ? new MainMenu() : MainMenu.last));
			add(10, () -> log("click " + b[0] + ": " + MainMenu.last.devClick(b[0])));
			add(25, () -> shot(b[1]));
		}
		add(3, () -> TatnatClient.game().openScreen(MainMenu.last));
		add(10, () -> log("done"));
		add(5, () -> log("click Quit Game: " + MainMenu.last.devClick("Quit Game")));
		add(200, () -> System.exit(0));
	}

	private void add(int wait, Runnable r) {
		waits.add(wait);
		steps.add(r);
	}

	private static void log(String s) {
		TatnatClient.LOG.info("[menucheck] {}", s);
	}

	private static void shot(String name) {
		try {
			java.lang.reflect.Method m = Class.forName("com.tatnat.client.mc.DevTest").getMethod("shot", String.class);
			m.setAccessible(true);
			m.invoke(null, name);
		} catch (Throwable t) {
			TatnatClient.LOG.warn("[menucheck] screenshot failed: {}", t.toString());
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
			TatnatClient.LOG.error("[menucheck] step " + step + " FAILED", t);
		}
	}
}
