package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

/** Pushes the underwater fog back so you can see far underwater ({@code WaterFogEnvironmentMixin}). */
public class ClearWater extends Module {
	public static ClearWater INSTANCE;

	public final SliderSetting strength = add(new SliderSetting("Fog Removal", "How much of the underwater fog to remove", 100, 0, 100, 5, "%"));

	public ClearWater() {
		super("Clear Water", "Removes the thick blue fog underwater", Category.VISUAL, false);
		icon = Icons.Icon.DROP;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}
}
