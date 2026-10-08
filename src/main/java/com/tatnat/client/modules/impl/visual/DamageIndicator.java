package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.EntityInfo;
import com.tatnat.client.platform.Gfx;

/** A health bar (and the hearts left) over mobs and players near you. */
public class DamageIndicator extends Module {
	private final BooleanSetting players = add(new BooleanSetting("Players", "Show it over other players", true));
	private final BooleanSetting mobs = add(new BooleanSetting("Mobs", "Show it over mobs and animals", true));
	private final BooleanSetting numbers = add(new BooleanSetting("Show Health", "Write the health next to the bar", true));
	private final BooleanSetting damagedOnly = add(new BooleanSetting("Only When Hurt", "Hide it while they're at full health", false));
	private final SliderSetting range = add(new SliderSetting("Range", "Only entities this close", 24, 4, 64, 1, " blocks"));

	public DamageIndicator() {
		super("Damage Indicator", "Health bars over mobs and players", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.HEART;
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden()) return;
		Gfx g = e.gfx;
		for (EntityInfo l : game().entities(range.get())) {
			boolean isPlayer = l.kind == EntityInfo.Kind.PLAYER;
			if (!(isPlayer ? players.on() : l.kind == EntityInfo.Kind.MOB && mobs.on())) continue;
			if (l.maxHealth <= 0 || damagedOnly.on() && l.health >= l.maxHealth) continue;
			// Above the name tag for players, just over the head for mobs.
			double[] p = WorldLabels.onScreen(l.x, l.top + (isPlayer ? 0.75 : 0.35), l.z);
			if (p == null) continue;
			float frac = Math.max(0, Math.min(1, l.health / l.maxHealth));
			float s = (float) Math.max(0.6, Math.min(1.0, 8.0 / Math.max(1, p[3])));
			int bw = 40, bh = 4;
			int color = frac > 0.6f ? 0xFF55FF55 : frac > 0.3f ? 0xFFFFFF55 : 0xFFFF5555;
			g.push();
			g.translate((float) p[0], (float) p[1]);
			g.scale(s, s);
			g.rect(-bw / 2 - 1, -1, bw / 2 + 1, bh + 1, 0xA0000000);
			g.rect(-bw / 2, 0, -bw / 2 + Math.round(bw * frac), bh, color);
			if (numbers.on()) {
				String text = String.format(java.util.Locale.ROOT, "%.1f ❤", l.health / 2f);
				int tw = g.mcTextWidth(text, false);
				g.mcText(text, -tw / 2, -10, color, true, false);
			}
			g.pop();
		}
	}
}
