package com.tatnat.client.mc;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.tatnat.client.platform.UiScreen;

import net.minecraft.client.gui.screen.Screen;

/** Hosts a shared {@link UiScreen} as a legacy Screen; input is passed on in real window pixels. */
public final class ScreenBridge extends Screen {
	final UiScreen ui;

	public ScreenBridge(UiScreen ui) {
		this.ui = ui;
	}

	private static double mx() {
		return GameImpl.INSTANCE.mouseX();
	}

	private static double my() {
		return GameImpl.INSTANCE.mouseY();
	}

	@Override
	public void init() {
		Keyboard.enableRepeatEvents(true);
	}

	@Override
	public boolean shouldPauseGame() {
		return ui.pausesGame();
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		if (client.world == null) {
			renderDirtBackground(0);
		} else if (ui.backdrop() == UiScreen.Backdrop.DIM) {
			fill(0, 0, width, height, 0x3C000000);
		}
		GfxImpl gfx = new GfxImpl();
		ui.render(gfx, mx(), my());
		ui.renderOverlay(gfx, mx(), my());
	}

	@Override
	protected void mouseClicked(int x, int y, int button) {
		ui.mouseClicked(mx(), my(), button);
	}

	@Override
	protected void mouseReleased(int x, int y, int button) {
		ui.mouseReleased(mx(), my(), button);
	}

	@Override
	protected void mouseDragged(int x, int y, int button, long held) {
		ui.mouseDragged(mx(), my(), button);
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) ui.mouseScrolled(mx(), my(), Math.signum(wheel));
	}

	@Override
	protected void keyPressed(char c, int code) {
		int key = LwjglKeys.toGlfw(code);
		if (key >= 0 && ui.keyPressed(key, hasShiftDown())) return;
		if (c >= 32 && c != 127 && c != 167) {
			ui.charTyped(String.valueOf(c));
			return;
		}
		if (code == Keyboard.KEY_ESCAPE && ui.closesOnEscape()) client.setScreen(null);
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
		ui.removed();
	}
}
