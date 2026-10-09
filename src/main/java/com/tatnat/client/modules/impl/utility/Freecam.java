package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.Bind;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.util.KeyCodes;

/**
 * Detaches the camera from your body so you can fly around and look. Your player stays exactly
 * where it is and receives no movement input; mouse movement turns the camera; clicks are
 * blocked so you can't hit or place blocks from the camera position. Toggle with F4 by default.
 * The platform layer owns the floating camera itself.
 *
 * Note: many PvP servers forbid freecam. It only moves your view (the server sees you standing
 * still), but use it where it's allowed.
 */
public class Freecam extends Module {
	public static Freecam INSTANCE;

	private final SliderSetting speed = add(new SliderSetting("Speed", "Flying speed", 1.0, 0.1, 5, 0.1, "x"));
	private final BooleanSetting hideBody = add(new BooleanSetting("Hide Body", "Make your own player invisible while flying", true));

	private boolean flying;

	public Freecam() {
		super("Freecam", "Fly the camera around without moving (F4)", Category.UTILITY, false);
		icon = Icons.Icon.PLANE;
		defaultToggleKey(KeyCodes.F4);
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.flying;
	}

	public boolean hideBody() {
		return hideBody.on();
	}

	@Override
	protected void onEnable() {
		flying = game().inWorld() && TatnatClient.features().startFreecam();
		// Can't fly outside a world (or on a version without freecam): switch straight back off.
		if (!flying) game().execute(() -> setEnabled(false));
	}

	@Override
	protected void onDisable() {
		if (flying) TatnatClient.features().stopFreecam();
		flying = false;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!flying) return;
		if (!game().inWorld()) {
			setEnabled(false);
			return;
		}
		if (game().screenOpen()) {
			TatnatClient.features().moveFreecam(0, 0, 0);
			return;
		}
		double fwd = (game().keyDown(Bind.FORWARD) ? 1 : 0) - (game().keyDown(Bind.BACK) ? 1 : 0);
		double side = (game().keyDown(Bind.LEFT) ? 1 : 0) - (game().keyDown(Bind.RIGHT) ? 1 : 0);
		double up = (game().keyDown(Bind.JUMP) ? 1 : 0) - (game().keyDown(Bind.SNEAK) ? 1 : 0);
		double sp = speed.get() * (game().keyDown(Bind.SPRINT) ? 2.5 : 1.0);
		double yaw = Math.toRadians(TatnatClient.features().freecamYaw());
		double sin = Math.sin(yaw), cos = Math.cos(yaw);
		double dx = -sin * fwd + cos * side, dz = cos * fwd + sin * side;
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len > 1) {
			dx /= len;
			dz /= len;
		}
		TatnatClient.features().moveFreecam(dx * sp, up * sp, dz * sp);
	}

	/** Blocks attacking / using from the floating camera. */
	@Subscribe
	public void onMouse(Events.MouseButton e) {
		if (flying && e.inGame && (e.button == 0 || e.button == 1)) e.cancel();
	}
}
