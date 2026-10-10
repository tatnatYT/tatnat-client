package com.tatnat.client.mc;

import com.tatnat.client.platform.UiScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Hosts a shared {@link UiScreen} as a 1.21.11 Screen, converting input to real pixels. */
public final class ScreenBridge extends Screen {
	final UiScreen ui;

	public ScreenBridge(UiScreen ui) {
		super(Component.literal("Eclipse Client"));
		this.ui = ui;
	}

	private double px(double gui) {
		return gui * minecraft.getWindow().getGuiScale();
	}

	@Override
	public boolean isPauseScreen() {
		return ui.pausesGame();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (minecraft.level == null) {
			// Title screen: the usual panorama behind the menu.
			renderPanorama(g, partialTick);
			renderBlurredBackground(g);
		} else if (ui.backdrop() == UiScreen.Backdrop.DIM) {
			g.fill(0, 0, width, height, 0x3C000000);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		GfxImpl gfx = new GfxImpl(g);
		double mx = GameImpl.INSTANCE.mouseX(), my = GameImpl.INSTANCE.mouseY();
		ui.render(gfx, mx, my);
		ui.renderOverlay(gfx, mx, my);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		return ui.mouseClicked(px(x), px(y), button);
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		ui.mouseReleased(px(x), px(y), button);
		return true;
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
		ui.mouseDragged(px(x), px(y), button);
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		ui.mouseScrolled(px(x), px(y), scrollY);
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		if (ui.keyPressed(key, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0)) return true;
		return super.keyPressed(key, scancode, modifiers);
	}

	@Override
	public boolean charTyped(char c, int modifiers) {
		return ui.charTyped(String.valueOf(c));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return ui.closesOnEscape();
	}

	@Override
	public void removed() {
		ui.removed();
	}
}
