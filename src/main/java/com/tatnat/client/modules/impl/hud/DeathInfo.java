package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

/** {@code Died at: 123, 64, -453}: where you last died, so you can find your items. */
public class DeathInfo extends TextHudModule {
	private final BooleanSetting onlyAfterDeath = add(new BooleanSetting("Hide Until You Die", "Stay hidden until there's a death to show", true));

	private boolean dead, hasDeath;
	private int dx, dy, dz;
	private String dim = "";

	public DeathInfo() {
		super("Death Info", "Shows where you last died", false, 0.0, 0.48);
		icon = com.tatnat.client.ui.render.Icons.Icon.SKULL;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) return;
		boolean now = game().health() <= 0;
		if (now && !dead) {
			dx = (int) Math.floor(game().x());
			dy = (int) Math.floor(game().y());
			dz = (int) Math.floor(game().z());
			dim = game().worldKey();
			hasDeath = true;
		}
		dead = now;
	}

	@Override
	protected String label() {
		return "Died at";
	}

	@Override
	protected String value(boolean preview) {
		if (preview) return "123, 64, -453";
		if (!hasDeath) return "-";
		String where = dx + ", " + dy + ", " + dz;
		// Mention the dimension when you're not in it any more.
		return dim.equals(game().worldKey()) ? where : where + " (" + dim.replaceAll(".*[:/]", "").replace('_', ' ') + ")";
	}

	@Override
	protected long draw(com.tatnat.client.platform.Gfx g, boolean preview) {
		if (!preview && !hasDeath && onlyAfterDeath.on()) return size(0, 0);
		return super.draw(g, preview);
	}
}
