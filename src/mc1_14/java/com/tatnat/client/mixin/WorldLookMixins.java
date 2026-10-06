package com.tatnat.client.mixin;

import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.modules.impl.visual.ClearWater;
import com.tatnat.client.mc.FeaturesImpl;
import com.tatnat.client.modules.impl.visual.TimeChanger;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.MultiPlayerLevel;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.tags.FluidTags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.level.Level;

/** World appearance hooks: water fog, hit flash colour, time / weather, chunk slide-in. */
public final class WorldLookMixins {
	private WorldLookMixins() {
	}

	@Mixin(FogRenderer.class)
	public static class WaterFog {
		/** Water fog is exponential on this version: thin it out by the module strength. */
		@WrapOperation(method = "setupFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;fogDensity(F)V"))
		private static void tatnat$clearWater(float density, Operation<Void> original, @Local(argsOnly = true) Camera camera) {
			if (ClearWater.active() && camera.getFluidInCamera().is(FluidTags.WATER)) {
				density *= 1f - 0.95f * ClearWater.INSTANCE.strength.floatValue() / 100f;
			}
			original.call(density);
		}
	}

	/** Hit Color: 1.14 tints hurt mobs with the first four values put in the overlay buffer (r, g, b, strength). */
	@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
	public static class Overlay {
		private static final String M = "setupOverlayColor(Lnet/minecraft/world/entity/LivingEntity;FZ)Z";
		private static final String PUT = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;";

		@org.spongepowered.asm.mixin.injection.ModifyArg(method = M, at = @At(value = "INVOKE", target = PUT, ordinal = 0))
		private float tatnat$r(float v) {
			int c = FeaturesImpl.hurtColor;
			return c == 0 ? v : ((c >> 16) & 255) / 255f;
		}

		@org.spongepowered.asm.mixin.injection.ModifyArg(method = M, at = @At(value = "INVOKE", target = PUT, ordinal = 1))
		private float tatnat$g(float v) {
			int c = FeaturesImpl.hurtColor;
			return c == 0 ? v : ((c >> 8) & 255) / 255f;
		}

		@org.spongepowered.asm.mixin.injection.ModifyArg(method = M, at = @At(value = "INVOKE", target = PUT, ordinal = 2))
		private float tatnat$b(float v) {
			int c = FeaturesImpl.hurtColor;
			return c == 0 ? v : (c & 255) / 255f;
		}

		/** Hit Color stores opacity inverted (for the newer shader); here it is the tint strength. */
		@org.spongepowered.asm.mixin.injection.ModifyArg(method = M, at = @At(value = "INVOKE", target = PUT, ordinal = 3))
		private float tatnat$strength(float v) {
			int c = FeaturesImpl.hurtColor;
			return c == 0 ? v : 1f - ((c >>> 24) & 255) / 255f;
		}
	}

	/** The sky reads the time from LevelData, which the integrated server shares: only the render thread sees the change. */
	@Mixin(net.minecraft.world.level.storage.LevelData.class)
	public static class DayTime {
		@Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
		private void tatnat$time(CallbackInfoReturnable<Long> cir) {
			if (TimeChanger.active() && net.minecraft.client.Minecraft.getInstance().isSameThread()) cir.setReturnValue(TimeChanger.INSTANCE.time.get().longValue());
		}
	}

	@Mixin(Level.class)
	public static class Weather {
		@Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
		private void tatnat$rain(float partialTick, CallbackInfoReturnable<Float> cir) {
			if (!((Object) this instanceof MultiPlayerLevel) || !TimeChanger.active() || TimeChanger.INSTANCE.weather.is("Server")) return;
			cir.setReturnValue(TimeChanger.INSTANCE.weather.is("Clear") ? 0f : 1f);
		}

		@Inject(method = "getThunderLevel", at = @At("RETURN"), cancellable = true)
		private void tatnat$thunder(float partialTick, CallbackInfoReturnable<Float> cir) {
			if (!((Object) this instanceof MultiPlayerLevel) || !TimeChanger.active() || TimeChanger.INSTANCE.weather.is("Server")) return;
			cir.setReturnValue(TimeChanger.INSTANCE.weather.is("Thunder") ? 1f : 0f);
		}
	}
}
