package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.theme.Colors;

/**
 * Changes the red flash mobs and players get when hit. Vanilla bakes that red into a tiny 16x16
 * "overlay" texture; this rewrites its hurt rows ({@code OverlayTextureMixin}) whenever the
 * colour changes and puts vanilla's red back when turned off.
 */
public class HitColor extends Module {
	public static HitColor INSTANCE;
	/** Vanilla's hurt tint: red at 70% opacity. */
	public static final int VANILLA = 0xB3FF0000;

	/** Implemented by the overlay texture mixin. */
	public interface Target {
		void tatnat$setHurtColor(int argb);
	}

	public static Target target;

	private final ColorSetting color = add(new ColorSetting("Color", "Colour of the hit flash", 0xFF4EB1FF, true));
	private final SliderSetting opacity = add(new SliderSetting("Opacity", "How strong the flash is (vanilla is 30%)", 50, 0, 100, 5, "%"));

	private int applied = VANILLA;

	public HitColor() {
		super("Hit Color", "Change the red flash when something gets hit", Category.VISUAL, false);
		icon = Icons.Icon.SWORD;
		INSTANCE = this;
	}

	private int wanted() {
		// The entity shader keeps "alpha" of the original colour, so opacity is stored inverted.
		return Colors.withAlpha(color.color(), Math.round((1f - opacity.floatValue() / 100f) * 255f));
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		apply(wanted());
	}

	@Override
	protected void onEnable() {
		apply(wanted());
	}

	@Override
	protected void onDisable() {
		apply(VANILLA);
	}

	private void apply(int argb) {
		if (target == null || argb == applied) return;
		if (com.tatnat.client.util.DevTest.enabled()) com.tatnat.client.TatnatClient.LOG.info("[devtest] hit colour -> {} (target {})", Integer.toHexString(argb), target);
		target.tatnat$setHurtColor(argb);
		applied = argb;
	}
}
