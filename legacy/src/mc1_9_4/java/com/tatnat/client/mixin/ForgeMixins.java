package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.mc.HudRender;
import com.tatnat.client.modules.impl.utility.BetterTooltips;

/**
 * Hooks into Forge's own classes; listed only in tatnatclient.forge.mixins.json, which only the Forge
 * 1.8.9 jar loads. Forge classes ship with SRG member names, hence remap = false and SRG names here.
 */
public final class ForgeMixins {
	private ForgeMixins() {
	}

	/** Forge replaces the in-game overlay with GuiIngameForge, which never calls vanilla's renderGameOverlay. */
	@Mixin(targets = "net.minecraftforge.client.GuiIngameForge", remap = false)
	public static class Hud {
		@Inject(method = "func_175180_a(F)V", at = @At("TAIL"), remap = false)
		private void tatnat$render2d(float partialTick, CallbackInfo ci) {
			HudRender.render(partialTick);
		}
	}

	/** Better Tooltips on Forge: GuiScreen's tooltips are drawn by GuiUtils (fill 0xF0100010, the rest is border). */
	@Mixin(targets = "net.minecraftforge.fml.client.config.GuiUtils", remap = false)
	public static class TooltipColors {
		private static final String DRAW = "drawHoveringText";
		private static final String GRADIENT = "Lnet/minecraftforge/fml/client/config/GuiUtils;drawGradientRect(IIIIIII)V";

		@ModifyArg(method = DRAW, index = 5, at = @At(value = "INVOKE", target = GRADIENT, remap = false), remap = false)
		private static int tatnat$from(int from) {
			if (!BetterTooltips.active()) return from;
			return from == 0xF0100010 ? BetterTooltips.INSTANCE.background.get() : BetterTooltips.INSTANCE.border.color(0);
		}

		@ModifyArg(method = DRAW, index = 6, at = @At(value = "INVOKE", target = GRADIENT, remap = false), remap = false)
		private static int tatnat$to(int to) {
			if (!BetterTooltips.active()) return to;
			return to == 0xF0100010 ? BetterTooltips.INSTANCE.background.get() : BetterTooltips.INSTANCE.border.color(0.25);
		}
	}
}
