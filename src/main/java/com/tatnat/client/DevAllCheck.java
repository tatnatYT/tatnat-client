package com.tatnat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.ModuleManager;

/**
 * Dev-only whole-client check ({@code -Dtatnat.devtest=allcheck}), the same on every version and
 * loader: sets up a night scene with TNT, a zombie and dropped diamonds, turns every mod on,
 * screenshots the world and the menus, flips every mod off and on again, and logs any failure.
 * Ends with "[allcheck] done".
 */
public final class DevAllCheck {
	/** Mods that take over the camera or the whole screen; checked on their own at the end. */
	private static final Set<String> LATER = new HashSet<>(Arrays.asList("Freecam", "Autohide HUD", "UI Scaling", "Color Saturation"));

	/** "allcheck:Name|Name" limits the run to those mods (to find which one causes a problem). */
	private static final Set<String> ONLY = new HashSet<>();
	static {
		String p = System.getProperty("tatnat.devtest", "");
		if (p.startsWith("allcheck:")) ONLY.addAll(Arrays.asList(p.substring(9).split("[|]")));
	}

	private static boolean skip(Module m) {
		return LATER.contains(m.name) || !ONLY.isEmpty() && !ONLY.contains(m.name);
	}

	private int ticks = -1, step;
	private final List<Runnable> steps = new ArrayList<>();
	private final List<Integer> waits = new ArrayList<>();

	public static void init() {
		if (System.getProperty("tatnat.devtest", "").startsWith("allcheck")) TatnatClient.EVENTS.register(new DevAllCheck());
	}

	private DevAllCheck() {
		add(60, () -> {
			for (String c : new String[] {"time set 18000", "weather clear", "gamemode creative"}) cmd(c);
			// Entity names differ between versions; the wrong ones just fail in chat.
			cmd("summon tnt ~3 ~ ~3");
			cmd("summon PrimedTnt ~3 ~ ~3");
			cmd("summon zombie ~5 ~ ~1");
			cmd("summon Zombie ~5 ~ ~1");
			cmd("summon item ~2 ~1 ~ {Item:{id:\"minecraft:diamond\",count:3}}");
			cmd("summon item ~2 ~1 ~ {Item:{id:\"minecraft:diamond\",Count:3b}}");
			cmd("summon Item ~2 ~1 ~ {Item:{id:minecraft:diamond,Count:3}}");
			log("scene set up");
		});
		add(20, () -> {
			int n = 0;
			for (Module m : ModuleManager.get().all()) {
				if (skip(m)) continue;
				try {
					m.setEnabled(true);
					n++;
				} catch (Throwable t) {
					TatnatClient.LOG.error("[allcheck] enabling " + m.name + " FAILED", t);
				}
			}
			log("enabled " + n + " mods");
		});
		add(40, () -> {
			for (com.tatnat.client.platform.EntityInfo en : TatnatClient.game().entities(32))
				log("entity " + en.kind + " '" + en.name + "' health " + en.health + "/" + en.maxHealth + " ticks " + en.ticks);
			java.util.Map<String, List<String>> byKey = new java.util.HashMap<>();
			for (String[] k : TatnatClient.game().keyMappings()) byKey.computeIfAbsent(k[3], x -> new ArrayList<>()).add(k[0]);
			for (java.util.Map.Entry<String, List<String>> en : byKey.entrySet())
				if (en.getValue().size() > 1) log("key " + en.getKey() + " used by " + en.getValue());
			shot("allcheck-01-world");
		});
		add(5, () -> TatnatClient.game().openScreen(new com.tatnat.client.ui.clickgui.ClickGuiScreen()));
		add(25, () -> shot("allcheck-02-menu"));
		add(2, () -> {
			com.tatnat.client.ui.clickgui.ClickGuiScreen s = new com.tatnat.client.ui.clickgui.ClickGuiScreen();
			s.devShow(false, true);
			TatnatClient.game().openScreen(s);
		});
		add(25, () -> shot("allcheck-02b-menu-list"));
		add(2, () -> {
			com.tatnat.client.ui.clickgui.ClickGuiScreen s = new com.tatnat.client.ui.clickgui.ClickGuiScreen();
			for (Module m : ModuleManager.get().all()) if (m.name.equals("Keystrokes")) s.openSettingsFor(m);
			TatnatClient.game().openScreen(s);
		});
		add(25, () -> shot("allcheck-02c-mod-settings"));
		add(2, () -> {
			com.tatnat.client.ui.clickgui.ClickGuiScreen s = new com.tatnat.client.ui.clickgui.ClickGuiScreen();
			s.devShow(true, false);
			TatnatClient.game().openScreen(s);
		});
		add(25, () -> shot("allcheck-02d-settings"));
		add(2, () -> {
			com.tatnat.client.ui.clickgui.ClickGuiScreen s = new com.tatnat.client.ui.clickgui.ClickGuiScreen();
			s.devShow(false, false);
			TatnatClient.game().openScreen(s);
		});
		add(2, () -> TatnatClient.game().openScreen(new com.tatnat.client.ui.clickgui.KeybindScreen(null)));
		add(15, () -> shot("allcheck-03-keybinds"));
		add(2, () -> TatnatClient.game().openScreen(new com.tatnat.client.ui.clickgui.PackScreen(null)));
		add(15, () -> shot("allcheck-04-packs"));
		add(2, () -> TatnatClient.game().closeScreen());
		// Vanilla screens, where the platform offers a helper (DevTestSteps.screens).
		add(5, () -> screens(0));
		add(10, () -> screens(1));
		add(10, () -> shot("allcheck-08-inventory-shulker"));
		add(2, () -> screens(2));
		add(20, () -> shot("allcheck-09-advancements"));
		add(2, () -> screens(3));
		add(5, () -> {
			// Every mod off and on again: exercises each onDisable / onEnable.
			for (Module m : ModuleManager.get().all()) {
				if (skip(m)) continue;
				try {
					m.setEnabled(false);
					m.setEnabled(true);
				} catch (Throwable t) {
					TatnatClient.LOG.error("[allcheck] toggling " + m.name + " FAILED", t);
				}
			}
			log("toggled every mod");
		});
		add(20, () -> shot("allcheck-05-after-toggle"));
		// Totem Pop Counter: pop one of our own totems (1.19.4+ has /damage).
		add(2, () -> {
			cmd("gamemode survival");
			cmd("item replace entity @s weapon.offhand with totem_of_undying");
		});
		add(10, () -> cmd("damage @s 100 minecraft:generic"));
		add(30, () -> shot("allcheck-10-totem-pop"));
		add(2, () -> cmd("gamemode creative"));
		add(2, () -> later("UI Scaling", true));
		add(20, () -> shot("allcheck-06-ui-scaling"));
		add(2, () -> later("UI Scaling", false));
		add(2, () -> later("Color Saturation", true));
		add(20, () -> shot("allcheck-07-saturation"));
		add(2, () -> later("Color Saturation", false));
		// Motion Blur: a still view, then a shot halfway through a turn (should show a trail).
		add(2, () -> later("Motion Blur", true));
		add(20, () -> shot("allcheck-11-blur-still"));
		for (int i = 0; i < 6; i++) add(1, () -> cmd("tp @s ~ ~ ~ ~15 ~"));
		add(1, () -> shot("allcheck-12-blur-turning"));
		add(20, () -> shot("allcheck-13-blur-after"));
		add(2, () -> later("Freecam", true));
		add(10, () -> later("Freecam", false));
		add(5, () -> {
			for (Module m : ModuleManager.get().all()) m.setEnabled(false);
			log("done");
		});
		// Dev runs only: quit so the devtest script can finish (screenshots are written by then).
		add(40, () -> System.exit(0));
	}

