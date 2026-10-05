package com.tatnat.client.ui.hud;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.ModuleManager;
import com.tatnat.client.ui.clickgui.ClickGuiScreen;
import com.tatnat.client.ui.clickgui.Widgets;
import com.tatnat.client.ui.render.Animation;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Drag-and-drop HUD layout editor.
 *
 * Only the enabled HUD elements are shown, each in a 1px dashed accent box. Dragging snaps the
 * element's left/centre/right (and top/middle/bottom) to the screen edges, the screen centre and
 * other elements, drawing a yellow guide line for every active snap. Scroll over an element to
 * resize it, right-click it for its settings, press R over it to reset it. Hold Shift while
 * dragging to turn snapping off.
 */
public class HudEditorScreen extends Screen {
	/** Snap distance in real pixels, converted to GUI units when used. */
	private static final double SNAP_PX = 8;
	private static final int EDGE_MARGIN = 2;

	private final Screen parent;
	private final Animation open = new Animation(200, 0f);

	private HudModule dragging;
	private double grabX, grabY;
	/** Active guides from the last drag step, in GUI coordinates. NaN = none. */
	private final List<Float> guidesX = new ArrayList<>(), guidesY = new ArrayList<>();

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
		open.animateTo(1f);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, Colors.argb(Math.round(60 * open.get()), 0, 0, 0));
	}

	private List<HudModule> elements() {
		List<HudModule> out = new ArrayList<>();
		for (HudModule m : ModuleManager.get().hud()) if (m.isEnabled()) out.add(m);
		return out;
	}

	private HudModule elementAt(double gx, double gy) {
		List<HudModule> list = elements();
		// Topmost (last drawn) first.
		for (int i = list.size() - 1; i >= 0; i--) {
			HudModule m = list.get(i);
			float x = m.screenX(width), y = m.screenY(height);
			if (gx >= x - 2 && gy >= y - 2 && gx <= x + m.scaledWidth() + 2 && gy <= y + m.scaledHeight() + 2) return m;
		}
		return null;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		float o = open.get();
		// The elements themselves, in GUI space, on top of the blur.
		HudRenderer.renderAll(g, true);

		RenderUtils.alpha = o;
		int scale = RenderUtils.beginPixels(g);
		double mx = Widgets.mouseX(), my = Widgets.mouseY();
		HudModule hovered = dragging != null ? dragging : elementAt(mx / scale, my / scale);

		for (HudModule m : elements()) {
			int x = Math.round(m.screenX(width) * scale) - 3, y = Math.round(m.screenY(height) * scale) - 3;
			int w = Math.round(m.scaledWidth() * scale) + 6, h = Math.round(m.scaledHeight() * scale) + 6;
			boolean active = m == hovered;
			if (active) RenderUtils.rect(g, x, y, x + w, y + h, Colors.withAlpha(Theme.ACCENT, 0x22));
			RenderUtils.dashedRect(g, x, y, w, h, 5, 4, 1, active ? Theme.ACCENT : Colors.withAlpha(Theme.ACCENT, 0xB0));
			if (active) {
				String label = m.name + "  " + Math.round(m.scale.get() * 100) + "%";
				int lw = UIFont.SMALL.width(label) + 14;
				int ly = y - 26 >= 0 ? y - 26 : y + h + 4;
				RenderUtils.roundedRect(g, x, ly, lw, 22, Theme.RADIUS_SMALL, Theme.BACKGROUND);
				UIFont.SMALL.draw(g, label, x + 7, ly + 4, Theme.TEXT);
			}
		}

		// Yellow snap guides across the whole screen.
		int W = minecraft.getWindow().getWidth(), H = minecraft.getWindow().getHeight();
		if (dragging != null) {
			for (float gx : guidesX) {
				int px = Math.round(gx * scale);
				RenderUtils.rect(g, px, 0, px + 1, H, Theme.GUIDE);
			}
			for (float gy : guidesY) {
				int py = Math.round(gy * scale);
				RenderUtils.rect(g, 0, py, W, py + 1, Theme.GUIDE);
			}
		}

		drawToolbar(g, W, H, mx, my);
		RenderUtils.end(g);
		RenderUtils.alpha = 1f;
	}

	private int doneX, doneY, doneW, doneH;

	private void drawToolbar(GuiGraphics g, int W, int H, double mx, double my) {
		String hint = "Drag to move  ·  Scroll to resize  ·  Right-click for settings  ·  R to reset  ·  Shift = no snap";
		int hw = UIFont.SMALL.width(hint);
		doneW = 110;
		doneH = 34;
		int tw = hw + doneW + 48, th = 50;
		int tx = (W - tw) / 2, ty = H - th - 24 + Math.round((1 - open.get()) * 10);
		RenderUtils.shadow(g, tx, ty, tw, th, Theme.RADIUS_LARGE, 8, 0.5f);
		RenderUtils.roundedRect(g, tx, ty, tw, th, Theme.RADIUS_LARGE, Theme.BACKGROUND);
		UIFont.SMALL.draw(g, hint, tx + 18, ty + (th - UIFont.SMALL.size()) / 2, Theme.TEXT_MUTED);
		doneX = tx + tw - doneW - 8;
		doneY = ty + (th - doneH) / 2;
		Widgets.button(g, doneX, doneY, doneW, doneH, "Done", UIFont.TITLE, Theme.ACCENT, 0xFF0E1A26, Widgets.inside(mx, my, doneX, doneY, doneW, doneH));
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int scale = RenderUtils.guiScale();
		if (Widgets.inside(event.x() * scale, event.y() * scale, doneX, doneY, doneW, doneH)) {
			onClose();
			return true;
		}
		HudModule m = elementAt(event.x(), event.y());
		if (m == null) return true;
		if (event.button() == 1) {
			ClickGuiScreen gui = new ClickGuiScreen();
			gui.openSettingsFor(m);
			minecraft.setScreen(gui);
			return true;
		}
		if (event.button() == 0) {
			dragging = m;
			grabX = event.x() - m.screenX(width);
			grabY = event.y() - m.screenY(height);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return true;
		float x = (float) (event.x() - grabX), y = (float) (event.y() - grabY);
		guidesX.clear();
		guidesY.clear();
		boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
		if (!shift) {
			float[] snapped = snap(dragging, x, y);
			x = snapped[0];
			y = snapped[1];
		}
		dragging.setScreenPos(x, y, width, height);
		TatnatClient.CONFIG.markDirty();
		return true;
	}

	/**
	 * Magnetic snapping. Tries the element's left/centre/right against every vertical target line
	 * (screen edges, screen centre, other elements' edges and centres) and keeps the closest one
	 * within range; same for the vertical axis. Returns the adjusted top-left.
	 */
	float[] snap(HudModule m, float x, float y) {
		float w = m.scaledWidth(), h = m.scaledHeight();
		float range = (float) (SNAP_PX / RenderUtils.guiScale());

		List<Float> tx = new ArrayList<>(), ty = new ArrayList<>();
		tx.add((float) EDGE_MARGIN);
		tx.add(width / 2f);
		tx.add((float) width - EDGE_MARGIN);
		ty.add((float) EDGE_MARGIN);
		ty.add(height / 2f);
		ty.add((float) height - EDGE_MARGIN);
		for (HudModule o : elements()) {
			if (o == m) continue;
			float ox = o.screenX(width), oy = o.screenY(height);
			tx.add(ox);
			tx.add(ox + o.scaledWidth() / 2f);
			tx.add(ox + o.scaledWidth());
			ty.add(oy);
			ty.add(oy + o.scaledHeight() / 2f);
			ty.add(oy + o.scaledHeight());
		}

		float[] anchorsX = {0, w / 2f, w};
		float[] anchorsY = {0, h / 2f, h};
		float bestDx = Float.MAX_VALUE, bestDy = Float.MAX_VALUE;
		for (float t : tx) for (float a : anchorsX) {
			float d = t - (x + a);
			if (Math.abs(d) <= range && Math.abs(d) < Math.abs(bestDx)) bestDx = d;
		}
		for (float t : ty) for (float a : anchorsY) {
			float d = t - (y + a);
			if (Math.abs(d) <= range && Math.abs(d) < Math.abs(bestDy)) bestDy = d;
		}
		if (bestDx != Float.MAX_VALUE) x += bestDx;
		if (bestDy != Float.MAX_VALUE) y += bestDy;

		// Every target line that now lines up exactly gets a guide (can be several at once).
		for (float t : tx) for (float a : anchorsX) if (bestDx != Float.MAX_VALUE && Math.abs(t - (x + a)) < 0.01f && !guidesX.contains(t)) guidesX.add(t);
		for (float t : ty) for (float a : anchorsY) if (bestDy != Float.MAX_VALUE && Math.abs(t - (y + a)) < 0.01f && !guidesY.contains(t)) guidesY.add(t);
		return new float[] {x, y};
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		guidesX.clear();
		guidesY.clear();
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		HudModule m = elementAt(mouseX, mouseY);
		if (m == null) return true;
		// Keep the element's centre in place while it grows or shrinks.
		float cx = m.screenX(width) + m.scaledWidth() / 2f, cy = m.screenY(height) + m.scaledHeight() / 2f;
		m.scale.set(m.scale.get() + (scrollY > 0 ? 0.05 : -0.05));
		m.setScreenPos(cx - m.scaledWidth() / 2f, cy - m.scaledHeight() / 2f, width, height);
		TatnatClient.CONFIG.markDirty();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_R) {
			int scale = RenderUtils.guiScale();
			HudModule m = elementAt(Widgets.mouseX() / scale, Widgets.mouseY() / scale);
			if (m != null) {
				m.resetPosition();
				m.scale.reset();
				TatnatClient.CONFIG.markDirty();
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		TatnatClient.CONFIG.save();
		minecraft.setScreen(parent instanceof ClickGuiScreen ? new ClickGuiScreen() : parent);
	}
}
