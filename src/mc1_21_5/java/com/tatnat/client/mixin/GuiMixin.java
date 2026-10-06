package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.mc.GfxImpl;
import com.tatnat.client.modules.impl.visual.Crosshair;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/** Posts {@link Events.Render2D} after the vanilla HUD, so our elements sit on top of it. */
@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void tatnat$render2d(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		TatnatClient.EVENTS.post(new Events.Render2D(new GfxImpl(graphics), delta.getGameTimeDeltaPartialTick(false)));
	}

	/** The Crosshair mod draws its own, so vanilla's is skipped. */
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void tatnat$crosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (Crosshair.active()) ci.cancel();
	}

	/** Potion Status replaces the vanilla effect icons in the top right (unless its setting says keep them). */
	@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
	private void tatnat$hideEffects(net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.DeltaTracker delta, CallbackInfo ci) {
		if (com.tatnat.client.modules.impl.hud.PotionStatus.hidesVanilla()) ci.cancel();
	}
}
