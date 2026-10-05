package com.tatnat.client.modules.impl.utility;

import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.ui.render.Icons;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Restyled item tooltips (your own background and border colours, see
 * {@code TooltipRenderUtilMixin}) with extra info added at the bottom: durability, the item's id
 * and how many data components (NBT) it carries.
 */
public class BetterTooltips extends Module {
	public static BetterTooltips INSTANCE;

	public final ColorSetting background = add(new ColorSetting("Background Color", "Tooltip background", 0xF0141416, false));
	public final ColorSetting border = add(new ColorSetting("Border Color", "Tooltip border (Chroma works too)", 0xFFE5323E, true));
	private final BooleanSetting durability = add(new BooleanSetting("Show Durability", "Durability left on tools and armour", true));
	private final BooleanSetting itemId = add(new BooleanSetting("Show Item ID", "The item's id, like minecraft:diamond_sword", true));
	private final BooleanSetting components = add(new BooleanSetting("Show Data Count", "How many data components (NBT) the item has", false));

	public BetterTooltips() {
		super("Better Tooltips", "Nicer item tooltips with durability and item IDs", Category.UTILITY, false);
		icon = Icons.Icon.TOOLTIP;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	/** Called with the finished vanilla tooltip; returns it with our lines added. */
	public List<Component> extend(ItemStack stack, List<Component> lines, boolean advanced) {
		if (stack.isEmpty()) return lines;
		List<Component> out = new ArrayList<>(lines);
		if (durability.on() && stack.isDamageableItem() && !advanced) {
			int left = stack.getMaxDamage() - stack.getDamageValue();
			float f = left / (float) stack.getMaxDamage();
			ChatFormatting c = f > 0.6f ? ChatFormatting.GREEN : f > 0.3f ? ChatFormatting.YELLOW : ChatFormatting.RED;
			out.add(Component.literal("Durability: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(left + " / " + stack.getMaxDamage()).withStyle(c)));
		}
		if (itemId.on() && !advanced) {
			out.add(Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
		}
		if (components.on()) {
			out.add(Component.literal("Data: " + stack.getComponents().size() + " components").withStyle(ChatFormatting.DARK_GRAY));
		}
		return out;
	}
}
