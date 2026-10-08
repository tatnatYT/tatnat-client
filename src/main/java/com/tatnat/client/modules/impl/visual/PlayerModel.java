package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;

/**
 * Player Model and Elytras: stop drawing armor and/or elytras on players and mobs, so skins and
 * capes show (1.15+ for armor, 1.14+ for elytras).
 */
public class PlayerModel extends Module {
	private static PlayerModel instance;

	private final BooleanSetting armor = add(new BooleanSetting("Hide Armor", "Don't draw armor", true));
	private final BooleanSetting elytra = add(new BooleanSetting("Hide Elytra", "Don't draw elytras (your cape shows instead)", false));

	public PlayerModel() {
		super("Player Model", "Hide armor or elytras on player models", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.USER;
		instance = this;
	}

	public static boolean hideArmor() {
		return instance != null && instance.isEnabled() && instance.armor.on();
	}

	public static boolean hideElytra() {
		return instance != null && instance.isEnabled() && instance.elytra.on();
	}
}
