package com.tatnat.client.mc;

import com.tatnat.client.platform.UiScreen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Hosts a shared {@link UiScreen} as a 1.21.11 Screen, converting input to real pixels. */
public final class ScreenBridge extends Screen {
	final UiScreen ui;

	public ScreenBridge(UiScreen ui) {
		super(Component.literal("tatnat client"));
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
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		if (minecraft.level == null) {
			// Title screen: the usual panorama behind the menu.
			extractPanorama(g, partialTick);
			extractBlurredBackground(g);
		} else if (ui.backdrop() == UiScreen.Backdrop.DIM) {
			g.fill(0, 0, width, height, 0x3C000000);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		GfxImpl gfx = new GfxImpl(g);
		double mx = GameImpl.INSTANCE.mouseX(), my = GameImpl.INSTANCE.mouseY();
		ui.render(gfx, mx, my);
		ui.renderOverlay(gfx, mx, my);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		return ui.mouseClicked(px(e.x()), px(e.y()), e.button());
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		ui.mouseReleased(px(e.x()), px(e.y()), e.button());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		ui.mouseDragged(px(e.x()), px(e.y()), e.button());
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		ui.mouseScrolled(px(x), px(y), scrollY);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (ui.keyPressed(e.key(), (e.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0)) return true;
		return super.keyPressed(e);
	}

	@Override
	public boolean charTyped(CharacterEvent e) {
		return ui.charTyped(e.codepointAsString());
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
