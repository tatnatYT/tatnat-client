package com.tatnat.client.ui.clickgui;

import java.util.Locale;

import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.ui.render.Animation;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;

import com.tatnat.client.platform.Gfx;

/**
 * A colour swatch that unfolds into an HSV picker: saturation/brightness square, hue bar,
 * opacity bar and (if allowed) a Chroma switch. Hue is kept apart from the stored colour so
 * dragging into grey and back doesn't lose it. Right-click the row to reset.
 */
public class ColorComponent extends SettingComponent<ColorSetting> {
	private boolean open;
	private final Animation openAnim = new Animation(200, 0f);
	private final Animation chromaAnim;
	private float hue, sat, val;
	private int dragging; // 0 none, 1 square, 2 hue, 3 alpha
	private int sqX, sqY, sqW, sqH, bar, hueX, alphaY, alphaH, chromaY;

	public ColorComponent(ColorSetting setting) {
		super(setting);
		syncHsv();
		chromaAnim = new Animation(150, setting.chroma() ? 1f : 0f);
	}

	private void syncHsv() {
		float[] hsv = Colors.toHsv(setting.get());
		hue = hsv[0];
		sat = hsv[1];
		val = hsv[2];
	}

	private int pickerHeight() {
		return Ui.px(12 + 130 + 12 + 14 + 14 + (setting.allowChroma ? 40 : 0) + 6);
	}

	@Override
	public int height() {
		return rowH() + Math.round(pickerHeight() * openAnim.get());
	}

	@Override
	protected int controlWidth() {
		return Ui.px(52);
	}

	@Override
	protected void renderControl(Gfx g, int cx, int cy, double mx, double my) {
		int sw = controlWidth(), sh = Ui.px(26), b = Math.max(1, Ui.px(2));
		RenderUtils.roundedRect(g, cx - b, cy - sh / 2 - b, sw + b * 2, sh + b * 2, Ui.px(Theme.RADIUS), open ? Theme.ACCENT : Theme.TRACK);
		RenderUtils.roundedRect(g, cx, cy - sh / 2, sw, sh, Ui.px(Theme.RADIUS_SMALL), Colors.withAlpha(setting.color(), 255));
	}

	/** Dev test hook. */
	void devOpen() {
		open = true;
		openAnim.animateTo(1f);
	}

