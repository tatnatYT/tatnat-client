package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;

/** Inventory screen tweaks: hide the player model next to your inventory (1.14+). */
public class InventoryTweaks extends Module {
	private static InventoryTweaks instance;

	private final BooleanSetting hideModel = add(new BooleanSetting("Hide Player Model", "No player model next to your inventory", true));

	public InventoryTweaks() {
		super("Inventory", "Tweaks for the inventory screen", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.CHEST;
		instance = this;
	}

	public static boolean hideModel() {
		return instance != null && instance.isEnabled() && instance.hideModel.on();
	}
}
