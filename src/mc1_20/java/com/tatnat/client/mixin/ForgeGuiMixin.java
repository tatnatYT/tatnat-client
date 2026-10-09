package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.mc.GfxImpl;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/** Forge only: its own HUD renderer replaces Gui.render, so the Render2D hook goes here too. */
@Pseudo
@Mixin(targets = "net.minecraftforge.client.gui.overlay.ForgeGui")
public class ForgeGuiMixin {
	@Inject(method = "render", at = @At("TAIL"), require = 0)
	private void tatnat$render2d(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
		TatnatClient.EVENTS.post(new Events.Render2D(new GfxImpl(graphics), partialTick));
	}
}
