package com.tatnat.client.ui.clickgui;

import com.tatnat.client.util.KeyCodes;

import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Theme;

import com.tatnat.client.platform.Gfx;

/** A single-line text field. Click to edit; Enter, Escape or clicking elsewhere finishes. */
public class TextComponent extends SettingComponent<TextSetting> {
	private boolean focused;

	public TextComponent(TextSetting setting) {
		super(setting);
	}

	@Override
	protected int controlWidth() {
		return Ui.px(220);
	}

	@Override
	protected void renderControl(Gfx g, int cx, int cy, double mx, double my) {
		int fw = controlWidth(), fh = Ui.px(32), b = Math.max(1, Ui.px(1));
		int fy = cy - fh / 2;
		RenderUtils.roundedRect(g, cx - b, fy - b, fw + b * 2, fh + b * 2, Ui.px(Theme.RADIUS), focused ? Theme.ACCENT : Theme.TRACK);
		RenderUtils.roundedRect(g, cx, fy, fw, fh, Ui.px(Theme.RADIUS), 0xFF161618);
		String shown = setting.get();
		int room = fw - Ui.px(24);
		// Show the end of long text while typing, the start otherwise.
		while (UIFont.BODY.width(shown) > room && !shown.isEmpty()) shown = focused ? shown.substring(1) : shown.substring(0, shown.length() - 1);
		UIFont.BODY.drawMid(g, shown, cx + Ui.px(12), cy, Theme.TEXT);
		if (focused && System.currentTimeMillis() / 500 % 2 == 0) {
			int caret = cx + Ui.px(12) + UIFont.BODY.width(shown) + 1;
			RenderUtils.rect(g, caret, cy - Ui.px(9), caret + Math.max(1, Ui.px(2)), cy + Ui.px(9), Theme.ACCENT);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		boolean hit = inRow(mx, my);
		focused = hit && button == 0;
		return hit;
	}

	@Override
	public boolean keyPressed(int key) {
		if (!focused) return false;
		if (key == KeyCodes.BACKSPACE) {
			String v = setting.get();
			if (!v.isEmpty()) setting.set(v.substring(0, v.length() - 1));
			changed();
		} else if (key == KeyCodes.ENTER || key == KeyCodes.KP_ENTER || key == KeyCodes.ESCAPE) {
			focused = false;
		}
		return true;
	}

	@Override
	public boolean charTyped(String chars) {
		if (!focused) return false;
		setting.set(setting.get() + chars);
		changed();
		return true;
	}

	@Override
	public boolean isCapturingKeys() {
		return focused;
	}
}
