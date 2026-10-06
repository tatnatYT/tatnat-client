package com.tatnat.client.modules;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.Setting;

/**
 * The Performance tab: switches that make the menus cheaper to draw on slow PCs. Not a mod (it
 * never shows in the grid); it has its own sidebar page and its own entry in the config.
 */
public final class Performance extends Module {
	public static final Performance INSTANCE = new Performance();

	public final BooleanSetting animations = add(new BooleanSetting("Menu Animations",
			"Fades, slides and toggle movement. Off = everything changes instantly", true));
	public final BooleanSetting smoothScroll = add(new BooleanSetting("Smooth Scrolling",
			"Lists glide when you scroll. Off = they jump straight to the new spot", true));
	public final BooleanSetting hoverEffects = add(new BooleanSetting("Hover Effects",
			"Cards and buttons light up under the mouse", true));
	public final BooleanSetting shadows = add(new BooleanSetting("Shadows",
			"Soft shadows behind the panels (the heaviest effect)", true));
	public final BooleanSetting roundedCorners = add(new BooleanSetting("Rounded Corners",
			"Smooth rounded corners. Off = square corners, cheaper to draw", true));
	public final ActionSetting lightweight = add(new ActionSetting("Lightweight Mode",
			"Turns every effect above off for the fastest menu", () -> lightweight() ? "On" : "Turn on", () -> setAll(false)));
	public final ActionSetting restore = add(new ActionSetting("Restore Defaults",
			"Turns every effect back on", () -> "Restore", () -> setAll(true)));

	private Performance() {
		super("Performance", "Make the menus lighter on slow PCs", Category.UTILITY, true);
	}

	private void setAll(boolean on) {
		for (Setting<?> s : settings()) {
			if (s instanceof BooleanSetting) ((BooleanSetting) s).set(on);
		}
		TatnatClient.CONFIG.markDirty();
	}

	private boolean lightweight() {
		for (Setting<?> s : settings()) {
			if (s instanceof BooleanSetting && ((BooleanSetting) s).on()) return false;
		}
		return true;
	}

	public static boolean animations() {
		return INSTANCE.animations.on();
	}

	public static boolean smoothScroll() {
		return INSTANCE.smoothScroll.on();
	}

	public static boolean hoverEffects() {
		return INSTANCE.hoverEffects.on();
	}

	public static boolean shadows() {
		return INSTANCE.shadows.on();
	}

	public static boolean roundedCorners() {
		return INSTANCE.roundedCorners.on();
	}
}
