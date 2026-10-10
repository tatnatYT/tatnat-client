package com.tatnat.client.modules.impl.hud;

import java.util.HashMap;
import java.util.Map;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.EntityInfo;

/**
 * Totem Pop Counter: counts how many Totems of Undying each player has popped (1.11+) and shows
 * it in red right after their name in their name tag ("Steve -3"). A player's count resets when
 * they die.
 */
public class TotemPops extends Module {
	private static TotemPops instance;
	private static final Map<String, Integer> POPS = new HashMap<>();

	private final BooleanSetting includeSelf = add(new BooleanSetting("Count Yourself", "Also count your own pops (seen in third person)", true));

	public TotemPops() {
		super("Totem Pop Counter", "Shows each player's totem pops in their name tag", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.TOTEMPOP;
		instance = this;
	}

	/** Called by the platform when an entity plays the totem animation. */
	public static void popped(String name, boolean self) {
		TotemPops m = instance;
		if (m == null || !m.isEnabled() || name == null || name.isEmpty()) return;
		if (self && !m.includeSelf.on()) return;
		POPS.merge(name, 1, Integer::sum);
	}

	/** What to add after a player's name tag ("-3"), or null. Asked by the name tag mixins. */
	public static String tag(String name) {
		TotemPops m = instance;
		if (m == null || !m.isEnabled() || name == null) return null;
		Integer n = POPS.get(name);
		return n == null ? null : "-" + n;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) {
			POPS.clear();
			return;
		}
		if (POPS.isEmpty()) return;
		// Dead players start over.
		for (EntityInfo p : game().entities(256)) {
			if (p.kind == EntityInfo.Kind.PLAYER && p.health <= 0) POPS.remove(p.name);
		}
		if (game().health() <= 0) POPS.remove(game().playerName());
	}

	@Override
	protected void onDisable() {
		POPS.clear();
	}
}
