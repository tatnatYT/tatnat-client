package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.joml.Matrix4f;

import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.visual.FovModifier;
import com.tatnat.client.modules.impl.visual.Zoom;
import com.tatnat.client.mc.GameImpl;

import net.minecraft.client.Minecraft;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

/** Freecam hand hiding (Zoom lives in CameraFovMixin on 26.1). */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	/** No floating hand while flying around in Freecam. */
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void tatnat$hideHand(net.minecraft.client.renderer.state.level.CameraRenderState camera, net.minecraft.client.renderer.state.level.PlayerRenderState player, com.mojang.renderpearl.api.textures.GpuTextureView target, CallbackInfo ci) {
		if (Freecam.active()) ci.cancel();
	}
}
