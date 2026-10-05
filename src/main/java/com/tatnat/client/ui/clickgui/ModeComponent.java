package com.tatnat.client.ui.clickgui;

import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;

import net.minecraft.client.gui.GuiGraphics;

/** A cycle button: left-click goes to the next option, right-click to the previous one. */
public class ModeComponent extends SettingComponent<ModeSetting> {
	public ModeComponent(ModeSetting setting) {
		super(setting);
	}

	@Override
	protected int controlWidth() {
		int widest = 0;
		for (String m : setting.modes) widest = Math.max(widest, UIFont.BODY.width(m));
		return Math.min(Ui.px(240), widest + Ui.px(64));
	}

	@Override
	protected void renderControl(GuiGraphics g, int cx, int cy, double mx, double my) {
		int bw = controlWidth(), bh = Ui.px(32);
		boolean hover = Widgets.inside(mx, my, cx, cy - bh / 2, bw, bh);
		RenderUtils.roundedRect(g, cx, cy - bh / 2, bw, bh, Ui.px(Theme.RADIUS), hover ? Colors.shade(Theme.TRACK, 1.2f) : Theme.TRACK);
		int arrow = Ui.px(12);
		Icons.draw(g, Icons.Icon.BACK, cx + Ui.px(16), cy, arrow, Theme.TEXT_MUTED);
		Icons.draw(g, Icons.Icon.FORWARD, cx + bw - Ui.px(16), cy, arrow, Theme.TEXT_MUTED);
		String v = UIFont.BODY.trim(setting.get(), bw - Ui.px(56));
		UIFont.BODY.drawCentered(g, v, cx + bw / 2, cy - UIFont.BODY.size() / 2, Theme.TEXT);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (!inRow(mx, my) || (button != 0 && button != 1)) return false;
		setting.cycle(button == 0 ? 1 : -1);
		changed();
		return true;
	}
}
