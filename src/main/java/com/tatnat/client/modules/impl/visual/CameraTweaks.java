package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;

/** Camera: how far the third-person camera sits behind (or in front of) you. Vanilla is 4 blocks. */
public class CameraTweaks extends Module {
	private static CameraTweaks instance;

	private final SliderSetting distance = add(new SliderSetting("Third Person Distance", "Blocks between you and the camera (vanilla 4)", 4, 1, 16, 0.5, " blocks"));

	public CameraTweaks() {
		super("Camera", "Change the third-person camera distance", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.VIDEOCAM;
		instance = this;
	}

	/** Multiplier for the game's own camera distance (1 = unchanged). Used by the camera mixin (1.14+). */
	public static double distanceFactor() {
		CameraTweaks m = instance;
		return m != null && m.isEnabled() ? m.distance.get() / 4.0 : 1.0;
	}
}
