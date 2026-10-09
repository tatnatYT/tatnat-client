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
		RenderUtils.shadow(g, px, py, pw, ph, Ui.px(Theme.RADIUS_LARGE), 8, 0.35f);
		RenderUtils.roundedRect(g, px, py, pw, ph, Ui.px(Theme.RADIUS_LARGE), Theme.BACKGROUND);

		int x = px + Ui.px(22), w = pw - Ui.px(44);
		UIFont.HEADER.draw(g, title(), x, py + Ui.px(20), Theme.TEXT);
		UIFont.SMALL.draw(g, hint(), x, py + Ui.px(56), Theme.TEXT_MUTED);
		// Back button.
		int bw = Ui.px(90), bh = Ui.px(34), bx = px + pw - bw - Ui.px(22), by = py + Ui.px(18);
		Widgets.button(g, bx, by, bw, bh, "Back", UIFont.BODY, Theme.PANEL, Theme.TEXT, Widgets.inside(mx, my, bx, by, bw, bh));
		hit(bx, by, bw, bh, this::close);

		// Search box (always typing into it).
		int sy = py + Ui.px(84), sh = Ui.px(40);
		RenderUtils.roundedRect(g, x - 1, sy - 1, w + 2, sh + 2, Ui.px(Theme.RADIUS), Theme.ACCENT);
		RenderUtils.roundedRect(g, x, sy, w, sh, Ui.px(Theme.RADIUS), Theme.PANEL);
		String shown = search.isEmpty() ? "Type to search…" : search + (System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
		UIFont.BODY.drawMid(g, shown, x + Ui.px(14), sy + sh / 2, search.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT);

		// Rows.
		String q = search.toLowerCase(Locale.ROOT).trim();
		List<Row> list = new ArrayList<>();
		for (Row r : rows()) if (q.isEmpty() || (r.label + " " + r.sub).toLowerCase(Locale.ROOT).contains(q)) list.add(r);
		int top = sy + sh + Ui.px(14), bottom = py + ph - Ui.px(18), rh = Ui.px(54), gap = Ui.px(6);
		int total = list.size() * (rh + gap);
		scroll = Math.max(0, Math.min(scroll, Math.max(0, total - (bottom - top))));
		scrollShown += (scroll - scrollShown) * 0.35;
		g.scissor(px, top, px + pw, bottom);
		int y = top - (int) Math.round(scrollShown);
		for (Row r : list) {
			if (y + rh >= top && y <= bottom) {
				RenderUtils.roundedRect(g, x, y, w, rh, Ui.px(Theme.RADIUS), r.highlight ? Theme.HOVER : Theme.PANEL);
				if (r.highlight) RenderUtils.rect(g, x, y + Ui.px(8), x + Ui.px(3), y + rh - Ui.px(8), Theme.ACCENT);
				UIFont.TITLE.draw(g, r.label, x + Ui.px(16), y + rh / 2 - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
				UIFont.SMALL.draw(g, r.sub, x + Ui.px(16), y + rh / 2 + Ui.px(4), Theme.TEXT_MUTED);
				int right = x + w - Ui.px(12);
				for (int i = r.buttons.size() - 1; i >= 0; i--) {
					String t = r.buttons.get(i);
					int tw = Math.max(Ui.px(70), UIFont.BODY.width(t) + Ui.px(24)), th = Ui.px(34);
					int tx = right - tw, ty = y + (rh - th) / 2;
					boolean hov = Widgets.inside(mx, my, tx, ty, tw, th) && my >= top && my <= bottom;
					Widgets.button(g, tx, ty, tw, th, t, UIFont.BODY, i == 0 ? Theme.ACCENT : Theme.HOVER, 0xFFFFFFFF, hov);
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
