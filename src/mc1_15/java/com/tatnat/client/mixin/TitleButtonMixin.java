package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.mc.GameImpl;
import com.tatnat.client.modules.ClientOptions;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/** A "tatnat client" button in the title screen's top-left corner (Settings > Title Screen Button). */
@Mixin(TitleScreen.class)
public abstract class TitleButtonMixin extends Screen {
	protected TitleButtonMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void tatnat$button(CallbackInfo ci) {
		if (!ClientOptions.titleButton()) return;
		addButton(new Button(4, 4, 98, 20, "tatnat client", b -> GameImpl.openModMenu()));
	}
}
