package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.mc.PlayerLooks;
import com.tatnat.client.modules.impl.visual.FovModifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Your own player's skin, cape and model as seen on your screen (Nick Hider, Custom Capes), and
 * the sprint/fly FOV change (FOV Modifier's Static FOV). 1.20 - 1.20.1 use separate getters.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerLookMixins {
	private boolean tatnat$isMe() {
		return (Object) this == Minecraft.getInstance().player;
	}

	@Inject(method = "getSkinTextureLocation", at = @At("RETURN"), cancellable = true)
	private void tatnat$skin(CallbackInfoReturnable<ResourceLocation> cir) {
		if (tatnat$isMe()) cir.setReturnValue(PlayerLooks.skin(cir.getReturnValue()));
	}

	@Inject(method = "getModelName", at = @At("RETURN"), cancellable = true)
	private void tatnat$model(CallbackInfoReturnable<String> cir) {
		if (tatnat$isMe()) cir.setReturnValue(PlayerLooks.model(cir.getReturnValue()));
	}

	@Inject(method = {"getCloakTextureLocation", "getElytraTextureLocation"}, at = @At("RETURN"), cancellable = true)
	private void tatnat$cape(CallbackInfoReturnable<ResourceLocation> cir) {
		if (tatnat$isMe()) cir.setReturnValue(PlayerLooks.cape(cir.getReturnValue()));
	}

	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void tatnat$staticFov(CallbackInfoReturnable<Float> cir) {
		if (FovModifier.active() && FovModifier.INSTANCE.staticFov.on()) cir.setReturnValue(1.0f);
	}
}
