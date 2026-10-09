package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;

/** Switches to third person while you ride, fly or glide, and back when you stop. */
public class AutoPerspective extends Module {
	private int before = -1;

	public AutoPerspective() {
		super("Auto Perspective", "Third person while riding, flying or gliding", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.SWAP;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld()) {
			before = -1;
			return;
		}
		boolean moving = game().ridingOrFlying();
		if (moving && before < 0) {
			before = game().perspective();
			if (before == 0) game().setPerspective(1);
		} else if (!moving && before >= 0) {
			game().setPerspective(before);
			before = -1;
		}
	}

	@Override
	protected void onDisable() {
		if (before >= 0) game().setPerspective(before);
		before = -1;
	}
}
