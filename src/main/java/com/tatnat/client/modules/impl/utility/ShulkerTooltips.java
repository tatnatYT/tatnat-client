package com.tatnat.client.modules.impl.utility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;

/**
 * Shulker Tooltips: hovering a shulker box lists everything inside it, merged per item
 * ("128x Diamond"), instead of vanilla's first five stacks (1.14+).
 */
public class ShulkerTooltips extends Module {
	private static ShulkerTooltips instance;

	private final SliderSetting maxLines = add(new SliderSetting("Max Lines", "Items listed before \"…and N more\"", 12, 3, 27, 1, ""));
	private final BooleanSetting showEmpty = add(new BooleanSetting("Show Empty Slots", "Add how many slots are still free", true));

	public ShulkerTooltips() {
		super("Shulker Tooltips", "See everything inside a shulker box", Category.UTILITY, true);
		icon = com.tatnat.client.ui.render.Icons.Icon.SHULKER;
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/**
	 * Builds the tooltip lines (with colour codes) for a box's contents, given each stack's name and
	 * count in slot order. Returns an empty list when the mod is off.
	 */
	public static List<String> lines(List<String> names, List<Integer> counts) {
		List<String> out = new ArrayList<>();
		if (!active()) return out;
		Map<String, Integer> merged = new LinkedHashMap<>();
		for (int i = 0; i < names.size(); i++) merged.merge(names.get(i), counts.get(i), Integer::sum);
		out.add("§7Contents:");
		if (merged.isEmpty()) {
			out.add("§8  Empty");
			return out;
		}
		int shown = 0, max = instance.maxLines.intValue();
		for (Map.Entry<String, Integer> e : merged.entrySet()) {
			if (shown++ >= max) break;
			out.add("§f  " + e.getValue() + "x §7" + e.getKey());
		}
		if (merged.size() > max) out.add("§8  …and " + (merged.size() - max) + " more");
		if (instance.showEmpty.on()) out.add("§8  " + Math.max(0, 27 - names.size()) + " empty slots");
		return out;
	}

	/** Vanilla's own content lines ("Diamond x32", "and 3 more...") that the full list replaces. */
	public static boolean isVanillaLine(String line) {
		return line.matches(".+ x\\d+") || line.matches("and \\d+ more\\.\\.\\.");
	}
}
