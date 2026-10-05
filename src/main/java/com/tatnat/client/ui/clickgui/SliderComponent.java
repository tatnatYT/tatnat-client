package com.tatnat.client.ui.clickgui;

import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;

import net.minecraft.client.gui.GuiGraphics;

/**
 * "Scale: 1.25x" on the left, a draggable slider on the right. The knob glides while the stored
 * value snaps to the setting's step. Right-click resets to the default.
 */
public class SliderComponent extends SettingComponent<SliderSetting> {
	private boolean dragging;
	private int trackX;
	/** Smoothed knob position (0..1), so value snaps don't make the knob jump. */
	private float shown = -1;

	public SliderComponent(SliderSetting setting) {
		super(setting);
	}

	private static int trackW() {
		return Ui.px(170);
	}

	@Override
	protected int controlWidth() {
		return trackW();
	}

	@Override
	protected String label() {
		return setting.name + ": " + setting.display();
	}

	@Override
	protected void renderControl(GuiGraphics g, int cx, int cy, double mx, double my) {
		trackX = cx;
		int tw = trackW(), th = Math.max(4, Ui.px(6)), knob = Ui.px(9);
		float target = (float) setting.fraction();
		if (shown < 0) shown = target;
		shown += (target - shown) * 0.35f;
		if (Math.abs(target - shown) < 0.001f) shown = target;

		RenderUtils.roundedRect(g, cx, cy - th / 2, tw, th, th / 2, Theme.TRACK);
		int fill = Math.round(tw * shown);
		if (fill > 0) RenderUtils.roundedRect(g, cx, cy - th / 2, Math.max(th, fill), th, th / 2, Theme.ACCENT);
		boolean hover = dragging || Widgets.inside(mx, my, cx - knob, cy - knob * 2, tw + knob * 2, knob * 4);
		int kx = cx + fill;
		if (hover) RenderUtils.circle(g, kx, cy, knob + Ui.px(5), Colors.withAlpha(Theme.ACCENT, 0x40));
		RenderUtils.circle(g, kx, cy, knob, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		int knob = Ui.px(9);
		if (!Widgets.inside(mx, my, trackX - knob, y, trackW() + knob * 2, boxH())) return false;
		if (button == 1) {
			setting.reset();
			changed();
			return true;
		}
		if (button != 0) return false;
		dragging = true;
		mouseDragged(mx, my);
		return true;
	}

	@Override
	public void mouseDragged(double mx, double my) {
		if (!dragging) return;
		double f = Math.max(0, Math.min(1, (mx - trackX) / trackW()));
		setting.set(setting.min + f * (setting.max - setting.min));
		changed();
	}

	@Override
	public void mouseReleased(double mx, double my, int button) {
		dragging = false;
	}
}
