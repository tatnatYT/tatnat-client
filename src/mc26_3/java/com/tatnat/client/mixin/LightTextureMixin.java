package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tatnat.client.modules.impl.visual.FullBright;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;

/**
 * Full Bright: when the lightmap reads the gamma option, hand it the module's level instead.
 * Only this one read is changed -- the saved option (and the slider in Video Settings) is untouched.
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightTextureMixin {
	@WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
	private Object tatnat$fullBright(OptionInstance<?> option, Operation<Object> original) {
		Object value = original.call(option);
		if (FullBright.active() && option == Minecraft.getInstance().options.gamma()) {
			return FullBright.INSTANCE.level.get();
		}
		return value;
	}
}
