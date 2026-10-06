package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tatnat.client.modules.impl.cosmetic.EnchantGlint;
import com.tatnat.client.modules.impl.utility.Freecam;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.player.Input;

/** Freecam input blocking, cape physics and glint speed. */
public final class MiscMixins {
	private MiscMixins() {
	}

	/** While Freecam is on, your player gets no movement input at all. */
	@Mixin(KeyboardInput.class)
	public static class FreezePlayer {
		@Inject(method = "tick", at = @At("TAIL"))
		private void tatnat$freeze(boolean slow, float slowFactor, CallbackInfo ci) {
			if (!Freecam.active()) return;
			Input self = (Input) (Object) this;
			self.up = self.down = self.left = self.right = self.jumping = self.shiftKeyDown = false;
			self.leftImpulse = self.forwardImpulse = 0f;
		}
	}

	/** Enchant Glint speed multiplier on top of vanilla's Glint Speed option. */
	@Mixin(RenderStateShard.class)
	public static class GlintSpeed {
		@WrapOperation(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
		private static Object tatnat$speed(OptionInstance<?> option, Operation<Object> original) {
			Object v = original.call(option);
			if (EnchantGlint.active() && v instanceof Double d) return d * EnchantGlint.INSTANCE.speed.get();
			return v;
		}
	}
}
