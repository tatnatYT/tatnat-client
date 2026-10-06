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
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Posts {@link Events.Render2D} after the vanilla HUD, so our elements sit on top of it. */
@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void tatnat$render2d(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		TatnatClient.EVENTS.post(new Events.Render2D(new GfxImpl(graphics), delta.getGameTimeDeltaPartialTick(false)));
	}

	/** The Crosshair mod draws its own, so vanilla's is skipped. */
	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void tatnat$crosshair(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		if (Crosshair.active()) ci.cancel();
	}
}
