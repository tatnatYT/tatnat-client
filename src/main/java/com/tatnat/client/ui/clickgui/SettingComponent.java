package com.tatnat.client.ui.clickgui;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.Setting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Theme;

import com.tatnat.client.platform.Gfx;

/**
 * One row in a mod's settings page. Rows draw the name and a muted description on the left and
 * their control on the right; subclasses only draw the control and handle its input. Coordinates
 * are real pixels; sizes are design pixels run through {@link Ui#px}.
 */
public abstract class SettingComponent<S extends Setting<?>> {
	protected final S setting;
	/** Position from the last frame, used for hit-testing. */
	protected int x, y, w;

	protected SettingComponent(S setting) {
		this.setting = setting;
	}

	public static SettingComponent<?> of(Setting<?> s) {
		if (s instanceof BooleanSetting) return new BooleanComponent((BooleanSetting) s);
		if (s instanceof SliderSetting) return new SliderComponent((SliderSetting) s);
		if (s instanceof ModeSetting) return new ModeComponent((ModeSetting) s);
		if (s instanceof ColorSetting) return new ColorComponent((ColorSetting) s);
		if (s instanceof TextSetting) return new TextComponent((TextSetting) s);
		if (s instanceof KeybindSetting) return new KeybindComponent((KeybindSetting) s);
		if (s instanceof com.tatnat.client.modules.settings.ActionSetting) return new ActionComponent((com.tatnat.client.modules.settings.ActionSetting) s);
		throw new IllegalArgumentException("No component for " + s.getClass().getSimpleName());
	}

	/** Row height including the gap below it. */
	public static int rowH() {
		return Ui.px(64);
	}

	protected static int pad() {
		return Ui.px(16);
	}

	/** The visible box of the row (without the gap). */
	protected int boxH() {
		return rowH() - Ui.px(8);
	}

	public boolean visible() {
		return setting.isVisible();
	}

	public int height() {
		return rowH();
	}

	/** Width reserved on the right for the control, so the label can be trimmed to fit. */
	protected int controlWidth() {
		return Widgets.toggleW();
	}

	/** The label text; sliders override this to include their value. */
	protected String label() {
		return setting.name;
	}

	public void render(Gfx g, int x, int y, int w, double mx, double my) {
		this.x = x;
		this.y = y;
		this.w = w;
		boolean hover = Widgets.inside(mx, my, x, y, w, boxH());
		RenderUtils.roundedRect(g, x, y, w, boxH(), Ui.px(Theme.RADIUS), hover ? Theme.HOVER : Theme.PANEL);
		int textW = w - pad() * 3 - controlWidth();
		int mid = y + boxH() / 2;
		UIFont.TITLE.draw(g, UIFont.TITLE.trim(label(), textW), x + pad(), mid - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
		UIFont.SMALL.draw(g, UIFont.SMALL.trim(setting.description, textW), x + pad(), mid + Ui.px(4), Theme.TEXT_MUTED);
		renderControl(g, x + w - pad() - controlWidth(), mid, mx, my);
	}

	/** Draws the control with its left edge at {@code cx} and vertically centred on {@code cy}. */
	protected abstract void renderControl(Gfx g, int cx, int cy, double mx, double my);

	public boolean mouseClicked(double mx, double my, int button) {
		return false;
	}

	public void mouseReleased(double mx, double my, int button) {
	}

	public void mouseDragged(double mx, double my) {
	}

	/** Returns true if the key was used (so Escape doesn't also close the menu). */
	public boolean keyPressed(int key) {
		return false;
	}

	public boolean charTyped(String chars) {
		return false;
	}

	/** True while this component wants all keyboard input (text field, key listener). */
	public boolean isCapturingKeys() {
		return false;
	}

	protected boolean inRow(double mx, double my) {
		return Widgets.inside(mx, my, x, y, w, boxH());
	}

	protected static void changed() {
		TatnatClient.CONFIG.markDirty();
	}
}
