package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.mc.PlayerLooks;
import com.tatnat.client.modules.impl.visual.FovModifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Your own player's skin and cape as seen on your screen (Nick Hider, Custom Capes), and the
 * sprint/fly FOV change (FOV Modifier's Static FOV).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerLookMixins {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void tatnat$skin(CallbackInfoReturnable<PlayerSkin> cir) {
		if ((Object) this != Minecraft.getInstance().player) return;
		cir.setReturnValue(PlayerLooks.apply(cir.getReturnValue()));
	}

	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void tatnat$staticFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
		if (FovModifier.active() && FovModifier.INSTANCE.staticFov.on()) cir.setReturnValue(1.0f);
	}
}
