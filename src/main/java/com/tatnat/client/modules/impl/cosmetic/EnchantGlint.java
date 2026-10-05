package com.tatnat.client.modules.impl.cosmetic;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Recolours the enchantment shimmer (the platform re-tints vanilla's glint textures, keeping
 * their brightness) and changes how fast it moves. Turning it off restores vanilla's glint.
 */
public class EnchantGlint extends Module {
	public static EnchantGlint INSTANCE;

	private final ColorSetting color = add(new ColorSetting("Color", "Glint colour (Chroma cycles the rainbow)", 0xFFE5323E, true));
	public final SliderSetting speed = add(new SliderSetting("Speed", "How fast the shimmer moves", 1.0, 0.1, 5, 0.1, "x"));

	private int appliedColor;
	private int frame;

	public EnchantGlint() {
		super("Enchant Glint", "Change the colour and speed of the enchant shimmer", Category.COSMETIC, false);
		icon = Icons.Icon.SPARKLE;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		// Chroma needs a re-tint a few times a second; a fixed colour only when it changes.
		if (color.chroma() && ++frame % 2 != 0) return;
		int c = color.color() | 0xFF000000;
		if (c != appliedColor) {
			TatnatClient.features().tintGlint(c);
			appliedColor = c;
		}
	}

	@Override
	protected void onEnable() {
		appliedColor = 0;
	}

	@Override
	protected void onDisable() {
		TatnatClient.features().tintGlint(0);
		appliedColor = 0;
	}
}
