package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Motion Blur / Color Saturation: the game's post-effect loader is private. */
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public interface PostEffectAccess {
	@Invoker("setPostEffect")
	void tatnat$loadEffect(net.minecraft.resources.Identifier id);
}
