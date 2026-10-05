package com.tatnat.client.modules.impl.hud;

import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ModeSetting;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Your armour (and held item) as real 3D item icons with durability. */
public class ArmorStatus extends HudModule {
	private final ModeSetting durability = add(new ModeSetting("Durability", "How durability is shown next to each item", "Numbers", "Numbers", "Percentage", "Off"));
	private final ModeSetting layout = add(new ModeSetting("Layout", "Stack items vertically or side by side", "Vertical", "Vertical", "Horizontal"));
	private final BooleanSetting heldItem = add(new BooleanSetting("Held Item", "Also show the item in your hand", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the items", false));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the numbers", true));

	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	public ArmorStatus() {
		super("Armor Status", "Shows your armour and its durability", false, 1.0, 0.6);
		icon = com.tatnat.client.ui.render.Icons.Icon.ARMOR;
	}

	private List<ItemStack> items(boolean preview) {
		List<ItemStack> list = new ArrayList<>();
		if (mc.player != null) {
			if (heldItem.on() && !mc.player.getMainHandItem().isEmpty()) list.add(mc.player.getMainHandItem());
			for (EquipmentSlot slot : SLOTS) {
				ItemStack s = mc.player.getItemBySlot(slot);
				if (!s.isEmpty()) list.add(s);
			}
		}
		if (list.isEmpty() && preview) {
			// Sample set so the element can be positioned before you own any armour.
			if (heldItem.on()) list.add(new ItemStack(Items.DIAMOND_SWORD));
			list.add(new ItemStack(Items.DIAMOND_HELMET));
			list.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
			list.add(new ItemStack(Items.DIAMOND_LEGGINGS));
			list.add(new ItemStack(Items.DIAMOND_BOOTS));
		}
		return list;
	}

	@Override
	public boolean hasContent() {
		return !items(false).isEmpty();
	}

	@Override
	protected long draw(GuiGraphics g, boolean preview) {
		List<ItemStack> items = items(preview);
		boolean vertical = layout.is("Vertical");
		int textW = 0;
		for (ItemStack s : items) textW = Math.max(textW, mc.font.width(label(s)));
		int cell = vertical ? 18 : 18 + (textW > 0 ? textW + 2 : 0);
		int w = vertical ? 18 + (textW > 0 ? textW + 3 : 0) : Math.max(1, items.size() * cell);
		int h = vertical ? Math.max(1, items.size() * 18) : 18;
		if (background.on()) g.fill(-2, -2, w + 2, h + 2, 0x6F000000);

		for (int i = 0; i < items.size(); i++) {
			ItemStack s = items.get(i);
			int x = vertical ? 0 : i * cell, y = vertical ? i * 18 : 0;
			g.renderItem(s, x + 1, y + 1);
			// Vanilla's own durability bar under the icon.
			g.renderItemDecorations(mc.font, s, x + 1, y + 1, "");
			String text = label(s);
			if (!text.isEmpty()) g.drawString(mc.font, text, x + 19, y + 5, durabilityColor(s), shadow.on());
		}
		return size(w, h);
	}

	private String label(ItemStack s) {
		if (durability.is("Off")) return s.getCount() > 1 ? String.valueOf(s.getCount()) : "";
		if (!s.isDamageableItem()) return s.getCount() > 1 ? String.valueOf(s.getCount()) : "";
		int left = s.getMaxDamage() - s.getDamageValue();
		return durability.is("Percentage") ? (left * 100 / s.getMaxDamage()) + "%" : String.valueOf(left);
	}

	private static int durabilityColor(ItemStack s) {
		if (!s.isDamageableItem()) return 0xFFFFFFFF;
		float f = (s.getMaxDamage() - s.getDamageValue()) / (float) s.getMaxDamage();
		return f > 0.6f ? 0xFF55FF55 : f > 0.3f ? 0xFFFFFF55 : f > 0.1f ? 0xFFFFAA00 : 0xFFFF5555;
	}
}
