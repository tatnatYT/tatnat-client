package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;

/** A cleaner F3 screen: hides the system-information column (Java, memory, CPU, GPU…) on the right (1.14 - 1.21.x). */
public class CustomF3 extends Module {
	private static CustomF3 instance;

	private final BooleanSetting spam = add(new BooleanSetting("Hide Spam", "Hide the system information on the right", true));

	public CustomF3() {
		super("Custom F3", "A cleaner debug screen", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.BUG;
		instance = this;
	}

	public static boolean hideSpam() {
		return instance != null && instance.isEnabled() && instance.spam.on();
	}
}
