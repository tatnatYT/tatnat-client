package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Posts {@link Events.Attack} when you hit an entity (Combo Counter, Reach Display). */
@Mixin(MultiPlayerGameMode.class)
public class GameplayHooksMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void tatnat$attack(Player player, Entity target, CallbackInfo ci) {
		TatnatClient.EVENTS.post(new Events.Attack(target));
	}
}