	@Override
	public void render(Gfx g, int x, int y, int w, double mx, double my) {
		super.render(g, x, y, w, mx, my);
		float t = openAnim.get();
		if (t <= 0.01f) return;
		float prev = RenderUtils.alpha;
		RenderUtils.alpha = prev * t;
		int py = y + boxH() + Ui.px(2);
		int ph = pickerHeight() - Ui.px(8);
		// Clip to the animated height so the picker unfolds instead of popping in.
		g.scissor(x, py, x + w, py + Math.round(pickerHeight() * t));
		RenderUtils.roundedRect(g, x, py, w, ph, Ui.px(Theme.RADIUS), Theme.PANEL);

		int pad = pad();
		bar = Ui.px(18);
		sqX = x + pad;
		sqY = py + Ui.px(12);
		sqW = w - pad * 3 - bar;
		sqH = Ui.px(130);
		hueX = sqX + sqW + pad;
		alphaY = sqY + sqH + Ui.px(12);
		alphaH = Ui.px(14);
		chromaY = alphaY + alphaH + Ui.px(14);

		// Saturation (x) / brightness (y) square as 2px vertical gradient strips.
		for (int i = 0; i < sqW; i += 2) {
			int top = Colors.hsv(hue, i / (float) sqW, 1f);
			RenderUtils.verticalGradient(g, sqX + i, sqY, Math.min(sqX + i + 2, sqX + sqW), sqY + sqH, top, 0xFF000000);
		}
		int kx = sqX + Math.round(sat * sqW), ky = sqY + Math.round((1 - val) * sqH);
		RenderUtils.circle(g, kx, ky, Ui.px(7), 0xFFFFFFFF);
		RenderUtils.circle(g, kx, ky, Ui.px(5), Colors.hsv(hue, sat, val));

		// Hue bar.
		for (int i = 0; i < sqH; i += 2) {
			RenderUtils.rect(g, hueX, sqY + i, hueX + bar, Math.min(sqY + i + 2, sqY + sqH), Colors.hsv(i / (float) sqH, 1f, 1f));
		}
		int hy = sqY + Math.round(hue * sqH);
		RenderUtils.roundedRect(g, hueX - Ui.px(3), hy - Ui.px(3), bar + Ui.px(6), Ui.px(6), Ui.px(3), 0xFFFFFFFF);

		// Opacity bar over a checkerboard so transparency is visible.
		int ab = sqW + pad + bar, cell = Math.max(3, alphaH / 2);
		for (int i = 0; i < ab; i += cell) {
			for (int r = 0; r < 2; r++) {
				int c = ((i / cell) + r) % 2 == 0 ? 0xFF55555C : 0xFF2A2A30;
				RenderUtils.rect(g, sqX + i, alphaY + r * cell, Math.min(sqX + i + cell, sqX + ab), Math.min(alphaY + (r + 1) * cell, alphaY + alphaH), c);
			}
		}
		int rgb = Colors.hsv(hue, sat, val);
		RenderUtils.horizontalGradient(g, sqX, alphaY, sqX + ab, alphaY + alphaH, Colors.withAlpha(rgb, 0), Colors.withAlpha(rgb, 255));
		int ax = sqX + Math.round(Colors.alpha(setting.get()) / 255f * ab);
		RenderUtils.roundedRect(g, ax - Ui.px(3), alphaY - Ui.px(3), Ui.px(6), alphaH + Ui.px(6), Ui.px(3), 0xFFFFFFFF);

		if (setting.allowChroma) {
			chromaAnim.animateTo(setting.chroma() ? 1f : 0f);
			int mid = chromaY + Widgets.toggleH() / 2;
			UIFont.BODY.drawMid(g, "Chroma", sqX, mid, Theme.TEXT);
			String hex = String.format(Locale.ROOT, "#%06X", setting.get() & 0xFFFFFF);
			UIFont.SMALL.drawMid(g, hex, sqX + UIFont.BODY.width("Chroma") + Ui.px(14), mid, Theme.TEXT_MUTED);
			int tx = hueX + bar - Widgets.toggleW();
			Widgets.toggle(g, tx, chromaY, chromaAnim, Widgets.inside(mx, my, tx, chromaY, Widgets.toggleW(), Widgets.toggleH()));
		}
		g.endScissor();
		RenderUtils.alpha = prev;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (inRow(mx, my)) {
			if (button == 1) {
				setting.reset();
				syncHsv();
				changed();
				return true;
			}
			open = !open;
			openAnim.animateTo(open ? 1f : 0f);
			return true;
		}
		if (!open || button != 0) return false;
		int m = Ui.px(5);
		if (Widgets.inside(mx, my, sqX - m, sqY - m, sqW + m * 2, sqH + m * 2)) dragging = 1;
		else if (Widgets.inside(mx, my, hueX - m, sqY - m, bar + m * 2, sqH + m * 2)) dragging = 2;
		else if (Widgets.inside(mx, my, sqX - m, alphaY - m, sqW + pad() + bar + m * 2, alphaH + m * 2)) dragging = 3;
		else if (setting.allowChroma && Widgets.inside(mx, my, sqX, chromaY, sqW + pad() + bar, Widgets.toggleH())) {
			setting.setChroma(!setting.chroma());
			changed();
			return true;
		}
		if (dragging != 0) {
			mouseDragged(mx, my);
			return true;
		}
		return Widgets.inside(mx, my, x, y, w, height());
	}

	@Override
	public void mouseDragged(double mx, double my) {
		if (dragging == 1) {
			sat = clamp01((float) (mx - sqX) / sqW);
			val = 1f - clamp01((float) (my - sqY) / sqH);
		} else if (dragging == 2) {
			hue = clamp01((float) (my - sqY) / sqH);
		} else if (dragging == 3) {
			int a = Math.round(clamp01((float) (mx - sqX) / (sqW + pad() + bar)) * 255f);
			setting.set(Colors.withAlpha(setting.get(), a));
			changed();
			return;
		} else {
			return;
		}
		setting.set(Colors.withAlpha(Colors.hsv(hue, sat, val), Colors.alpha(setting.get())));
		changed();
	}

	@Override
	public void mouseReleased(double mx, double my, int button) {
		dragging = 0;
	}

	private static float clamp01(float f) {
		return Math.max(0f, Math.min(1f, f));
	}
}
