package com.tatnat.client.modules.impl.visual;

import java.util.Locale;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.platform.EntityInfo;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.util.WorldProjector;

/** A beam of light shooting up from valuable dropped items, so you can't miss them. */
public class LootBeams extends Module {
	/** Beam parts closer to the camera than this (blocks) aren't drawn. */
	private static final double NEAR = 0.5;

	private final TextSetting filter = add(new TextSetting("Item Filter", "Item names that get a beam, separated by commas (empty = every item)",
			"diamond, netherite, totem, enchanted, elytra, ancient debris, golden apple", 256));
	private final SliderSetting height = add(new SliderSetting("Beam Height", "How tall the beam is", 12, 2, 64, 1, " blocks"));
	private final SliderSetting range = add(new SliderSetting("Range", "Only items this close", 48, 8, 64, 1, " blocks"));
	private final ColorSetting color = add(new ColorSetting("Color", "Beam colour", 0xFF4EB1FF, true));

	public LootBeams() {
		super("Loot Beams", "Beams of light over valuable dropped items", Category.VISUAL, false);
		icon = Icons.Icon.BEAM;
	}

	private boolean wanted(String name) {
		String f = filter.get().trim().toLowerCase(Locale.ROOT);
		if (f.isEmpty()) return true;
		String n = name.toLowerCase(Locale.ROOT);
		for (String part : f.split(",")) if (!part.trim().isEmpty() && n.contains(part.trim())) return true;
		return false;
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden()) return;
		int scale = -1;
		com.tatnat.client.ui.render.RectBatch g = com.tatnat.client.ui.render.RectBatch.of(e.gfx);
		for (EntityInfo it : game().entities(range.get())) {
			if (it.kind != EntityInfo.Kind.ITEM || !wanted(it.name)) continue;
			if (scale < 0) scale = RenderUtils.beginPixels(g);
			// Centred on the block the item lies in, from its floor up.
			double bx = Math.floor(it.x) + 0.5, bz = Math.floor(it.z) + 0.5, by = Math.floor(it.top - 0.25 + 0.01); // items are 0.25 tall
			int steps = 12;
			double prevY = by;
			double[] prev = WorldProjector.project(bx, by, bz);
			for (int i = 1; i <= steps; i++) {
				double y = by + height.get() * i / steps;
				double[] p = WorldProjector.project(bx, y, bz);
				double[] from = prev, to = p;
				// Clip the segment at a near plane: points right in front of the camera project
				// thousands of pixels away (stray lines across the screen, and very slow to draw).
				if (from[3] < NEAR && to[3] < NEAR) {
					prev = p;
					prevY = y;
					continue;
				}
				if (from[3] < NEAR) from = WorldProjector.project(bx, prevY + (y - prevY) * (NEAR - from[3]) / (to[3] - from[3]), bz);
				else if (to[3] < NEAR) to = WorldProjector.project(bx, prevY + (y - prevY) * (NEAR - from[3]) / (to[3] - from[3]), bz);
				float w = (float) Math.max(1, Math.min(5, 24 / Math.max(1, to[3]))) * scale / 2f;
				int alpha = (int) (0xC0 * (1 - i / (double) steps)) + 0x20;
				Icons.thickLine(g, (float) from[0] * scale, (float) from[1] * scale, (float) to[0] * scale, (float) to[1] * scale, w,
						Colors.withAlpha(color.color(i / (double) steps), alpha));
				prev = p;
				prevY = y;
			}
		}
		g.flush();
		if (scale >= 0) RenderUtils.end(g);
	}
}
