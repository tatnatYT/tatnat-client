package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;

/** Dropped items lie flat on the ground instead of floating, bobbing and spinning (1.16 - 1.21.1). */
public class ItemPhysic extends Module {
	private static ItemPhysic instance;

	public ItemPhysic() {
		super("Item Physic", "Dropped items lie flat on the ground", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.CUBE;
		instance = this;
	}

	public static boolean on() {
		return instance != null && instance.isEnabled();
	}
}
