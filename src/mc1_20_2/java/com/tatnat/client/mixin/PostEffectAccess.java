package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Motion Blur / Color Saturation: GameRenderer.loadEffect is private. */
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public interface PostEffectAccess {
	@Invoker("loadEffect")
	void tatnat$loadEffect(net.minecraft.resources.ResourceLocation id);
}
