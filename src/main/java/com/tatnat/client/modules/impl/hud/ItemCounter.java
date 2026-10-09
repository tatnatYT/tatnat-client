package com.tatnat.client.modules.impl.hud;

import java.util.Locale;
import java.util.Map;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.TextSetting;

/** {@code Blocks: 640}: how many of some item you carry. Matches item names, e.g. "wool, planks". */
public class ItemCounter extends TextHudModule {
	private final TextSetting label = add(new TextSetting("Label", "Text in front of the number", "Blocks", 24));
	private final TextSetting filter = add(new TextSetting("Item Filter", "Item names to count, separated by commas", "wool, planks, cobblestone, terracotta", 256));

	public ItemCounter() {
		super("Item Counter", "Counts how many of an item you have", false, 0.75, 0.075);
		icon = com.tatnat.client.ui.render.Icons.Icon.STACK;
	}

	/** Total of every stack whose name or id contains one of the comma-separated words. */
	static int count(String words) {
		String[] parts = words.toLowerCase(Locale.ROOT).split(",");
		int n = 0;
		for (Map.Entry<String, Integer> e : com.tatnat.client.TatnatClient.game().inventoryCounts().entrySet()) {
			String key = e.getKey().toLowerCase(Locale.ROOT);
			for (String p : parts) {
				String w = p.trim();
				if (!w.isEmpty() && (key.contains(w) || key.contains(w.replace(' ', '_')))) {
					n += e.getValue();
					break;
				}
			}
		}
		return n;
	}

	@Override
	protected String label() {
		return label.get();
	}

	@Override
	protected String value(boolean preview) {
		return preview ? "640" : String.valueOf(count(filter.get()));
	}
}
