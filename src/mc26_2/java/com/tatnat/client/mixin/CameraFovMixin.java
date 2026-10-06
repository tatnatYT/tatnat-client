package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.mc.GameImpl;
import com.tatnat.client.modules.impl.visual.FovModifier;
import com.tatnat.client.modules.impl.visual.Zoom;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

/** Applies Zoom and FOV Modifier to the world FOV. On 26.1 the camera works it out (the hand has its own). */
@Mixin(Camera.class)
public class CameraFovMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void tatnat$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
		float fov = cir.getReturnValue();
		// FOV Modifier scales by custom / vanilla setting, so underwater and similar effects still apply.
		if (FovModifier.active()) fov *= FovModifier.INSTANCE.fov.floatValue() / Minecraft.getInstance().options.fov().get();
		Zoom zoom = Zoom.INSTANCE;
		if (zoom != null && zoom.isEnabled()) fov /= (float) zoom.update();
		GameImpl.lastFov = fov;
		cir.setReturnValue(fov);
	}
}
