package com.tatnat.client.ui.clickgui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.platform.UiScreen;
import com.tatnat.client.ui.render.RectBatch;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;
import com.tatnat.client.util.KeyCodes;

/**
 * A simple searchable list in the menu's style (Keybind Search, Pack Organizer): a title, a search
 * box you can type into straight away, and rows with buttons on the right. Esc goes back.
 */
public abstract class ListScreen implements UiScreen {
	/** One row: a label, a smaller line under it, and buttons (text + action) on the right. */
	protected static final class Row {
		final String label, sub;
		final List<String> buttons = new ArrayList<>();
		final List<Runnable> actions = new ArrayList<>();
		boolean highlight;

		protected Row(String label, String sub) {
			this.label = label;
			this.sub = sub == null ? "" : sub;
		}

		protected Row button(String text, Runnable action) {
			buttons.add(text);
			actions.add(action);
			return this;
		}

		protected Row highlight(boolean on) {
			highlight = on;
			return this;
		}
	}

	private final UiScreen parent;
	protected String search = "";
	private double scroll, scrollShown;
	private final List<int[]> hitBoxes = new ArrayList<>();
	private final List<Runnable> hitActions = new ArrayList<>();

	protected ListScreen(UiScreen parent) {
		this.parent = parent;
	}

	protected abstract String title();

	/** A line under the title (what the screen does / what you're doing right now). */
	protected abstract String hint();

	/** Every row; the screen filters them by the search text. */
	protected abstract List<Row> rows();

	/** A key press the subclass wants (e.g. a new key binding); true when used. */
	protected boolean onKey(int key) {
		return false;
	}

	protected void close() {
		if (parent != null) TatnatClient.game().openScreen(parent);
		else TatnatClient.game().closeScreen();
	}

	@Override
	public Backdrop backdrop() {
		return Backdrop.DIM;
	}

	@Override
	public boolean pausesGame() {
		return false;
	}

	@Override
	public void render(Gfx raw, double mouseX, double mouseY) {
		RectBatch g = RectBatch.of(raw);
		try {
			draw(g);
		} finally {
			g.flush();
		}
	}