	private void add(int wait, Runnable r) {
		waits.add(wait);
		steps.add(r);
	}

	private static void later(String name, boolean on) {
		for (Module m : ModuleManager.get().all()) {
			if (!m.name.equals(name)) continue;
			try {
				m.setEnabled(on);
			} catch (Throwable t) {
				TatnatClient.LOG.error("[allcheck] " + name + " FAILED", t);
			}
		}
	}

	private static void cmd(String c) {
		try {
			TatnatClient.game().sendCommand(c);
		} catch (Throwable t) {
			TatnatClient.LOG.warn("[allcheck] command {} failed: {}", c, t.toString());
		}
	}

	private static void log(String s) {
		TatnatClient.LOG.info("[allcheck] {}", s);
	}

	private static void screens(int stage) {
		try {
			java.lang.reflect.Method m = Class.forName("com.tatnat.client.mc.DevTestSteps").getMethod("screens", int.class);
			m.setAccessible(true);
			m.invoke(null, stage);
		} catch (NoSuchMethodException e) {
			if (stage == 0) log("no vanilla-screen helper on this version");
		} catch (Throwable t) {
			TatnatClient.LOG.error("[allcheck] screens " + stage + " FAILED", t);
		}
	}

	/** The platform's DevTest.shot (each version has one). */
	private static void shot(String name) {
		try {
			java.lang.reflect.Method m = Class.forName("com.tatnat.client.mc.DevTest").getMethod("shot", String.class);
			m.setAccessible(true);
			m.invoke(null, name);
		} catch (Throwable t) {
			TatnatClient.LOG.warn("[allcheck] screenshot failed: {}", t.toString());
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
			TatnatClient.LOG.error("[allcheck] step " + step + " FAILED", t);
		}
	}
}
