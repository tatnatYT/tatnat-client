package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.Gfx;

/**
 * Totem Counter: a totem icon with your count next to it in green, centred over the hotbar just
 * above your armor (or anywhere you drag it). Red when you have none.
 */
public class TotemCounter extends HudModule {
	private final BooleanSetting nextToHearts = add(new BooleanSetting("Next to Hearts", "Sit over the hotbar above your armor (off: place it in the HUD editor)", true));
	private final BooleanSetting hideWhenNone = add(new BooleanSetting("Hide When None", "Only show it while you carry totems", false));

	public TotemCounter() {
		super("Totem Counter", "A totem icon with how many Totems of Undying you have", false, 0.75, 0.15);
		icon = com.tatnat.client.ui.render.Icons.Icon.TOTEM;
	}

	private static int totems(boolean preview) {
		return preview ? 12 : ItemCounter.count("totem_of_undying, totem of undying");
	}

	/** The icon, then the count beside it (not on it), top-left at (x, y). Returns the width. */
	private static int drawTotem(Gfx g, int x, int y, int count) {
		Object stack = com.tatnat.client.TatnatClient.game().findStack("totem");
		// Versions without totems (before 1.11) have nothing to show.
		if (stack == null && count == 0) return 0;
		if (stack != null) g.item(stack, x, y);
		String n = String.valueOf(count);
		g.mcText(n, x + 18, y + 4, count == 0 ? 0xFFFF5555 : 0xFF55FF55, true, false);
		return 18 + g.mcTextWidth(n, false);
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!nextToHearts.on() || !game().inWorld() || game().hudHidden()) return;
		int n = totems(false);
		if (n == 0 && hideWhenNone.on()) return;
		// Centred over the hotbar, just above the armor row (higher when there are more heart rows).
		int lift = 0;
		float[] st = game().barStats();
		if (st != null) {
			int rows = Math.max(1, (int) Math.ceil((st[1] + st[2]) / 2f / 10f));
			lift = (rows - 1) * Math.max(10 - (rows - 2), 3);
		}
		int w = 18 + e.gfx.mcTextWidth(String.valueOf(n), false);
		drawTotem(e.gfx, game().guiWidth() / 2 - w / 2, game().guiHeight() - 67 - lift, n);
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		if (!preview && nextToHearts.on()) return size(0, 0);
		int n = totems(preview);
		if (!preview && n == 0 && hideWhenNone.on()) return size(0, 0);
		int w = drawTotem(g, 0, 0, n);
		return w == 0 ? size(0, 0) : size(w, 16);
	}
}
