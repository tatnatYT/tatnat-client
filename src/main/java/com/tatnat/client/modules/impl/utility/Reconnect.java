package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.Gfx;

/** Rejoins the server by itself after you get disconnected, with a countdown and a limit on tries. */
public class Reconnect extends Module {
	private final SliderSetting delay = add(new SliderSetting("Delay", "Seconds to wait before reconnecting", 5, 1, 60, 1, " s"));
	private final SliderSetting attempts = add(new SliderSetting("Max Attempts", "Give up after this many tries in a row", 5, 1, 20, 1, ""));

	private long since = -1;
	private int tries;

	public Reconnect() {
		super("Reconnect", "Automatically rejoins after a disconnect", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.REFRESH;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (game().inWorld()) tries = 0; // made it back in
		if (!game().disconnectedScreen()) {
			since = -1;
			return;
		}
		long now = System.currentTimeMillis();
		if (since < 0) since = now;
		if (tries < attempts.intValue() && now - since >= delay.get() * 1000) {
			tries++;
			since = -1;
			game().reconnect();
		}
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (since < 0 || !game().disconnectedScreen()) return;
		String text = tries >= attempts.intValue() ? "Reconnect: gave up after " + tries + " tries"
				: "Reconnecting in " + Math.max(0, (long) Math.ceil(delay.get() - (System.currentTimeMillis() - since) / 1000.0)) + "s (try "
						+ (tries + 1) + "/" + attempts.intValue() + ")";
		Gfx g = e.gfx;
		int w = g.mcTextWidth(text, false), x = (game().guiWidth() - w) / 2, y = game().guiHeight() - 24;
		g.rect(x - 6, y - 4, x + w + 6, y + 12, 0xB0000000);
		g.mcText(text, x, y, 0xFF4EB1FF, false, false);
	}
}
