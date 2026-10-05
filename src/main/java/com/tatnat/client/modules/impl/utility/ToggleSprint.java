package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.platform.Bind;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.ui.render.Icons;

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
		icon = Icons.Icon.RUN;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) return;
		// Consuming clicks counts physical presses; vanilla sprint/sneak only read "is held", so
		// eating the clicks here doesn't change any vanilla behaviour.
		while (game().consumeClick(Bind.SPRINT)) sprinting = !sprinting;
		while (game().consumeClick(Bind.SNEAK)) if (toggleSneak.on()) sneaking = !sneaking;
		if (!toggleSneak.on()) sneaking = false;

		if (!game().screenOpen()) {
			if (sprinting) game().setKeyDown(Bind.SPRINT, true);
			if (sneaking) game().setKeyDown(Bind.SNEAK, true);
		}
	}

	@Override
	protected void onDisable() {
		if (sprinting) game().setKeyDown(Bind.SPRINT, false);
		if (sneaking) game().setKeyDown(Bind.SNEAK, false);
		sprinting = sneaking = false;
	}

	private String status(boolean preview) {
		String s;
		if (sneaking) s = "Sneaking (Toggled)";
		else if (sprinting) s = "Sprinting (Toggled)";
		else if (game().inWorld() && game().sprinting()) s = "Sprinting (Vanilla)";
		else if (preview) s = "Sprinting (Toggled)";
		else return "";
		return brackets.on() ? "[" + s + "]" : s;
	}

	@Override
	public boolean hasContent() {
		return indicator.on() && !status(false).isEmpty();
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		String s = status(preview);
		if (!indicator.on() && !preview) return size(1, 1);
		g.mcText(s, 0, 0, color.color(), shadow.on(), false);
		return size(g.mcTextWidth(s, false), 8);
	}
}
