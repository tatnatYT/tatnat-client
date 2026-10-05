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
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.Level;

/** World appearance hooks: water fog, hit flash colour, time / weather, chunk slide-in. */
public final class WorldLookMixins {
	private WorldLookMixins() {
	}

	@Mixin(FogRenderer.class)
	public static class WaterFog {
		@Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
		private static void tatnat$clearWater(Camera camera, FogRenderer.FogMode mode, Vector4f color, float renderDistance, boolean thick,
				float partialTick, CallbackInfoReturnable<FogParameters> cir) {
			if (!ClearWater.active() || camera.getFluidInCamera() != FogType.WATER) return;
			FogParameters f = cir.getReturnValue();
			float s = ClearWater.INSTANCE.strength.floatValue() / 100f;
			float end = f.end() + (Math.max(f.end(), renderDistance) - f.end()) * s;
			float start = f.start() + (end * 0.8f - f.start()) * s;
			cir.setReturnValue(new FogParameters(start, end, f.shape(), f.red(), f.green(), f.blue(), f.alpha()));
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
			for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) texture.getPixels().setPixel(x, y, argb);
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
