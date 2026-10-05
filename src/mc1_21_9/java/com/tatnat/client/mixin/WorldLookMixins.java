package com.tatnat.client.mixin;

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
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.Level;

/** World appearance hooks: water fog, hit flash colour, time / weather, chunk slide-in. */
public final class WorldLookMixins {
	private WorldLookMixins() {
	}

	@Mixin(WaterFogEnvironment.class)
	public static class WaterFog {
		@Inject(method = "setupFog", at = @At("TAIL"))
		private void tatnat$clearWater(FogData fog, net.minecraft.world.entity.Entity camera, net.minecraft.core.BlockPos pos, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
			if (!ClearWater.active()) return;
			float s = ClearWater.INSTANCE.strength.floatValue() / 100f;
			float far = Math.max(fog.environmentalEnd, fog.renderDistanceEnd);
			fog.environmentalEnd += (far - fog.environmentalEnd) * s;
			fog.environmentalStart += (fog.environmentalEnd * 0.8f - fog.environmentalStart) * s;
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
