package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.modules.impl.cosmetic.CustomCapes;
import com.tatnat.client.modules.impl.visual.FovModifier;
import com.tatnat.client.modules.impl.visual.NickHider;

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
		PlayerSkin skin = cir.getReturnValue();
		if (NickHider.active() && NickHider.INSTANCE.hideSkin.on()) skin = NickHider.INSTANCE.replacementSkin();
		if (CustomCapes.active()) skin = CustomCapes.INSTANCE.apply(skin);
		cir.setReturnValue(skin);
	}

	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void tatnat$staticFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
		if (FovModifier.active() && FovModifier.INSTANCE.staticFov.on()) cir.setReturnValue(1.0f);
	}
}
