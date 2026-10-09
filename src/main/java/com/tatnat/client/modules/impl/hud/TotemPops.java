package com.tatnat.client.modules.impl.hud;

import java.util.LinkedHashMap;
import java.util.Map;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.EntityInfo;
import com.tatnat.client.platform.Gfx;

/**
 * Totem Pop Counter: counts how many Totems of Undying each player has popped (1.11+). Shows a
 * red "-3" over their head and a list on screen; a player's count resets when they die.
 */
public class TotemPops extends HudModule {
	private static TotemPops instance;
	private static final Map<String, Integer> POPS = new LinkedHashMap<>();
	private static final Map<String, Long> LAST = new LinkedHashMap<>();

	private final BooleanSetting overHead = add(new BooleanSetting("Show Over Heads", "A red -N over players who popped", true));
	private final BooleanSetting includeSelf = add(new BooleanSetting("Count Yourself", "Also count your own pops", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the list", true));

	public TotemPops() {
		super("Totem Pop Counter", "Counts every player's totem pops", false, 1.0, 0.45);
		icon = com.tatnat.client.ui.render.Icons.Icon.TOTEMPOP;
		instance = this;
	}

	/** Called by the platform when an entity plays the totem animation. */
	public static void popped(String name, boolean self) {
		TotemPops m = instance;
		if (m == null || !m.isEnabled() || name == null || name.isEmpty()) return;
		if (self && !m.includeSelf.on()) return;
		POPS.merge(name, 1, Integer::sum);
		LAST.put(name, System.currentTimeMillis());
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) {
			POPS.clear();
			return;
		}
		// Dead players start over.
		for (EntityInfo p : game().entities(256)) {
			if (p.kind == EntityInfo.Kind.PLAYER && p.health <= 0 && POPS.remove(p.name) != null) LAST.remove(p.name);
		}
		if (game().health() <= 0) POPS.remove(game().playerName());
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!overHead.on() || POPS.isEmpty() || !game().inWorld() || game().hudHidden()) return;
		for (EntityInfo p : game().entities(64)) {
			Integer n = p.kind == EntityInfo.Kind.PLAYER ? POPS.get(p.name) : null;
			if (n == null) continue;
			double[] s = com.tatnat.client.util.WorldProjector.project(p.x, p.top + 0.95, p.z);
			if (s[2] != 1) continue;
			String text = "-" + n;
			Gfx g = e.gfx;
			float sc = (float) Math.max(0.7, Math.min(1.4, 10.0 / Math.max(1, s[3])));
			int tw = g.mcTextWidth(text, true);
			g.push();
			g.translate((float) s[0], (float) s[1]);
			g.scale(sc, sc);
			g.rect(-tw / 2 - 3, -6, tw / 2 + 3, 5, 0x90000000);
			g.mcText(text, -tw / 2, -4, 0xFFFF4040, true, true);
			g.pop();
		}
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		Map<String, Integer> list = preview ? sample() : POPS;
		if (list.isEmpty()) return size(0, 0);
		int w = g.mcTextWidth("Totem Pops", true);
		for (Map.Entry<String, Integer> en : list.entrySet()) w = Math.max(w, g.mcTextWidth(en.getKey() + "  " + en.getValue(), false));
		w += 10;
		int h = 14 + list.size() * 10;
		if (background.on()) g.rect(0, 0, w, h, 0x6F000000);
		g.mcText("Totem Pops", 5, 3, 0xFFFFD24A, true, true);
		int y = 14;
		long now = System.currentTimeMillis();
		for (Map.Entry<String, Integer> en : list.entrySet()) {
			// Just popped: flash red for a moment.
			boolean fresh = !preview && LAST.containsKey(en.getKey()) && now - LAST.get(en.getKey()) < 1500;
			g.mcText(en.getKey(), 5, y, fresh ? 0xFFFF5555 : 0xFFFFFFFF, true, false);
			String n = String.valueOf(en.getValue());
			g.mcText(n, w - 5 - g.mcTextWidth(n, false), y, 0xFFFF4040, true, false);
			y += 10;
		}
		return size(w, h);
	}

	private static Map<String, Integer> sample() {
		Map<String, Integer> m = new LinkedHashMap<>();
		m.put("Steve", 3);
		m.put("Alex", 1);
		return m;
	}
}
