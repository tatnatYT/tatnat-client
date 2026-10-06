package com.tatnat.client.mixin;

import java.util.List;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlStateManager;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.mc.PlayerLooks;
import com.tatnat.client.mc.TooltipLines;
import com.tatnat.client.modules.impl.utility.BetterTooltips;
import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.utility.ScrollableTooltips;
import com.tatnat.client.modules.impl.visual.FovModifier;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Player look, names, chat, tooltips and Freecam input for the legacy versions. */
public final class PlayerMixins {
	private PlayerMixins() {
	}

	/** Your own skin, cape and model (Nick Hider, Custom Capes) and the sprint/fly FOV change. */
	@Mixin(AbstractClientPlayerEntity.class)
	public static abstract class Looks {
		private boolean tatnat$isMe() {
			return (Object) this == MinecraftClient.getInstance().player;
		}

		@Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("RETURN"), cancellable = true)
		private void tatnat$skin(CallbackInfoReturnable<Identifier> cir) {
			if (tatnat$isMe()) cir.setReturnValue(PlayerLooks.skin(cir.getReturnValue()));
		}

		@Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
		private void tatnat$model(CallbackInfoReturnable<String> cir) {
			if (tatnat$isMe()) cir.setReturnValue(PlayerLooks.model(cir.getReturnValue()));
		}

		@Inject(method = "getCapeId", at = @At("RETURN"), cancellable = true)
		private void tatnat$cape(CallbackInfoReturnable<Identifier> cir) {
			if (tatnat$isMe()) cir.setReturnValue(PlayerLooks.cape(cir.getReturnValue()));
		}

		@Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
		private void tatnat$staticFov(CallbackInfoReturnable<Float> cir) {
			if (FovModifier.active() && FovModifier.INSTANCE.staticFov.on()) cir.setReturnValue(1.0f);
		}
	}

	/** Nick Hider in the tab list. */
	@Mixin(PlayerListHud.class)
	public static class TabName {
		@Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
		private void tatnat$tabName(PlayerListEntry info, CallbackInfoReturnable<String> cir) {
			if (!NickHider.active() || !NickHider.INSTANCE.inTab.on()) return;
			if (PlayerLooks.isMe(info.getProfile().getId())) cir.setReturnValue(PlayerLooks.replaceName(cir.getReturnValue()));
		}
	}

	/** Nick Hider on your name tag (third person). */
	@Mixin(EntityRenderer.class)
	public static class NameTag {
		@ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
		private String tatnat$nameTag(String name, @Local(argsOnly = true) Entity entity) {
			if (NickHider.active() && entity == MinecraftClient.getInstance().player && name != null) return PlayerLooks.replaceName(name);
			return name;
		}
	}

	/** Every chat line passes through here: Auto GG reads it, Nick Hider rewrites your name in it. */
	@Mixin(ChatHud.class)
	public static class Chat {
		@ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"), argsOnly = true)
		private Text tatnat$chat(Text message) {
			TatnatClient.EVENTS.post(new Events.Chat(message.asUnformattedString()));
			if (NickHider.active() && NickHider.INSTANCE.inChat.on()) return PlayerLooks.replaceName(message);
			return message;
		}
	}

	/** While Freecam is on, your player gets no movement input at all. */
	@Mixin(KeyboardInput.class)
	public static class FreezePlayer {
		@Inject(method = "tick", at = @At("TAIL"))
		private void tatnat$freeze(CallbackInfo ci) {
			if (!Freecam.active()) return;
			Input self = (Input) (Object) this;
			self.movementSideways = self.movementForward = 0f;
			self.jumping = self.sneaking = false;
		}
	}

	/** Better Tooltips' extra lines. */
	@Mixin(ItemStack.class)
	public static class TooltipText {
		@Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
		private void tatnat$lines(PlayerEntity player, boolean advanced, CallbackInfoReturnable<List<String>> cir) {
			if (BetterTooltips.active()) cir.setReturnValue(TooltipLines.extend((ItemStack) (Object) this, cir.getReturnValue(), advanced));
		}
	}

	/** Scrollable Tooltips: remember the hovered item; the wheel scrolls its tooltip. */
	@Mixin(HandledScreen.class)
	public static class TooltipHover {
		@Shadow
		private Slot focusedSlot;

		@Inject(method = "render", at = @At("HEAD"))
		private void tatnat$hover(int mx, int my, float pt, CallbackInfo ci) {
			if (ScrollableTooltips.active()) ScrollableTooltips.INSTANCE.hovering(focusedSlot != null && focusedSlot.hasStack() ? focusedSlot.getStack() : null);
		}
	}

	@Mixin(Screen.class)
	public static class Tooltip {
		private static final String TOOLTIP = "renderTooltip(Ljava/util/List;II)V";

		@Inject(method = "handleMouse", at = @At("HEAD"))
		private void tatnat$wheel(CallbackInfo ci) {
			if (!((Object) this instanceof HandledScreen) || !ScrollableTooltips.active()) return;
			int wheel = Mouse.getEventDWheel();
			if (wheel != 0) ScrollableTooltips.INSTANCE.scroll(Math.signum(wheel));
		}

		/** Better Tooltips: recolour vanilla's gradient boxes (its fill is 0xF0100010, the rest is border). */
		@WrapOperation(method = TOOLTIP, at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/gui/screen/Screen;fillGradient(IIIIII)V"))
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
			if (!ScrollableTooltips.active() || lines.isEmpty()) {
				original.call(lines, x, y);
				return;
			}
			MinecraftClient mc = MinecraftClient.getInstance();
			int w = 0;
			for (String c : lines) w = Math.max(w, mc.textRenderer.getStringWidth(c));
			// Vanilla: 8px for the first line, then 10px per line plus a 2px gap.
			int h = 8 + (lines.size() > 1 ? 2 + (lines.size() - 1) * 10 : 0);
			int screenH = new Window(mc).getHeight();
			ScrollableTooltips st = ScrollableTooltips.INSTANCE;
			// Only tooltips taller than the screen scroll; clamp so you can't scroll past either end.
			float max = Math.max(0, h + 16 - screenH);
			st.offset = Math.max(-max, Math.min(0, st.offset));
			if (max <= 0) {
				original.call(lines, x, y);
				return;
			}
			GlStateManager.pushMatrix();
			GlStateManager.translate(0, st.offset, 0);
			original.call(lines, x, y);
			GlStateManager.popMatrix();
			// Same placement as vanilla: right of the cursor, flipped left if it would leave the screen.
			int tx = x + 12;
			if (tx + w > screen.width) tx -= 28 + w;
			int bw = st.barWidth.intValue();
			int bx = tx + w + 6;
			int trackH = screenH - 8;
			int barH = Math.max(12, Math.round(trackH * (screenH / (float) (h + 16))));
			int barY = 4 + Math.round((trackH - barH) * (-st.offset / max));
			GlStateManager.disableLighting();
			GlStateManager.disableDepthTest();
			DrawableHelper.fill(bx, 4, bx + bw, 4 + trackH, 0x40000000);
			DrawableHelper.fill(bx, barY, bx + bw, barY + barH, st.barColor.color());
			GlStateManager.enableDepthTest();
		}
	}
}
