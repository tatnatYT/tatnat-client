package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;

/**
 * Hides the HUD (like F1) after you've stood still for a while, and brings it back the moment you
 * move, turn, take damage or open a menu.
 */
public class AutohideHud extends Module {
	private final SliderSetting delay = add(new SliderSetting("Fade Delay", "Seconds of standing still before the HUD hides", 5, 1, 60, 1, " s"));

	private long lastActive = System.currentTimeMillis();
	private double lx, ly, lz;
	private float lyaw, lpitch, lhealth;
	private boolean hiddenByUs;

	public AutohideHud() {
		super("Autohide HUD", "Hides the HUD while you stand still", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.EYE_OFF;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) return;
		double x = game().x(), y = game().y(), z = game().z();
		float yaw = game().yaw(), pitch = game().pitch(), health = game().health();
		boolean active = Math.abs(x - lx) + Math.abs(y - ly) + Math.abs(z - lz) > 0.01 || Math.abs(yaw - lyaw) + Math.abs(pitch - lpitch) > 0.5
				|| health < lhealth || game().screenOpen();
		lx = x;
		ly = y;
		lz = z;
		lyaw = yaw;
		lpitch = pitch;
		lhealth = health;
		long now = System.currentTimeMillis();
		if (active) {
			lastActive = now;
			if (hiddenByUs) show();
		} else if (!hiddenByUs && !game().hudHidden() && now - lastActive > delay.get() * 1000) {
			game().setHudHidden(true);
			hiddenByUs = true;
		}
	}

	private void show() {
		// Only undo our own hiding (not an F1 the player pressed themselves).
		if (game().hudHidden()) game().setHudHidden(false);
		hiddenByUs = false;
	}

	@Override
	protected void onDisable() {
		if (hiddenByUs) show();
	}
}
