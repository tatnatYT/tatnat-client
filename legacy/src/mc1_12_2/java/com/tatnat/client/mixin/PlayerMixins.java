package com.tatnat.client.mixin;

import java.util.List;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

		// Legacy Yarn names these two the wrong way round on 1.8.9: "getCapeId" returns the skin.
		@Inject(method = "getCapeId", at = @At("RETURN"), cancellable = true)
		private void tatnat$skin(CallbackInfoReturnable<Identifier> cir) {
			if (tatnat$isMe()) cir.setReturnValue(PlayerLooks.skin(cir.getReturnValue()));
		}

		@Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
		private void tatnat$model(CallbackInfoReturnable<String> cir) {
			if (tatnat$isMe()) cir.setReturnValue(PlayerLooks.model(cir.getReturnValue()));
		}

		// ...and "getSkinId()" returns the cape.
		@Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("RETURN"), cancellable = true)
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
	public abstract static class NameTag {
		private static boolean tatnat$inside;

		@Shadow
		protected abstract void renderLabelIfPresent(Entity entity, String text, double x, double y, double z, int maxDistance);

		// Plain Mixin 0.7 (Forge 1.8.9 has no MixinExtras): draw the label again with the new name.
		@Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
		private void tatnat$nameTag(Entity entity, String name, double x, double y, double z, int maxDistance, CallbackInfo ci) {
			if (tatnat$inside || name == null) return;
			String nick = NickHider.active() && entity == MinecraftClient.getInstance().player ? PlayerLooks.replaceName(name) : name;
			com.tatnat.client.modules.impl.utility.TierTagger.Tag tier = entity instanceof net.minecraft.entity.player.PlayerEntity ? com.tatnat.client.modules.impl.utility.TierTagger.nameTag(entity.getUuid()) : null;
			if (tier != null) nick = tier.legacy() + nick;
			String pop = com.tatnat.client.modules.impl.hud.TotemPops.tag(entity instanceof net.minecraft.entity.player.PlayerEntity ? entity.getName().asUnformattedString() : null);
			if (pop != null) nick = nick + " §c" + pop;
			if (nick.equals(name)) return;
			ci.cancel();
			tatnat$inside = true;
			try {
				renderLabelIfPresent(entity, nick, x, y, z, maxDistance);
			} finally {
				tatnat$inside = false;
			}
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

		/** Cull Logs: drop spam before it reaches the chat. */
		@org.spongepowered.asm.mixin.injection.Inject(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"), cancellable = true)
		private void tatnat$cull(Text message, int id, CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.utility.CullLogs.blocked(message.asUnformattedString())) ci.cancel();
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
			if (BetterTooltips.active() || com.tatnat.client.modules.impl.utility.TierTagger.active()) cir.setReturnValue(TooltipLines.extend((ItemStack) (Object) this, cir.getReturnValue(), advanced));
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

		/** Better Tooltips: recolour vanilla's gradient boxes (its fill is 0xF0100010, the rest is border).
		 * require = 0: on Forge this method only forwards to GuiUtils, see ForgeMixins.TooltipColors. */
		@ModifyArg(method = TOOLTIP, index = 4, require = 0, at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/gui/screen/Screen;fillGradient(IIIIII)V"))
		private int tatnat$backgroundFrom(int from) {
			if (!BetterTooltips.active()) return from;
			return from == 0xF0100010 ? BetterTooltips.INSTANCE.background.get() : BetterTooltips.INSTANCE.border.color(0);
		}

		@ModifyArg(method = TOOLTIP, index = 5, require = 0, at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/gui/screen/Screen;fillGradient(IIIIII)V"))
		private int tatnat$backgroundTo(int to) {
			if (!BetterTooltips.active()) return to;
			// The fill's two colours are equal; the border fades to a darker second colour.
			return to == 0xF0100010 ? BetterTooltips.INSTANCE.background.get() : BetterTooltips.INSTANCE.border.color(0.25);
		}

		// Scrollable Tooltips. Plain Mixin 0.7 (Forge 1.8.9 has no MixinExtras), so instead of wrapping
		// the method, HEAD pushes a translated matrix and RETURN pops it. Forge routes item tooltips
		// through its own 4-argument drawHoveringText (the vanilla one just calls it), so both are hooked;
		// the depth counter makes the outermost call the only one that acts.
		private static int tatnat$depth;
		private static boolean tatnat$pushed;
		private static int tatnat$x, tatnat$w, tatnat$h;
		private static float tatnat$max;

		@Inject(method = TOOLTIP, at = @At("HEAD"))
		private void tatnat$shift(List<String> lines, int x, int y, CallbackInfo ci) {
			tatnat$begin(lines, x);
		}

		@Inject(method = TOOLTIP, at = @At("RETURN"))
		private void tatnat$shiftEnd(List<String> lines, int x, int y, CallbackInfo ci) {
			tatnat$end();
		}

		@Inject(method = "drawHoveringText", at = @At("HEAD"), require = 0, remap = false)
		private void tatnat$shiftForge(List<String> lines, int x, int y, net.minecraft.client.font.TextRenderer font, CallbackInfo ci) {
			tatnat$begin(lines, x);
		}

		@Inject(method = "drawHoveringText", at = @At("RETURN"), require = 0, remap = false)
		private void tatnat$shiftForgeEnd(List<String> lines, int x, int y, net.minecraft.client.font.TextRenderer font, CallbackInfo ci) {
			tatnat$end();
		}

		private static void tatnat$begin(List<String> lines, int x) {
			if (tatnat$depth++ > 0 || !ScrollableTooltips.active() || lines.isEmpty()) return;
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
			if (max <= 0) return;
			tatnat$x = x;
			tatnat$w = w;
			tatnat$h = h;
			tatnat$max = max;
			tatnat$pushed = true;
			GlStateManager.pushMatrix();
			GlStateManager.translate(0, st.offset, 0);
		}

		private static void tatnat$end() {
			if (--tatnat$depth > 0 || !tatnat$pushed) return;
			tatnat$pushed = false;
			GlStateManager.popMatrix();
			MinecraftClient mc = MinecraftClient.getInstance();
			ScrollableTooltips st = ScrollableTooltips.INSTANCE;
			int screenH = new Window(mc).getHeight();
			int w = tatnat$w, h = tatnat$h;
			// Same placement as vanilla: right of the cursor, flipped left if it would leave the screen.
			int tx = tatnat$x + 12;
			if (tx + w > new Window(mc).getWidth()) tx -= 28 + w;
			int bw = st.barWidth.intValue();
			int bx = tx + w + 6;
			int trackH = screenH - 8;
			int barH = Math.max(12, Math.round(trackH * (screenH / (float) (h + 16))));
			int barY = 4 + Math.round((trackH - barH) * (-st.offset / tatnat$max));
			GlStateManager.disableLighting();
			GlStateManager.disableDepthTest();
			DrawableHelper.fill(bx, 4, bx + bw, 4 + trackH, 0x40000000);
			DrawableHelper.fill(bx, barY, bx + bw, barY + barH, st.barColor.color());
			GlStateManager.enableDepthTest();
		}
	}
}
