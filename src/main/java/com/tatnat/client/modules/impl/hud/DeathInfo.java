package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.Gfx;

/**
 * Where you last died, so you can find your items: right on the death screen ("You died at
 * 123, 64, -453 in the Nether") and, until you get there, as {@code Died at: 123, 64, -453} on the HUD.
 */
public class DeathInfo extends TextHudModule {
	private static DeathInfo instance;

	private final BooleanSetting onDeathScreen = add(new BooleanSetting("On Death Screen", "Show where you died on the death screen", true));
	private final BooleanSetting onlyAfterDeath = add(new BooleanSetting("Hide Until You Die", "Keep the HUD line hidden until there's a death to show", true));

	private boolean dead, hasDeath;
	private int dx, dy, dz;
	private String dim = "";

	public DeathInfo() {
		super("Death Info", "Shows where you died, on the death screen and the HUD", false, 0.75, 0.225);
		icon = com.tatnat.client.ui.render.Icons.Icon.SKULL;
		instance = this;
	}

	private void record() {
		dx = (int) Math.floor(game().x());
		dy = (int) Math.floor(game().y());
		dz = (int) Math.floor(game().z());
		dim = game().worldKey();
		hasDeath = true;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) return;
		boolean now = game().health() <= 0;
		if (now && !dead) record();
		dead = now;
	}

	private static String dimName(String key) {
		String d = key.replaceAll(".*[:/]", "");
		if (d.equals("the_nether")) return "the Nether";
		if (d.equals("the_end")) return "the End";
		if (d.equals("overworld")) return "the Overworld";
		return d.replace('_', ' ');
	}

	/** Called by the death screen after it has drawn (GUI coordinates). */
	public static void onDeathScreen(Gfx g) {
		DeathInfo m = instance;
		if (m == null || !m.isEnabled() || !m.onDeathScreen.on()) return;
		if (!m.dead || !m.hasDeath) m.record(); // the screen can open before our tick sees the death
		String where = m.dx + ", " + m.dy + ", " + m.dz;
		String line = "§7You died at §f" + where + "§7 in " + "§f" + dimName(m.dim);
		int w = TatnatClient.game().guiWidth(), h = TatnatClient.game().guiHeight();
		int tw = g.mcTextWidth(line, false);
		int y = h / 4 + 52;
		g.rect(w / 2 - tw / 2 - 6, y - 4, w / 2 + tw / 2 + 6, y + 12, 0x80000000);
		g.mcText(line, w / 2 - tw / 2, y, 0xFFFFFFFF, true, false);
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
		return dim.equals(game().worldKey()) ? where : where + " (" + dimName(dim) + ")";
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		if (!preview && !hasDeath && onlyAfterDeath.on()) return size(0, 0);
		return super.draw(g, preview);
	}
}