	private void draw(Gfx g) {
		Ui.update();
		RenderUtils.alpha = 1f;
		RenderUtils.beginPixels(g);
		hitBoxes.clear();
		hitActions.clear();
		double mx = Widgets.mouseX(), my = Widgets.mouseY();
		int W = TatnatClient.game().windowWidth(), H = TatnatClient.game().windowHeight();
		int pw = Math.min(W - Ui.px(40), Ui.px(820)), ph = Math.min(H - Ui.px(40), Ui.px(660));
		int px = (W - pw) / 2, py = (H - ph) / 2;
		RenderUtils.verticalGradient(g, 0, 0, W, H, 0x66050508, 0xB0050508);
		int wr = Ui.px(Theme.RADIUS_WINDOW);
		RenderUtils.shadow(g, px, py, pw, ph, wr, 14, 0.6f);
		RenderUtils.roundedGradient(g, px, py, pw, ph, wr, Theme.WINDOW_TOP, Theme.WINDOW_BOTTOM);
		RenderUtils.glow(g, px + Ui.px(100), py + Ui.px(50), Ui.px(200), 0x18E5323E, 8);
		RenderUtils.roundedOutline(g, px, py, pw, ph, wr, 1, Theme.BORDER);
		RenderUtils.rect(g, px + wr, py + 1, px + pw - wr, py + 2, 0x14FFFFFF);

		int x = px + Ui.px(28), w = pw - Ui.px(56);
		// Back button, then the title and hint next to it.
		int bs = Ui.px(40), by = py + Ui.px(26);
		Widgets.iconButton(g, x, by, bs, com.tatnat.client.ui.render.Icons.Icon.BACK, Widgets.inside(mx, my, x, by, bs, bs), false, 0);
		hit(x, by, bs, bs, this::close);
		UIFont.HEADER.draw(g, title(), x + bs + Ui.px(16), py + Ui.px(22), Theme.TEXT);
		UIFont.SMALL.draw(g, UIFont.SMALL.trim(hint(), w - bs - Ui.px(16)), x + bs + Ui.px(16), py + Ui.px(56), Theme.TEXT_MUTED);

		// Search box (always typing into it).
		int sy = py + Ui.px(92), sh = Ui.px(42);
		RenderUtils.surface(g, x, sy, w, sh, sh / 2, 0xFF22242B, 0xFF16171B, Colors.withAlpha(Theme.ACCENT, 0xB0));
		com.tatnat.client.ui.render.Icons.draw(g, com.tatnat.client.ui.render.Icons.Icon.SEARCH, x + Ui.px(22), sy + sh / 2, Ui.px(16), Theme.ACCENT);
		String shown = search.isEmpty() ? "Type to search…" : search + (System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
		UIFont.BODY.drawMid(g, shown, x + Ui.px(44), sy + sh / 2, search.isEmpty() ? 0xFF7A7C84 : Theme.TEXT);

		// Rows.
		String q = search.toLowerCase(Locale.ROOT).trim();
		List<Row> list = new ArrayList<>();
		for (Row r : rows()) if (q.isEmpty() || (r.label + " " + r.sub).toLowerCase(Locale.ROOT).contains(q)) list.add(r);
		int top = sy + sh + Ui.px(16), bottom = py + ph - Ui.px(22), rh = Ui.px(60), gap = Ui.px(8);
		int total = list.size() * (rh + gap);
		scroll = Math.max(0, Math.min(scroll, Math.max(0, total - (bottom - top))));
		scrollShown += (scroll - scrollShown) * 0.35;
		g.scissor(px, top, px + pw, bottom);
		int y = top - (int) Math.round(scrollShown);
		for (Row r : list) {
			if (y + rh >= top && y <= bottom) {
				boolean rowHover = Widgets.inside(mx, my, x, y, w, rh) && my >= top && my <= bottom;
				RenderUtils.surface(g, x, y, w, rh, Ui.px(Theme.RADIUS_CARD), r.highlight ? Theme.CARD_ON_TOP : rowHover ? Theme.CARD_HOVER_TOP : Theme.CARD_TOP,
						rowHover ? Theme.CARD_HOVER_BOTTOM : Theme.CARD_BOTTOM, r.highlight ? 0x70E5323E : rowHover ? Theme.BORDER_HOVER : Theme.BORDER);
				if (r.highlight) RenderUtils.roundedRect(g, x + Ui.px(6), y + Ui.px(14), Math.max(2, Ui.px(4)), rh - Ui.px(28), Math.max(1, Ui.px(2)), Theme.ACCENT);
				UIFont.TITLE.draw(g, r.label, x + Ui.px(16), y + rh / 2 - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
				UIFont.SMALL.draw(g, r.sub, x + Ui.px(16), y + rh / 2 + Ui.px(4), Theme.TEXT_MUTED);
				int right = x + w - Ui.px(12);
				for (int i = r.buttons.size() - 1; i >= 0; i--) {
					String t = r.buttons.get(i);
					int tw = Math.max(Ui.px(70), UIFont.BODY.width(t) + Ui.px(24)), th = Ui.px(34);
					int tx = right - tw, ty = y + (rh - th) / 2;
					boolean hov = Widgets.inside(mx, my, tx, ty, tw, th) && my >= top && my <= bottom;
					Widgets.button(g, tx, ty, tw, th, t, UIFont.BODY, i == 0 ? Theme.ACCENT : 0xFF2A2C33, 0xFFFFFFFF, hov);
					if (ty >= top && ty + th <= bottom) hit(tx, ty, tw, th, r.actions.get(i));
					right = tx - Ui.px(8);
				}
			}
			y += rh + gap;
		}
		if (list.isEmpty()) UIFont.BODY.draw(g, "Nothing matches \"" + search + "\".", x, top + Ui.px(8), Theme.TEXT_MUTED);
		g.endScissor();
		RenderUtils.end(g);
	}

	private void hit(int x, int y, int w, int h, Runnable action) {
		hitBoxes.add(new int[] {x, y, w, h});
		hitActions.add(action);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		double mx = Widgets.mouseX(), my = Widgets.mouseY();
		for (int i = hitBoxes.size() - 1; i >= 0; i--) {
			int[] b = hitBoxes.get(i);
			if (Widgets.inside(mx, my, b[0], b[1], b[2], b[3])) {
				hitActions.get(i).run();
				return true;
			}
		}
		return true;
	}

	@Override
	public void mouseReleased(double x, double y, int button) {
	}

	@Override
	public void mouseDragged(double x, double y, int button) {
	}

	@Override
	public void mouseScrolled(double x, double y, double amount) {
		scroll -= amount * Ui.px(64);
	}

	@Override
	public boolean keyPressed(int key, boolean shift) {
		if (onKey(key)) return true;
		if (key == KeyCodes.ESCAPE) {
			close();
			return true;
		}
		if (key == KeyCodes.BACKSPACE && !search.isEmpty()) {
			search = search.substring(0, search.length() - 1);
			scroll = 0;
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(String chars) {
		if (search.length() < 40) search += chars;
		scroll = 0;
		return true;
	}

	@Override
	public void removed() {
	}

	@Override
	public boolean closesOnEscape() {
		return false;
	}
}
