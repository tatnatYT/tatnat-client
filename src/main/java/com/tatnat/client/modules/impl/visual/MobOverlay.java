package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;

/** Puts the glowing outline (like the Glowing effect) on hostile mobs, other creatures or players, visible through walls. */
public class MobOverlay extends Module {
	private static MobOverlay instance;

	private final BooleanSetting hostile = add(new BooleanSetting("Hostile Mobs", "Zombies, creepers, skeletons…", true));
	private final BooleanSetting passive = add(new BooleanSetting("Other Creatures", "Animals, villagers and other mobs", false));
	private final BooleanSetting players = add(new BooleanSetting("Players", "Other players", false));

	public MobOverlay() {
		super("Mob Overlay", "Glowing outlines on mobs, even through walls", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.EYE;
		instance = this;
	}

	/** kind: 1 = hostile, 2 = other living, 3 = player. Asked by the glowing mixin (1.16+). */
	public static boolean glows(int kind) {
		MobOverlay m = instance;
		if (m == null || !m.isEnabled()) return false;
		return kind == 1 ? m.hostile.on() : kind == 2 ? m.passive.on() : kind == 3 && m.players.on();
	}
}
