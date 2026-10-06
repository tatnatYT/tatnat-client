package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;

import net.minecraft.client.Minecraft;

/** Posts {@link Events.Tick} once per client tick. */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void tatnat$tick(CallbackInfo ci) {
		TatnatClient.EVENTS.post(Events.Tick.INSTANCE);
	}
}
