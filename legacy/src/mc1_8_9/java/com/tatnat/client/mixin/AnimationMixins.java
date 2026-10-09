package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.modules.impl.visual.OldAnimations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;

/**
 * 1.7 Animations: 1.8 passes a swing of 0 to applyEquipAndSwingOffset while eating, drinking or
 * blocking; 1.7 passed the real swing progress, so the item kept swinging (block-hitting).
 * Call order in renderArmHoldingItem: 0 = nothing, 1 = eat, 2 = drink, 3 = block, 4 = bow.
 */
@Mixin(HeldItemRenderer.class)
public class AnimationMixins {
	private static float tatnat$swing;

	@Inject(method = "renderArmHoldingItem", at = @At("HEAD"))
	private void tatnat$captureSwing(float tickDelta, CallbackInfo ci) {
		MinecraftClient mc = MinecraftClient.getInstance();
		tatnat$swing = mc.player == null ? 0f : mc.player.getHandSwingProgress(tickDelta);
	}

	@ModifyArg(method = "renderArmHoldingItem", index = 1,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V", ordinal = 1))
	private float tatnat$eatSwing(float swing) {
		return OldAnimations.eating() ? tatnat$swing : swing;
	}

	@ModifyArg(method = "renderArmHoldingItem", index = 1,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V", ordinal = 2))
	private float tatnat$drinkSwing(float swing) {
		return OldAnimations.eating() ? tatnat$swing : swing;
	}

	@ModifyArg(method = "renderArmHoldingItem", index = 1,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V", ordinal = 3))
	private float tatnat$blockSwing(float swing) {
		return OldAnimations.blockHit() ? tatnat$swing : swing;
	}
}
