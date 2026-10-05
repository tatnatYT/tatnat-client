package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/** Nick Hider in the tab list: your entry shows the chosen name. */
@Mixin(PlayerTabOverlay.class)
public class NameMixins {
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void tatnat$tabName(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		if (!NickHider.active() || !NickHider.INSTANCE.inTab.on()) return;
		if (NickHider.INSTANCE.isMe(info.getProfile().id())) cir.setReturnValue(NickHider.INSTANCE.replace(cir.getReturnValue()));
	}
}
