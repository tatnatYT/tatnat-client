package com.tatnat.client.modules.impl.hud;

import java.util.List;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.EffectInfo;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.ui.render.Icons;

/** Your active potion effects: icon, name with level, and a ticking timer. */
public class PotionStatus extends HudModule {
	private final BooleanSetting amplifiers = add(new BooleanSetting("Show Amplifiers", "Add the level, like Speed II", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the list", false));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the text", true));
	private final BooleanSetting blink = add(new BooleanSetting("Blink When Ending", "Flash the timer in the last 10 seconds", true));

	private static final String[] ROMAN = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

	public PotionStatus() {
		super("Potion Status", "Shows your active effects and how long they last", false, 1.0, 0.25);
		icon = Icons.Icon.POTION;
	}

	@Override
	public boolean hasContent() {
		return !game().effects(false).isEmpty();
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		List<EffectInfo> list = game().effects(preview);
		int w = 0;
		for (EffectInfo e : list) w = Math.max(w, 22 + Math.max(g.mcTextWidth(name(e), false), g.mcTextWidth(e.duration, false)));
		int h = list.size() * 22;
		if (background.on() && !list.isEmpty()) g.rect(-3, -3, w + 3, h + 1, 0x6F000000);
		int y = 0;
		for (EffectInfo e : list) {
			g.effectIcon(e.icon, 0, y, 18);
			g.mcText(name(e), 22, y, 0xFFFFFFFF, shadow.on(), false);
			boolean hide = blink.on() && e.ending && System.currentTimeMillis() / 300 % 2 == 0;
			if (!hide) g.mcText(e.duration, 22, y + 10, e.ending ? 0xFFFF5555 : 0xFFAAAAAA, shadow.on(), false);
			y += 22;
		}
		return size(Math.max(1, w + 3), Math.max(1, h - 2));
	}

	private String name(EffectInfo e) {
		if (!amplifiers.on() || e.level <= 1) return e.name;
		return e.name + (e.level < ROMAN.length ? ROMAN[e.level] : " " + e.level);
	}
}
