package com.tatnat.client.ui.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.ModuleManager;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Game;
import com.tatnat.client.platform.Gfx;

/** Draws every enabled HUD element during the normal HUD pass. */
public final class HudRenderer {
	public static final HudRenderer INSTANCE = new HudRenderer();

	private HudRenderer() {
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		Game game = TatnatClient.game();
		// F1 hides everything; the HUD editor draws the elements itself (on top of its backdrop).
		if (game.hudHidden() || HudEditorScreen.isOpen()) return;
		com.tatnat.client.ui.render.RectBatch g = com.tatnat.client.ui.render.RectBatch.of(e.gfx);
		try {
			renderAll(g, false);
		} finally {
			g.flush();
		}
	}

	public static void renderAll(Gfx g, boolean preview) {
		Game game = TatnatClient.game();
		int w = game.guiWidth(), h = game.guiHeight();
		for (HudModule m : ModuleManager.get().hud()) {
			if (!m.isEnabled()) continue;
			if (!preview && !m.hasContent()) continue;
			m.render(g, w, h, preview);
		}
	}
}
