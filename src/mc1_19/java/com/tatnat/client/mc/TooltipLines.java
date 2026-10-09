package com.tatnat.client.mc;

import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.modules.impl.utility.BetterTooltips;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Better Tooltips' extra lines for 1.21.11. */
public final class TooltipLines {
	private TooltipLines() {
	}

	public static List<Component> extend(ItemStack stack, List<Component> lines, boolean advanced) {
		if (stack.isEmpty()) return lines;
		BetterTooltips bt = BetterTooltips.INSTANCE;
		List<Component> out = new ArrayList<>(lines);
		String tier = com.tatnat.client.modules.impl.utility.TierTagger.line(stack.getRarity().ordinal(), stack.getRarity().name());
		if (tier != null) out.add(Math.min(1, out.size()), Component.literal(tier));
		if (com.tatnat.client.modules.impl.utility.ShulkerTooltips.active() && stack.getItem().getDescriptionId().contains("shulker_box")) {
			java.util.List<String> names = new java.util.ArrayList<>();
			java.util.List<Integer> counts = new java.util.ArrayList<>();
			net.minecraft.nbt.CompoundTag tag = stack.getTagElement("BlockEntityTag");
			if (tag != null && tag.contains("Items", 9)) {
				net.minecraft.nbt.ListTag items = tag.getList("Items", 10);
				for (int i = 0; i < items.size(); i++) {
					ItemStack in = ItemStack.of(items.getCompound(i));
					if (in.isEmpty()) continue;
					names.add(in.getHoverName().getString());
					counts.add(in.getCount());
				}
			}
			out.removeIf(c -> com.tatnat.client.modules.impl.utility.ShulkerTooltips.isVanillaLine(c.getString()));
			int at = Math.min(tier != null ? 2 : 1, out.size());
			for (String line : com.tatnat.client.modules.impl.utility.ShulkerTooltips.lines(names, counts)) out.add(at++, Component.literal(line));
		}
		if (!BetterTooltips.active()) return out;
		if (bt.durability.on() && stack.isDamageableItem() && !advanced) {
			int left = stack.getMaxDamage() - stack.getDamageValue();
			float f = left / (float) stack.getMaxDamage();
			ChatFormatting c = f > 0.6f ? ChatFormatting.GREEN : f > 0.3f ? ChatFormatting.YELLOW : ChatFormatting.RED;
			out.add(Component.literal("Durability: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(left + " / " + stack.getMaxDamage()).withStyle(c)));
		}
		if (bt.itemId.on() && !advanced) {
			out.add(Component.literal(Registry.ITEM.getKey(stack.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
		}
		if (bt.components.on()) {
			int tags = stack.getTag() == null ? 0 : stack.getTag().size();
			out.add(Component.literal("Data: " + tags + " NBT tags").withStyle(ChatFormatting.DARK_GRAY));
		}
		return out;
	}
}
