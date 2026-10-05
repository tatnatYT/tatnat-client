package com.tatnat.client.ui.clickgui;

import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Theme;

import net.minecraft.client.gui.GuiGraphics;

/** A red button on the right of the row that runs the setting's action. */
public class ActionComponent extends SettingComponent<ActionSetting> {
	public ActionComponent(ActionSetting setting) {
		super(setting);
	}

	@Override
	protected int controlWidth() {
		return Math.max(Ui.px(120), UIFont.BODY.width(setting.buttonText()) + Ui.px(36));
	}

	@Override
	protected void renderControl(GuiGraphics g, int cx, int cy, double mx, double my) {
		int bw = controlWidth(), bh = Ui.px(34);
		Widgets.button(g, cx, cy - bh / 2, bw, bh, setting.buttonText(), UIFont.BODY, Theme.ACCENT, Theme.ON_ACCENT,
				Widgets.inside(mx, my, cx, cy - bh / 2, bw, bh));
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button != 0 || !inRow(mx, my)) return false;
		setting.run();
		changed();
		return true;
	}
}
