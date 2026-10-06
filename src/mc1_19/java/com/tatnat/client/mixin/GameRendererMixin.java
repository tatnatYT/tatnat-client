package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.visual.FovModifier;
import com.tatnat.client.modules.impl.visual.Zoom;
import com.tatnat.client.mc.GameImpl;

import net.minecraft.client.Minecraft;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

/** Applies Zoom to the world FOV (not the hand, which uses {@code useFovSetting = false}). */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void tatnat$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
		if (!useFovSetting) return;
		double fov = cir.getReturnValue();
		// FOV Modifier scales by custom / vanilla setting, so underwater and similar effects still apply.
		if (FovModifier.active()) fov *= FovModifier.INSTANCE.fov.floatValue() / Minecraft.getInstance().options.fov().get();
		Zoom zoom = Zoom.INSTANCE;
		if (zoom != null && zoom.isEnabled()) fov /= (float) zoom.update();
		GameImpl.lastFov = (float) fov;
		cir.setReturnValue(fov);
	}

	/** No floating hand while flying around in Freecam. */
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void tatnat$hideHand(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.Camera camera, float partialTick, CallbackInfo ci) {
		if (Freecam.active()) ci.cancel();
	}
}
