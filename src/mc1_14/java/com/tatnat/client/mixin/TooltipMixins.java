package com.tatnat.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tatnat.client.modules.impl.utility.BetterTooltips;
import com.tatnat.client.modules.impl.utility.ScrollableTooltips;

import net.minecraft.client.gui.Font;
import com.mojang.math.Matrix4f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.tatnat.client.mc.Graphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Better Tooltips (look + extra lines) and Scrollable Tooltips (wheel + scrollbar). */
public final class TooltipMixins {
	private TooltipMixins() {
	}

	@Mixin(ItemStack.class)
	public static class Lines {
		@Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
		private void tatnat$lines(Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
			if (BetterTooltips.active() || com.tatnat.client.modules.impl.utility.TierTagger.active() || com.tatnat.client.modules.impl.utility.ShulkerTooltips.active()) cir.setReturnValue(com.tatnat.client.mc.TooltipLines.extend((ItemStack) (Object) this, cir.getReturnValue(), flag.isAdvanced()));
		}
	}

	/** Lets the mouse handler see which slot is hovered (this version has no screen scroll hook). */
	@Mixin(AbstractContainerScreen.class)
	public interface SlotAccess {
		@org.spongepowered.asm.mixin.gen.Accessor("hoveredSlot")
		Slot tatnat$hovered();
	}

	@Mixin(AbstractContainerScreen.class)
	public static class Wheel {
		@Shadow
		protected Slot hoveredSlot;

		@Inject(method = "render", at = @At("HEAD"))
		private void tatnat$hover(int mx, int my, float pt, CallbackInfo ci) {
			if (ScrollableTooltips.active()) ScrollableTooltips.INSTANCE.hovering(hoveredSlot != null ? hoveredSlot.getItem() : ItemStack.EMPTY);
		}
	}

	@Mixin(Screen.class)
	public static class Shift {
		/** On 1.16 this public overload is the one that actually draws every tooltip. */
		private static final String TOOLTIP = "renderTooltip(Ljava/util/List;II)V";

		/**
		 * Better Tooltips: 1.16 draws the tooltip box as gradient rects straight from
		 * renderTooltip; recolour them (vanilla's fill is 0xF0100010, the rest is border).
		 */
		@WrapOperation(method = TOOLTIP, at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/gui/screens/Screen;fillGradient(IIIIII)V"))
		private void tatnat$background(Screen self, int x1, int y1, int x2, int y2, int from, int to, Operation<Void> original) {
			if (!BetterTooltips.active()) {
				original.call(self, x1, y1, x2, y2, from, to);
				return;
			}
			BetterTooltips bt = BetterTooltips.INSTANCE;
			if (from == 0xF0100010) {
				int bg = bt.background.get();
				original.call(self, x1, y1, x2, y2, bg, bg);
			} else {
				original.call(self, x1, y1, x2, y2, bt.border.color(0), bt.border.color(0.25));
			}
		}

		@WrapMethod(method = TOOLTIP)
		private void tatnat$shift(List<String> lines, int x, int y, Operation<Void> original) {
			Screen screen = (Screen) (Object) this;
			Font font = net.minecraft.client.Minecraft.getInstance().font;
			Graphics g = new Graphics();
			if (!ScrollableTooltips.active() || lines.isEmpty()) {
				original.call(lines, x, y);
				return;
			}
			int w = 0, h = 0;
			for (String c : lines) w = Math.max(w, font.width(c));
			// Vanilla 1.16: 8px for the first line, then 10px per line plus a 2px gap.
			h = 8 + (lines.size() > 1 ? 2 + (lines.size() - 1) * 10 : 0);
			int screenH = g.guiHeight();
			ScrollableTooltips st = ScrollableTooltips.INSTANCE;
			// Only tooltips taller than the screen scroll; clamp so you can't scroll past either end.
			float max = Math.max(0, h + 16 - screenH);
			st.offset = Math.max(-max, Math.min(0, st.offset));
			if (max <= 0) {
				original.call(lines, x, y);
				return;
			}
			// Vanilla draws tooltips on the GL matrix here, so shift that.
			com.mojang.blaze3d.platform.GlStateManager.pushMatrix();
			com.mojang.blaze3d.platform.GlStateManager.translatef(0, st.offset, 0);
			original.call(lines, x, y);
			com.mojang.blaze3d.platform.GlStateManager.popMatrix();
			// Same placement as vanilla: right of the cursor, flipped left if it would leave the screen.
			int tx = x + 12;
			if (tx + w > screen.width) tx -= 28 + w;
			int bw = st.barWidth.intValue();
			int bx = tx + w + 6;
			int trackH = screenH - 8;
			int barH = Math.max(12, Math.round(trackH * (screenH / (float) (h + 16))));
			int barY = 4 + Math.round((trackH - barH) * (-st.offset / max));
			g.fill(bx, 4, bx + bw, 4 + trackH, 0x40000000);
			g.fill(bx, barY, bx + bw, barY + barH, st.barColor.color());
		}
	}
}
