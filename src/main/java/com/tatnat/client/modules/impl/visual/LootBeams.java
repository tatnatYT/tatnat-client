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
	private final TextSetting filter = add(new TextSetting("Item Filter", "Item names that get a beam, separated by commas (empty = every item)",
			"diamond, netherite, totem, enchanted, elytra, ancient debris, golden apple", 256));
	private final SliderSetting height = add(new SliderSetting("Beam Height", "How tall the beam is", 12, 2, 64, 1, " blocks"));
	private final SliderSetting range = add(new SliderSetting("Range", "Only items this close", 48, 8, 64, 1, " blocks"));
	private final ColorSetting color = add(new ColorSetting("Color", "Beam colour", 0xFF4EB1FF, true));

	public LootBeams() {
		super("Loot Beams", "Beams of light over valuable dropped items", Category.VISUAL, false);
		icon = Icons.Icon.SPARKLE;
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
		for (EntityInfo it : game().entities(range.get())) {
			if (it.kind != EntityInfo.Kind.ITEM || !wanted(it.name)) continue;
			if (scale < 0) scale = RenderUtils.beginPixels(e.gfx);
			double[] prev = null;
			int steps = 12;
			for (int i = 0; i <= steps; i++) {
				double y = it.top - 0.2 + height.get() * i / steps;
				double[] p = WorldProjector.project(it.x, y, it.z);
				if (p[3] > 0.05 && prev != null && prev[3] > 0.05) {
					float w = (float) Math.max(1, Math.min(5, 24 / Math.max(1, p[3]))) * scale / 2f;
					int a = (int) (0xC0 * (1 - i / (double) steps)) + 0x20;
					Icons.thickLine(e.gfx, (float) prev[0] * scale, (float) prev[1] * scale, (float) p[0] * scale, (float) p[1] * scale, w,
							Colors.withAlpha(color.color(i / (double) steps), a));
				}
				prev = p;
			}
		}
		if (scale >= 0) RenderUtils.end(e.gfx);
	}
}
