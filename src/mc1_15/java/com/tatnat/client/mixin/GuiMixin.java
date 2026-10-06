package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.mc.GfxImpl;
import com.tatnat.client.modules.impl.visual.Crosshair;

import net.minecraft.client.gui.Gui;
import com.tatnat.client.mc.Graphics;

/** Posts {@link Events.Render2D} after the vanilla HUD, so our elements sit on top of it. */
@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void tatnat$render2d(float partialTick, CallbackInfo ci) {
		// Chat and the hotbar leave depth behind here, which would hide parts of our HUD.
		com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, net.minecraft.client.Minecraft.ON_OSX);
		TatnatClient.EVENTS.post(new Events.Render2D(new GfxImpl(new Graphics()), partialTick));
	}

	/** The Crosshair mod draws its own, so vanilla's is skipped. */
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void tatnat$crosshair(CallbackInfo ci) {
		if (Crosshair.active()) ci.cancel();
	}
}
