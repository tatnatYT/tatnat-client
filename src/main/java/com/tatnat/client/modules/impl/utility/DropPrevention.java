package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.platform.Bind;

/**
 * No more accidental drops: the drop key only works while you hold Sneak (Shift). Drag items out of
 * the inventory screen as usual.
 */
public class DropPrevention extends Module {
	public DropPrevention() {
		super("Drop Prevention", "Hold Sneak to drop items, so you never drop by accident", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.BOX;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (!game().inWorld() || game().screenOpen()) return;
		// Swallow drop presses unless sneaking; the game never sees them.
		if (!game().keyDown(Bind.SNEAK)) while (game().consumeClick(Bind.DROP)) { /* eaten */ }
	}
}
