package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;

/**
 * Runs every frame inside the world renderer's gizmo pass (whether or not F3 is open), so
 * {@link Events.Gizmos} handlers can add boxes, lines and floating text to the world.
 */
@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void tatnat$gizmos(Frustum frustum, double camX, double camY, double camZ, float partialTick, CallbackInfo ci) {
		TatnatClient.EVENTS.post(new Events.Gizmos(frustum, camX, camY, camZ, partialTick));
	}
}
