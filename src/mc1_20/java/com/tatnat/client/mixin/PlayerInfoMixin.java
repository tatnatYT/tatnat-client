package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;
import com.tatnat.client.mc.PlayerLooks;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

/** Nick Hider: your head in the tab list uses the default skin too. */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
	@Shadow
	public abstract GameProfile getProfile();

	@Inject(method = "getSkinLocation", at = @At("RETURN"), cancellable = true)
	private void tatnat$skin(CallbackInfoReturnable<ResourceLocation> cir) {
		if (PlayerLooks.isMe(getProfile().getId())) cir.setReturnValue(PlayerLooks.skin(cir.getReturnValue()));
	}

	@Inject(method = "getModelName", at = @At("RETURN"), cancellable = true)
	private void tatnat$model(CallbackInfoReturnable<String> cir) {
		if (PlayerLooks.isMe(getProfile().getId())) cir.setReturnValue(PlayerLooks.model(cir.getReturnValue()));
	}
}
