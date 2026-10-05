package com.tatnat.client.mixin;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
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
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.Level;

/** World appearance hooks: water fog, hit flash colour, time / weather, chunk slide-in. */
public final class WorldLookMixins {
	private WorldLookMixins() {
	}

	@Mixin(FogRenderer.class)
	public static class WaterFog {
		@Inject(method = "setupFog", at = @At("TAIL"))
		private static void tatnat$clearWater(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thick, float partialTick,
				CallbackInfo ci) {
			if (!ClearWater.active() || camera.getFluidInCamera() != FogType.WATER) return;
			float s = ClearWater.INSTANCE.strength.floatValue() / 100f;
			float end0 = RenderSystem.getShaderFogEnd(), start0 = RenderSystem.getShaderFogStart();
			float end = end0 + (Math.max(end0, renderDistance) - end0) * s;
			RenderSystem.setShaderFogEnd(end);
			RenderSystem.setShaderFogStart(start0 + (end * 0.8f - start0) * s);
		}
	}

	@Mixin(OverlayTexture.class)
	public static class Overlay implements FeaturesImpl.HurtOverlay {
		@Shadow
		@Final
		private DynamicTexture texture;

		@Inject(method = "<init>", at = @At("RETURN"))
		private void tatnat$capture(CallbackInfo ci) {
			FeaturesImpl.overlay = this;
		}

		@Override
		public void tatnat$setHurtColor(int argb) {
			// Rows 0-7 are the "hurt" half of the 16x16 overlay texture.
			for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) texture.getPixels().setPixelRGBA(x, y, FeaturesImpl.toAbgr(argb));
			texture.upload();
		}
	}

	@Mixin(ClientLevel.ClientLevelData.class)
	public static class DayTime {
		@Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
		private void tatnat$time(CallbackInfoReturnable<Long> cir) {
			if (TimeChanger.active()) cir.setReturnValue(TimeChanger.INSTANCE.time.get().longValue());
		}
	}

	@Mixin(Level.class)
	public static class Weather {
		@Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
		private void tatnat$rain(float partialTick, CallbackInfoReturnable<Float> cir) {
			if (!((Object) this instanceof ClientLevel) || !TimeChanger.active() || TimeChanger.INSTANCE.weather.is("Server")) return;
			cir.setReturnValue(TimeChanger.INSTANCE.weather.is("Clear") ? 0f : 1f);
		}

		@Inject(method = "getThunderLevel", at = @At("RETURN"), cancellable = true)
		private void tatnat$thunder(float partialTick, CallbackInfoReturnable<Float> cir) {
			if (!((Object) this instanceof ClientLevel) || !TimeChanger.active() || TimeChanger.INSTANCE.weather.is("Server")) return;
			cir.setReturnValue(TimeChanger.INSTANCE.weather.is("Thunder") ? 1f : 0f);
		}
	}
}
