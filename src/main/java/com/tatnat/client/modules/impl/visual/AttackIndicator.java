package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ModeSetting;

/** Where the attack cooldown indicator sits: under the crosshair, next to the hotbar, or hidden (1.9+). */
public class AttackIndicator extends Module {
	private final ModeSetting position = add(new ModeSetting("Position", "Where the cooldown indicator is shown", "Hotbar", "Crosshair", "Hotbar", "Off"));

	private String applied = "";

	public AttackIndicator() {
		super("Attack Indicator", "Move or hide the attack cooldown indicator", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.SWORD;
	}

	@Override
	protected void onEnable() {
		applied = "";
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		String want = position.get();
		if (want.equals(applied)) return;
		applied = want;
		game().setAttackIndicator(position.is("Off") ? 0 : position.is("Crosshair") ? 1 : 2);
	}
}
