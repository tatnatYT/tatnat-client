package com.tatnat.client.mc;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;

/** Draws the mod HUD after the game's own overlay (vanilla GuiIngame and Forge's GuiIngameForge). */
public final class HudRender {
	private HudRender() {
	}

	public static void render(float partialTick) {
		// Chat and the hotbar leave depth behind, which would hide parts of our HUD.
		GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
		TatnatClient.EVENTS.post(new Events.Render2D(new GfxImpl(), partialTick));
		GlStateManager.color(1f, 1f, 1f, 1f);
	}
}
