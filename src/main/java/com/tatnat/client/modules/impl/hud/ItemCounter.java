package com.tatnat.client.modules.impl.hud;

import java.util.List;
import java.util.Map;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.platform.ItemInfo;

/**
 * The item in your hand, with how many of it you carry in your whole inventory
 * ("[pearl] Ender Pearl  x16"). Hidden while your hand is empty.
 */
public class ItemCounter extends HudModule {
	private final BooleanSetting showName = add(new BooleanSetting("Show Name", "The item's name next to the icon", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind it", true));

	public ItemCounter() {
		super("Item Counter", "How many of the item in your hand you have", false, 0.75, 0.075);
		icon = com.tatnat.client.ui.render.Icons.Icon.STACK;
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		Object stack = null;
		String name = null;
		int count = 0;
		if (game().inWorld()) {
			String key = game().heldItemKey();
			List<ItemInfo> withHeld = game().armor(true, false), without = game().armor(false, false);
			if (key != null && withHeld.size() > without.size()) {
				stack = withHeld.get(0).stack;
				Integer n = game().inventoryCounts().get(key);
				count = n == null ? withHeld.get(0).count : n;
				name = key.substring(key.indexOf('|') + 1);
				if (name.isEmpty() || name.equals("null")) name = null;
			}
		}
		if (stack == null) {
			if (!preview) return size(0, 0);
			List<ItemInfo> sample = game().armor(true, true);
			if (sample.isEmpty()) return size(0, 0);
			stack = sample.get(0).stack;
			count = 16;
			name = "Diamond Sword";
		}
		String num = "x" + count;
		String text = showName.on() && name != null ? name : "";
		int pad = background.on() ? 4 : 0;
		int textW = text.isEmpty() ? 0 : g.mcTextWidth(text, false) + 6;
		int w = pad + 16 + 4 + textW + g.mcTextWidth(num, true) + pad, h = 16 + pad * 2;
		if (background.on()) g.rect(0, 0, w, h, 0x6F000000);
		g.item(stack, pad, pad);
		int tx = pad + 20, ty = pad + 4;
		if (!text.isEmpty()) {
			g.mcText(text, tx, ty, 0xFFFFFFFF, true, false);
			tx += textW;
		}
		g.mcText(num, tx, ty, count <= 1 ? 0xFFFF5555 : 0xFFFFD24A, true, true);
		return size(w, h);
	}

	/** Total of every stack whose name or id contains one of the comma-separated words (Totem Counter, UHC Overlay). */
	static int count(String words) {
		String[] parts = words.toLowerCase(java.util.Locale.ROOT).split(",");
		int n = 0;
		for (Map.Entry<String, Integer> e : com.tatnat.client.TatnatClient.game().inventoryCounts().entrySet()) {
			String key = e.getKey().toLowerCase(java.util.Locale.ROOT);
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
}
