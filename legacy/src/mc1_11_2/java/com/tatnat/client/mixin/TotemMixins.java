package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

/**
 * Totem Pop Counter: entity status 35 is the totem animation (totems arrived in 1.11). The network
 * handler plays it itself, so the count is taken there, after the packet is handled on the game thread.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class TotemMixins {
	@Shadow
	private ClientWorld world;

	@Inject(method = "onEntityStatus", at = @At("TAIL"))
	private void tatnat$pop(EntityStatusS2CPacket packet, CallbackInfo ci) {
		if (packet.getStatus() != 35 || world == null) return;
		Entity e = packet.getEntity(world);
		if (e != null) com.tatnat.client.modules.impl.hud.TotemPops.popped(e.getName().asUnformattedString(), e == MinecraftClient.getInstance().player);
	}
}
