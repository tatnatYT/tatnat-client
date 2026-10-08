package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.EntityInfo;

/** A countdown over primed TNT: {@code 3.2s}, green to red as it gets close. */
public class TntTimer extends Module {
	private final SliderSetting scale = add(new SliderSetting("Scale", "Size of the timer", 1.0, 0.5, 2.0, 0.1, "x"));

	public TntTimer() {
		super("TNT Timer", "Shows how long until primed TNT explodes", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.TNT;
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden()) return;
		for (EntityInfo t : game().entities(64)) {
			if (t.kind != EntityInfo.Kind.TNT) continue;
			double[] p = WorldLabels.onScreen(t.x, t.top + 0.5, t.z);
			if (p == null) continue;
			double sec = t.ticks / 20.0;
			int color = sec > 2.5 ? 0xFF55FF55 : sec > 1 ? 0xFFFFFF55 : 0xFFFF5555;
			WorldLabels.text(e.gfx, p, WorldLabels.time(sec), color, scale.floatValue());
		}
	}
}
