package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.mc.GameImpl;
import com.tatnat.client.modules.ClientOptions;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

/** A "Eclipse Client" button in the title screen's top-left corner (Settings > Title Screen Button). */
@Mixin(TitleScreen.class)
public abstract class TitleButtonMixin extends Screen {
	private static final int TATNAT_BUTTON = 7301;

	@Inject(method = "init", at = @At("TAIL"))
	private void tatnat$button(CallbackInfo ci) {
		if (ClientOptions.titleButton()) buttons.add(new ButtonWidget(TATNAT_BUTTON, 4, 4, 98, 20, "Eclipse Client"));
	}

	@Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
	private void tatnat$click(ButtonWidget button, CallbackInfo ci) {
		if (button.id != TATNAT_BUTTON) return;
		GameImpl.openModMenu();
		ci.cancel();
	}
}
