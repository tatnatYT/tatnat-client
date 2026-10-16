package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.mc.PlayerLooks;
import com.tatnat.client.modules.impl.visual.FovModifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;

/**
 * Your own player's skin and cape as seen on your screen (Nick Hider, Eclipse Plus capes), and the
 * sprint/fly FOV change (FOV Modifier's Static FOV).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerLookMixins {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void tatnat$skin(CallbackInfoReturnable<PlayerSkin> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		cir.setReturnValue(PlayerLooks.apply(cir.getReturnValue(), self.getUUID(), self == Minecraft.getInstance().player));
	}

	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void tatnat$staticFov(CallbackInfoReturnable<Float> cir) {
		if (FovModifier.active() && FovModifier.INSTANCE.staticFov.on()) cir.setReturnValue(1.0f);
	}
}
