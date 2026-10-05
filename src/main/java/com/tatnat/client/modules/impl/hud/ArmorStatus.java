package com.tatnat.client.modules.impl.hud;

import java.util.List;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.platform.ItemInfo;
import com.tatnat.client.ui.render.Icons;

/** Your armour (and held item) as real 3D item icons with durability. */
public class ArmorStatus extends HudModule {
	private final ModeSetting durability = add(new ModeSetting("Durability", "How durability is shown next to each item", "Numbers", "Numbers", "Percentage", "Off"));
	private final ModeSetting layout = add(new ModeSetting("Layout", "Stack items vertically or side by side", "Vertical", "Vertical", "Horizontal"));
	private final BooleanSetting heldItem = add(new BooleanSetting("Held Item", "Also show the item in your hand", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the items", false));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the numbers", true));

	public ArmorStatus() {
		super("Armor Status", "Shows your armour and its durability", false, 1.0, 0.6);
		icon = Icons.Icon.ARMOR;
	}

	@Override
	public boolean hasContent() {
		return !game().armor(heldItem.on(), false).isEmpty();
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		List<ItemInfo> items = game().armor(heldItem.on(), preview);
		boolean vertical = layout.is("Vertical");
		int textW = 0;
		for (ItemInfo s : items) textW = Math.max(textW, g.mcTextWidth(label(s), false));
		int cell = vertical ? 18 : 18 + (textW > 0 ? textW + 2 : 0);
		int w = vertical ? 18 + (textW > 0 ? textW + 3 : 0) : Math.max(1, items.size() * cell);
		int h = vertical ? Math.max(1, items.size() * 18) : 18;
		if (background.on()) g.rect(-2, -2, w + 2, h + 2, 0x6F000000);

		for (int i = 0; i < items.size(); i++) {
			ItemInfo s = items.get(i);
			int x = vertical ? 0 : i * cell, y = vertical ? i * 18 : 0;
			g.item(s.stack, x + 1, y + 1);
			String text = label(s);
			if (!text.isEmpty()) g.mcText(text, x + 19, y + 5, durabilityColor(s), shadow.on(), false);
		}
		return size(w, h);
	}

	private String label(ItemInfo s) {
		if (durability.is("Off") || !s.damageable) return s.count > 1 ? String.valueOf(s.count) : "";
		return durability.is("Percentage") ? (s.left() * 100 / Math.max(1, s.maxDamage)) + "%" : String.valueOf(s.left());
	}

	private static int durabilityColor(ItemInfo s) {
		if (!s.damageable) return 0xFFFFFFFF;
		float f = s.left() / (float) Math.max(1, s.maxDamage);
		return f > 0.6f ? 0xFF55FF55 : f > 0.3f ? 0xFFFFFF55 : f > 0.1f ? 0xFFFFAA00 : 0xFFFF5555;
	}
}
