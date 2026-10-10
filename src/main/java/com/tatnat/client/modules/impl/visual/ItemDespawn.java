package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.EntityInfo;

/**
 * A timer over dropped items: how long until they despawn (5 minutes after dropping). The game
 * only knows an item's age from when it came into view, so items you walk up to may show more time.
 */
public class ItemDespawn extends Module {
	private static final int LIFETIME = 6000; // ticks

	private final BooleanSetting lookOnly = add(new BooleanSetting("Only When Looking", "Only show the timer of the item under your crosshair", true));
	private final BooleanSetting showName = add(new BooleanSetting("Show Item Name", "Add the item's name and count", false));
	private final SliderSetting range = add(new SliderSetting("Range", "Only items this close", 16, 4, 64, 1, " blocks"));
	private final SliderSetting scale = add(new SliderSetting("Scale", "Size of the timer", 0.8, 0.5, 2.0, 0.1, "x"));

	public ItemDespawn() {
		super("Item Despawn", "Shows when dropped items will despawn", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.TRASH;
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden()) return;
		// With Only When Looking: just the item closest to the crosshair (within a small circle).
		java.util.List<EntityInfo> all = game().entities(range.get());
		EntityInfo target = null;
		if (lookOnly.on()) {
			double cx = game().guiWidth() / 2.0, cy = game().guiHeight() / 2.0, best = 24 * 24;
			for (EntityInfo it : all) {
				if (it.kind != EntityInfo.Kind.ITEM) continue;
				double[] p = WorldLabels.onScreen(it.x, it.top - 0.12, it.z);
				if (p == null) continue;
				double d = (p[0] - cx) * (p[0] - cx) + (p[1] - cy) * (p[1] - cy);
				if (d < best) {
					best = d;
					target = it;
				}
			}
			if (target == null) return;
		}
		for (EntityInfo it : all) {
			if (it.kind != EntityInfo.Kind.ITEM || target != null && it != target) continue;
			double[] p = WorldLabels.onScreen(it.x, it.top + 0.35, it.z);
			if (p == null) continue;
			double left = (LIFETIME - it.ticks) / 20.0;
			int color = left > 60 ? 0xFFFFFFFF : left > 15 ? 0xFFFFFF55 : 0xFFFF5555;
			String text = WorldLabels.time(left);
			if (showName.on()) text = it.name + (it.count > 1 ? " x" + it.count : "") + "  " + text;
			WorldLabels.text(e.gfx, p, text, color, scale.floatValue());
		}
	}
}
