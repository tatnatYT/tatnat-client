package com.tatnat.client.ui.clickgui;

import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.ui.render.Animation;

import com.tatnat.client.platform.Gfx;

/** Text on the left, sliding toggle switch on the right. Clicking anywhere on the row flips it. */
public class BooleanComponent extends SettingComponent<BooleanSetting> {
	private final Animation anim;

	public BooleanComponent(BooleanSetting setting) {
		super(setting);
		anim = new Animation(150, setting.on() ? 1f : 0f);
	}

	@Override
	protected void renderControl(Gfx g, int cx, int cy, double mx, double my) {
		anim.animateTo(setting.on() ? 1f : 0f);
		Widgets.toggle(g, cx, cy - Widgets.toggleH() / 2, anim, inRow(mx, my));
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button != 0 || !inRow(mx, my)) return false;
		setting.toggle();
		changed();
		return true;
	}
}
