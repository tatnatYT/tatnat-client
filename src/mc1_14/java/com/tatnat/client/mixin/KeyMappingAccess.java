package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.KeyMapping;

/** 1.14 has no KeyMapping.setDown; Toggle Sprint holds the key through this. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccess {
	@Accessor("isDown")
	void tatnat$setDown(boolean down);
}
