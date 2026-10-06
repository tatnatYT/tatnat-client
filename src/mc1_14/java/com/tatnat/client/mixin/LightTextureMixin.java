package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tatnat.client.modules.impl.visual.FullBright;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.LightTexture;

/**
 * Full Bright: when the lightmap reads the gamma option, hand it the module's level instead.
 * Only this one read is changed -- the saved option (and the slider in Video Settings) is untouched.
 */
@Mixin(LightTexture.class)
public class LightTextureMixin {
	@WrapOperation(method = "updateLightTexture", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;gamma:D", opcode = org.objectweb.asm.Opcodes.GETFIELD))
	private double tatnat$fullBright(Options options, Operation<Double> original) {
		double value = original.call(options);
		return FullBright.active() ? FullBright.INSTANCE.level.get() : value;
	}
}
