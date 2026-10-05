package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tatnat.client.modules.impl.cosmetic.CustomCapes;
import com.tatnat.client.modules.impl.cosmetic.EnchantGlint;
import com.tatnat.client.modules.impl.utility.Freecam;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/** Freecam input blocking, cape physics and glint speed. */
public final class MiscMixins {
	private MiscMixins() {
	}

	@Mixin(ClientInput.class)
	public interface InputAccess {
		@Accessor("moveVector")
		void tatnat$setMoveVector(Vec2 v);
	}

	/** While Freecam is on, your player gets no movement input at all. */
	@Mixin(KeyboardInput.class)
	public static class FreezePlayer {
		@Inject(method = "tick", at = @At("TAIL"))
		private void tatnat$freeze(CallbackInfo ci) {
			if (!Freecam.active()) return;
			ClientInput self = (ClientInput) (Object) this;
			self.keyPresses = Input.EMPTY;
			((InputAccess) self).tatnat$setMoveVector(Vec2.ZERO);
		}
	}

	/** Cape swing strength (Custom Capes "Physics"). Applies to your own cape only. */
	@Mixin(AvatarRenderer.class)
	public static class CapePhysics {
		@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
				at = @At("TAIL"))
		private void tatnat$physics(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
			if (!CustomCapes.active() || entity != Minecraft.getInstance().player) return;
			float k = CustomCapes.INSTANCE.physics.floatValue() / 100f;
			state.capeFlap *= k;
			state.capeLean *= k;
			state.capeLean2 *= k;
		}
	}

	/** Enchant Glint speed multiplier on top of vanilla's Glint Speed option. */
	@Mixin(TextureTransform.class)
	public static class GlintSpeed {
		@WrapOperation(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
		private static Object tatnat$speed(OptionInstance<?> option, Operation<Object> original) {
			Object v = original.call(option);
			if (EnchantGlint.active() && v instanceof Double d) return d * EnchantGlint.INSTANCE.speed.get();
			return v;
		}
	}
}
