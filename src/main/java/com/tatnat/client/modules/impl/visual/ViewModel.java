package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;

/** Moves and resizes the item in your hand in first person (1.15 - 26.1). The off hand is mirrored. */
public class ViewModel extends Module {
	private static ViewModel instance;

	private final SliderSetting scale = add(new SliderSetting("Item Scale", "Size of the held item", 1.0, 0.3, 1.5, 0.05, "x"));
	private final SliderSetting x = add(new SliderSetting("Item X Offset", "Left / right", 0, -1, 1, 0.05, ""));
	private final SliderSetting y = add(new SliderSetting("Item Y Offset", "Down / up", 0, -1, 1, 0.05, ""));
	private final SliderSetting z = add(new SliderSetting("Item Z Offset", "Closer / further", 0, -1, 1, 0.05, ""));

	public ViewModel() {
		super("ViewModel", "Move and resize the item in your hand", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.SWORD;
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	public static double x() {
		return instance.x.get();
	}

	public static double y() {
		return instance.y.get();
	}

	public static double z() {
		return instance.z.get();
	}

	public static float scale() {
		return (float) Math.max(0.05, instance.scale.get());
	}
}
