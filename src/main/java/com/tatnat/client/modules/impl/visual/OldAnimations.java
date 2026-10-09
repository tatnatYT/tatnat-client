package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;

/**
 * 1.7 Animations (1.8.9): the sword keeps swinging while you block (block-hitting) and the item
 * swings while you eat or drink, like 1.7 did.
 */
public class OldAnimations extends Module {
	private static OldAnimations instance;

	private final BooleanSetting blockHit = add(new BooleanSetting("1.7 Block Style", "Swing the sword while blocking", true));
	private final BooleanSetting eating = add(new BooleanSetting("Old Eating", "Swing while eating and drinking", true));

	public OldAnimations() {
		super("1.7 Animations", "1.7 block-hitting and eating animations (1.8.9)", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.ANIMATION;
		instance = this;
	}

	public static boolean blockHit() {
		return instance != null && instance.isEnabled() && instance.blockHit.on();
	}

	public static boolean eating() {
		return instance != null && instance.isEnabled() && instance.eating.on();
	}
}
