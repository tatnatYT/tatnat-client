package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Eclipse main menu: when Minecraft opens its title screen (at start-up, after leaving a world,
 * or when a menu closes with no world), the Eclipse one opens instead (Settings > Eclipse Main Menu).
 */
@Mixin(net.minecraft.client.Minecraft.class)
public class MainMenuMixin {
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
	private void tatnat$mainMenu(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
		if (!com.tatnat.client.ui.menu.MainMenu.replaces()) return;
		if (screen instanceof net.minecraft.client.gui.screens.TitleScreen || screen == null && net.minecraft.client.Minecraft.getInstance().level == null) {
			ci.cancel();
			com.tatnat.client.mc.GameImpl.INSTANCE.openScreen(new com.tatnat.client.ui.menu.MainMenu());
		}
	}
}
