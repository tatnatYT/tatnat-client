package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Tap your sprint key once to keep sprinting (and optionally sneak the same way), with a small
 * on-screen indicator. Works by holding the vanilla key binding down for you, so it behaves
 * exactly like holding the key -- servers see nothing unusual.
 */
public class ToggleSprint extends HudModule {
	private final BooleanSetting toggleSneak = add(new BooleanSetting("Toggle Sneak", "Tap sneak to keep sneaking too", false));
	private final BooleanSetting indicator = add(new BooleanSetting("Indicator", "Show Sprinting (Toggled) on screen", true));
	private final ColorSetting color = add(new ColorSetting("Text Color", "Colour of the indicator", 0xFFFFFFFF, true));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the indicator", true));
	private final BooleanSetting brackets = add(new BooleanSetting("Brackets", "Wrap the indicator in [ ]", false));

	private boolean sprinting, sneaking;

	public ToggleSprint() {
		super("Toggle Sprint", "Tap sprint (or sneak) once instead of holding it", Category.UTILITY, true, 0.0, 1.0);
		icon = com.tatnat.client.ui.render.Icons.Icon.RUN;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (mc.player == null) return;
		// consumeClick() counts physical presses; vanilla sprint/sneak only read isDown(), so
		// eating the clicks here doesn't change any vanilla behaviour.
		while (mc.options.keySprint.consumeClick()) sprinting = !sprinting;
		while (mc.options.keyShift.consumeClick()) if (toggleSneak.on()) sneaking = !sneaking;
		if (!toggleSneak.on()) sneaking = false;

		if (mc.screen == null) {
			if (sprinting) mc.options.keySprint.setDown(true);
			if (sneaking) mc.options.keyShift.setDown(true);
		}
	}

	@Override
	protected void onDisable() {
		if (sprinting) mc.options.keySprint.setDown(false);
		if (sneaking) mc.options.keyShift.setDown(false);
		sprinting = sneaking = false;
	}

	private String status(boolean preview) {
		String s;
		if (sneaking) s = "Sneaking (Toggled)";
		else if (sprinting) s = "Sprinting (Toggled)";
		else if (mc.player != null && mc.player.isSprinting()) s = "Sprinting (Vanilla)";
		else if (preview) s = "Sprinting (Toggled)";
		else return "";
		return brackets.on() ? "[" + s + "]" : s;
	}

	@Override
	public boolean hasContent() {
		return indicator.on() && !status(false).isEmpty();
	}

	@Override
	protected long draw(GuiGraphics g, boolean preview) {
		String s = status(preview);
		if (!indicator.on() && !preview) return size(1, 1);
		g.drawString(mc.font, s, 0, 0, color.color(), shadow.on());
		return size(mc.font.width(s), 8);
	}
}
