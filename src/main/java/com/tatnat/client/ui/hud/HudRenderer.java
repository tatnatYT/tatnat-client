package com.tatnat.client.ui.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.ModuleManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Draws every enabled HUD element during the normal HUD pass. */
public final class HudRenderer {
	public static final HudRenderer INSTANCE = new HudRenderer();

	private HudRenderer() {
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		Minecraft mc = Minecraft.getInstance();
		// F1 hides everything; the HUD editor draws the elements itself (on top of its blur).
		if (mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
		renderAll(e.graphics, false);
	}

	public static void renderAll(GuiGraphics g, boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		for (HudModule m : ModuleManager.get().hud()) {
			if (!m.isEnabled()) continue;
			if (!preview && !m.hasContent()) continue;
			m.render(g, w, h, preview);
		}
	}
}
