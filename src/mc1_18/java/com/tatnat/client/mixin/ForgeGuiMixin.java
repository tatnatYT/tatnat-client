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
import com.mojang.blaze3d.vertex.PoseStack;
import com.tatnat.client.mc.Graphics;

/** Forge only: its own HUD renderer replaces Gui.render, so the Render2D hook goes here too. */
@Pseudo
@Mixin(targets = "net.minecraftforge.client.gui.ForgeIngameGui")
public class ForgeGuiMixin {
	@Inject(method = "render", at = @At("TAIL"), require = 0)
	private void tatnat$render2d(PoseStack pose, float partialTick, CallbackInfo ci) {
		// Chat and the hotbar leave depth behind here, which would hide parts of our HUD.
		com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, net.minecraft.client.Minecraft.ON_OSX);
		TatnatClient.EVENTS.post(new Events.Render2D(new GfxImpl(new Graphics(pose)), partialTick));
	}
}
