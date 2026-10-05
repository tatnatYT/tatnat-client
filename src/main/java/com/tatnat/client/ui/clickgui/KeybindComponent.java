package com.tatnat.client.ui.clickgui;

import org.lwjgl.glfw.GLFW;

import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;
import com.tatnat.client.util.Keys;

import net.minecraft.client.gui.GuiGraphics;

/** Click, then press any key or mouse button to bind it. Escape cancels; Backspace/Delete unbinds. */
public class KeybindComponent extends SettingComponent<KeybindSetting> {
	private boolean listening;

	public KeybindComponent(KeybindSetting setting) {
		super(setting);
	}

	@Override
	protected int controlWidth() {
		return Ui.px(140);
	}

	@Override
	protected void renderControl(GuiGraphics g, int cx, int cy, double mx, double my) {
		int bw = controlWidth(), bh = Ui.px(32);
		int fy = cy - bh / 2;
		boolean hover = Widgets.inside(mx, my, cx, fy, bw, bh);
		int bg = listening ? Colors.withAlpha(Theme.ACCENT, 0x55) : hover ? Colors.shade(Theme.TRACK, 1.2f) : Theme.TRACK;
		RenderUtils.roundedRect(g, cx, fy, bw, bh, Ui.px(Theme.RADIUS), bg);
		String text = listening ? "Press a key..." : setting.keyName();
		UIFont.BODY.drawCentered(g, UIFont.BODY.trim(text, bw - Ui.px(14)), cx + bw / 2, cy - UIFont.BODY.size() / 2,
				listening || setting.isBound() ? Theme.TEXT : Theme.TEXT_MUTED);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (listening) {
			// Any mouse button except a plain left click on the row binds that button.
			if (button != 0 || !inRow(mx, my)) {
				setting.set(Keys.fromMouseButton(button));
				changed();
			}
			listening = false;
			return true;
		}
		if (button != 0 || !inRow(mx, my)) return false;
		listening = true;
		return true;
	}

	@Override
	public boolean keyPressed(int key) {
		if (!listening) return false;
		if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) setting.set(Keys.NONE);
		else if (key != GLFW.GLFW_KEY_ESCAPE) setting.set(key);
		listening = false;
		changed();
		return true;
	}

	@Override
	public boolean isCapturingKeys() {
		return listening;
	}
}
