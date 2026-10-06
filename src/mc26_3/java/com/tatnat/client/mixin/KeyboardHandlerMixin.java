package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/** Posts {@link Events.Key} for every key press before the game sees it. */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void tatnat$key(long window, int action, KeyEvent event, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (window != mc.getWindow().handle()) return;
		Events.Key e = TatnatClient.EVENTS.post(new Events.Key(com.tatnat.client.mc.SdlKeys.toGlfw(event.key()), com.tatnat.client.mc.SdlKeys.actionToGlfw(action), mc.gui.screen() == null));
		if (e.isCancelled()) ci.cancel();
	}
}
