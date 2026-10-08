package com.tatnat.client.mc;

import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.modules.impl.utility.BetterTooltips;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

/** Better Tooltips' extra lines. Tooltips are plain strings with formatting codes here. */
public final class TooltipLines {
	private TooltipLines() {
	}

	public static List<String> extend(ItemStack stack, List<String> lines, boolean advanced) {
		if (stack == null) return lines;
		BetterTooltips bt = BetterTooltips.INSTANCE;
		List<String> out = new ArrayList<>(lines);
		String tier = com.tatnat.client.modules.impl.utility.TierTagger.line(stack.getRarity().ordinal(), stack.getRarity().name());
		if (tier != null) out.add(Math.min(1, out.size()), tier);
		if (!BetterTooltips.active()) return out;
		if (bt.durability.on() && stack.isDamageable() && !advanced) {
			int left = stack.getMaxDamage() - stack.getDamage();
			float f = left / (float) stack.getMaxDamage();
			Formatting c = f > 0.6f ? Formatting.GREEN : f > 0.3f ? Formatting.YELLOW : Formatting.RED;
			out.add(Formatting.GRAY + "Durability: " + c + left + " / " + stack.getMaxDamage());
		}
		if (bt.itemId.on() && !advanced) {
			out.add(Formatting.DARK_GRAY + String.valueOf(Item.REGISTRY.getIdentifier(stack.getItem())));
		}
		if (bt.components.on()) {
			int tags = stack.getNbt() == null ? 0 : stack.getNbt().getKeys().size();
			out.add(Formatting.DARK_GRAY + "Data: " + tags + " NBT tags");
		}
		return out;
	}
}
