package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.Gfx;

/**
 * Totem Counter, like uku's: a totem icon with your count on it in big green numbers, sitting
 * between your hearts and your hunger bar (or anywhere you drag it). Red when you have none.
 */
public class TotemCounter extends HudModule {
	private final BooleanSetting nextToHearts = add(new BooleanSetting("Next to Hearts", "Sit between the hearts and hunger bar (off: place it in the HUD editor)", true));
	private final BooleanSetting hideWhenNone = add(new BooleanSetting("Hide When None", "Only show it while you carry totems", false));

	public TotemCounter() {
		super("Totem Counter", "A totem icon with how many Totems of Undying you have", false, 0.75, 0.15);
		icon = com.tatnat.client.ui.render.Icons.Icon.TOTEM;
	}

	private static int totems(boolean preview) {
		return preview ? 12 : ItemCounter.count("totem_of_undying, totem of undying");
	}

	/** The icon with the count over its bottom-right, top-left at (x, y); 18 x 18. */
	private static void drawTotem(Gfx g, int x, int y, int count) {
		Object stack = com.tatnat.client.TatnatClient.game().findStack("totem");
		if (stack != null) g.item(stack, x + 1, y);
		String n = String.valueOf(count);
		int w = g.mcTextWidth(n, true);
		// Big, bold and green like uku's; red at zero.
		g.push();
		g.translate(x + 9 - w * 0.65f, y + 8);
		g.scale(1.3f, 1.3f);
		g.mcText(n, 0, 0, count == 0 ? 0xFFFF5555 : 0xFF55FF55, true, true);
		g.pop();
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!nextToHearts.on() || !game().inWorld() || game().hudHidden()) return;
		int n = totems(false);
		if (n == 0 && hideWhenNone.on()) return;
		// The gap between the hearts (left of centre) and the hunger bar, over the hotbar.
		drawTotem(e.gfx, game().guiWidth() / 2 - 9, game().guiHeight() - 52, n);
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		if (!preview && nextToHearts.on()) return size(0, 0);
		int n = totems(preview);
		if (!preview && n == 0 && hideWhenNone.on()) return size(0, 0);
		drawTotem(g, 0, 0, n);
		return size(20, 22);
	}
}
