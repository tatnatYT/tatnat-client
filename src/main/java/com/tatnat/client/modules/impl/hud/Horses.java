package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.platform.Gfx;

/**
 * A clean jump-power bar while riding a horse (or llama, camel…), instead of the vanilla one. It
 * only shows while you're riding something that can jump.
 */
public class Horses extends HudModule {
	private static Horses instance;
	private static final int W = 120, H = 6;

	private final BooleanSetting hideVanilla = add(new BooleanSetting("Hide Vanilla Bar", "Hide the game's own jump bar (1.14 - 1.21.5)", true));
	private final ColorSetting color = add(new ColorSetting("Color", "Bar colour", 0xFF4EB1FF, true));

	public Horses() {
		super("Horses", "A cleaner jump bar when riding horses", false, 0.5, 0.82);
		icon = com.tatnat.client.ui.render.Icons.Icon.HORSESHOE;
		instance = this;
	}

	/** Asked by the vanilla jump-bar mixin. */
	public static boolean replacesVanilla() {
		return instance != null && instance.isEnabled() && instance.hideVanilla.on();
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		float jump = preview ? 0.6f : game().horseJump();
		if (jump < 0) return size(0, 0);
		g.rect(0, 0, W, H, 0x90000000);
		int fill = Math.round((W - 2) * Math.max(0, Math.min(1, jump)));
		g.rect(1, 1, 1 + fill, H - 1, jump >= 0.99f ? 0xFF55FF55 : color.color(0));
		return size(W, H);
	}
}
