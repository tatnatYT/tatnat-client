package com.tatnat.client.ui.clickgui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.account.AccountSwitcher;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.ClientOptions;
import com.tatnat.client.modules.ModuleManager;
import com.tatnat.client.modules.Performance;
import com.tatnat.client.modules.settings.Setting;
import com.tatnat.client.ui.hud.HudEditorScreen;
import com.tatnat.client.ui.render.Animation;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.Icons.Icon;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.RectBatch;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;

import com.tatnat.client.platform.Gfx;
import com.tatnat.client.platform.UiScreen;
import com.tatnat.client.util.KeyCodes;

/**
 * The Right Shift mod menu, laid out like Feather's:
 *
 * <pre>
 *                      v                  [move][heart][grid][list]
 *  [logo ]  [ Mod Menu /  All  HUD  Visual  Utility  Cosmetic   [search] ]
 *  [ MODS]  [ +--------+  +--------+  +--------+                         ]
 *  [ HUD ]  [ |  icon  |  |  icon  |  |  icon  |   3-column card grid   ]
 *  [ SET ]  [ |name [o]|  |name [o]|  |name [o]|   (or a list)          ]
 * </pre>
 *
 * Clicking a card opens that mod's settings in the same panel; the red tab turns into a back
 * button. Everything is drawn in real pixels, laid out for 1920x1080 and scaled by {@link Ui}.
 * The menu fades in and rises 10px over 200ms; toggles slide over 150ms; lists scroll smoothly.
 */
public class ClickGuiScreen implements UiScreen {
	private static final String YOUTUBE = "https://www.youtube.com/@tatnatmc";

	private enum Page { MODS, PERFORMANCE, SETTINGS, ACCOUNTS }

	// Remembered between openings, like Feather.
	private static Category lastCategory = null;
	private static boolean lastFavorites, lastList;

	private final Animation open = new Animation(200, 0f);
	private final Animation view = new Animation(220, 0f);
	private final Animation titleButtonAnim = new Animation(150, ClientOptions.titleButton() ? 1f : 0f);
	private final Map<Module, Animation> toggles = new HashMap<>();
	private final Map<Module, Animation> hovers = new HashMap<>();

	private Page page = Page.MODS;
	private Category category = lastCategory;
	private boolean favoritesOnly = lastFavorites, listView = lastList;
	private Module selected;
	private Module shown;
	private List<SettingComponent<?>> components = new ArrayList<>();
	private String search = "";
	private boolean searchFocused, closing;
	private long confirmResetAll;

	private double scroll, scrollTarget, setScroll, setScrollTarget;
	/** Share of the remaining scroll distance covered this frame (time-based, same feel at any FPS). */
	private double scrollEase = 1;
	private long lastFrame;
	private int contentH, setContentH;
	private int clipX, clipY, clipW, clipH;

	/** Clickable regions registered while drawing; checked in reverse (topmost first). */
	private static final class Hit {
		final int x, y, w, h;
		final boolean clipped;
		final Runnable left, right;

		Hit(int x, int y, int w, int h, boolean clipped, Runnable left, Runnable right) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.clipped = clipped;
			this.left = left;
			this.right = right;
		}
	}

	private final List<Hit> hits = new ArrayList<>();

	public ClickGuiScreen() {
		open.animateTo(1f);
	}

	// ---------------------------------------------------------------- state

	private List<Module> visibleModules() {
		List<Module> out = new ArrayList<>();
		String q = search.trim().toLowerCase(Locale.ROOT);
		for (Module m : ModuleManager.get().all()) {
			if (!q.isEmpty() && !(m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q))) continue;
			if (q.isEmpty() && category != null && m.category != category) continue;
			if (favoritesOnly && !m.isFavorite()) continue;
			out.add(m);
		}
		// Favourites first, then the registration order.
		out.sort(Comparator.comparing(m -> !m.isFavorite()));
		return out;
	}

	private void openSettings(Module m) {
		selected = m;
		shown = m;
		components = new ArrayList<>();
		for (Setting<?> s : m.settings()) components.add(SettingComponent.of(s));
		components.add(SettingComponent.of(m.toggleKey));
		setScroll = setScrollTarget = 0;
		view.animateTo(1f);
	}

	private void closeSettings() {
		selected = null;
		view.animateTo(0f);
	}

	/** Opens straight onto one mod's settings (right-click in the HUD editor). */
	public void openSettingsFor(Module m) {
		page = Page.MODS;
		category = null;
		openSettings(m);
		view.snap(1f);
	}

	/** Dev test hook: expands the first colour setting of the open mod. */
	public void devOpenFirstColorPicker() {
		for (SettingComponent<?> c : components) {
			if (c instanceof ColorComponent) {
				((ColorComponent) c).devOpen();
				return;
			}
		}
	}

	/** Dev test hook: types into the search box. */
	public void devSearch(String text) {
		search = text;
		searchFocused = !text.isEmpty();
		scrollTarget = 0;
	}

	/** Dev test hook. */
	public void devCloseSettings() {
		closeSettings();
		view.snap(0f);
	}

	/** Dev test hook: switches page / view. */
	public void devShow(boolean settingsPage, boolean list) {
		page = settingsPage ? Page.SETTINGS : Page.MODS;
		listView = list;
	}

	private Animation toggleAnim(Module m) {
		return toggles.computeIfAbsent(m, k -> new Animation(150, k.isEnabled() ? 1f : 0f));
	}

	private Animation hoverAnim(Module m) {
		return hovers.computeIfAbsent(m, k -> new Animation(120, 0f));
	}

	private void close() {
		if (closing) return;
		closing = true;
		open.animateTo(0f);
	}

	@Override
	public Backdrop backdrop() {
		// Like Feather: the world stays fully visible behind the menu.
		return Backdrop.CLEAR;
	}

	@Override
	public boolean pausesGame() {
		return false;
	}

	@Override
	public void removed() {
		RenderUtils.alpha = 1f;
		lastCategory = category;
		lastFavorites = favoritesOnly;
		lastList = listView;
		TatnatClient.CONFIG.save();
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void render(Gfx raw, double mouseX, double mouseY) {
		// Thousands of tiny fills per frame: batch them (see RectBatch).
		RectBatch g = RectBatch.of(raw);
		try {
			renderBatched(g, mouseX, mouseY);
		} finally {
			g.flush();
		}
	}

	private void renderBatched(Gfx g, double mouseX, double mouseY) {
		long now = System.nanoTime();
		double dt = lastFrame == 0 ? 1 / 60.0 : Math.min(0.1, (now - lastFrame) / 1e9);
		lastFrame = now;
		scrollEase = Performance.smoothScroll() ? 1 - Math.exp(-dt * 18) : 1;
		if (closing && open.isDone()) {
			TatnatClient.game().closeScreen();
			return;
		}
		Ui.update();
		float o = open.get();
		RenderUtils.alpha = o;
		RenderUtils.beginPixels(g);
		hits.clear();
		double mx = Widgets.mouseX(), my = Widgets.mouseY();

		int W = TatnatClient.game().windowWidth(), H = TatnatClient.game().windowHeight();
		int sideW = Ui.px(96), gap = Ui.px(14), panelW = Ui.px(912), panelH = Ui.px(666);
		int groupX = (W - (sideW + gap + panelW)) / 2;
		int panelX = groupX + sideW + gap;
		int panelY = (H - panelH) / 2 + Ui.px(18) + Math.round((1 - o) * Ui.px(10));

		drawToolbar(g, panelX, panelY, panelW, mx, my);
		drawSidebar(g, groupX, panelY, sideW, panelH, mx, my);

		RenderUtils.shadow(g, panelX, panelY, panelW, panelH, Ui.px(Theme.RADIUS_LARGE), 8, 0.35f);
		RenderUtils.roundedRect(g, panelX, panelY, panelW, panelH, Ui.px(Theme.RADIUS_LARGE), Theme.BACKGROUND);
		if (page == Page.MODS) drawModsPage(g, panelX, panelY, panelW, panelH, mx, my);
		else if (page == Page.PERFORMANCE) drawPerformancePage(g, panelX, panelY, panelW, panelH, mx, my);
		else if (page == Page.ACCOUNTS) drawAccountsPage(g, panelX, panelY, panelW, panelH, mx, my);
		else drawSettingsPage(g, panelX, panelY, panelW, panelH, mx, my);

		RenderUtils.end(g);
		RenderUtils.alpha = 1f;
	}

	/** The small bar above the panel: close chevron in the middle, view buttons on the right. */
	private void drawToolbar(Gfx g, int px, int py, int pw, double mx, double my) {
		int cy = py - Ui.px(40);
		int cs = Ui.px(30);
		boolean chevHover = Widgets.inside(mx, my, px + pw / 2 - cs, cy - cs / 2, cs * 2, cs);
		// Drop shadow first so the chevron stays visible over bright terrain.
		Icons.draw(g, Icon.CHEVRON_DOWN, px + pw / 2, cy + Math.max(1, Ui.px(2)), cs, 0x90000000);
		Icons.draw(g, Icon.CHEVRON_DOWN, px + pw / 2, cy, cs, chevHover ? 0xFFFFFFFF : 0xFFE8E8E8);
		hit(px + pw / 2 - cs, cy - cs / 2, cs * 2, cs, false, this::close, null);

		int bs = Ui.px(46), bgap = Ui.px(4), padd = Ui.px(5);
		Icon[] icons = {Icon.MOVE, favoritesOnly ? Icon.HEART_FILLED : Icon.HEART, Icon.GRID, Icon.LIST};
		int tw = icons.length * bs + (icons.length - 1) * bgap + padd * 2, th = bs + padd * 2;
		int tx = px + pw - tw, ty = py - th - Ui.px(12);
		RenderUtils.roundedRect(g, tx, ty, tw, th, Ui.px(Theme.RADIUS), Theme.BACKGROUND);
		for (int i = 0; i < icons.length; i++) {
			int bx = tx + padd + i * (bs + bgap), by = ty + padd;
			boolean active = (i == 1 && favoritesOnly) || (i == 2 && !listView) || (i == 3 && listView);
			boolean hover = Widgets.inside(mx, my, bx, by, bs, bs);
			if (hover) RenderUtils.roundedRect(g, bx, by, bs, bs, Ui.px(Theme.RADIUS_SMALL), Theme.HOVER);
			int col = i == 1 && favoritesOnly ? Theme.ACCENT : active || hover ? 0xFFFFFFFF : Theme.TEXT_MUTED;
			Icons.draw(g, icons[i], bx + bs / 2, by + bs / 2, Ui.px(24), col);
			final int idx = i;
			hit(bx, by, bs, bs, false, () -> {
				if (idx == 0) {
					TatnatClient.game().openScreen(new HudEditorScreen(this));
				} else if (idx == 1) {
					favoritesOnly = !favoritesOnly;
					page = Page.MODS;
					closeSettings();
					scrollTarget = 0;
				} else {
					listView = idx == 3;
				}
			}, null);
		}
	}

	private void drawSidebar(Gfx g, int x, int y, int w, int h, double mx, double my) {
		RenderUtils.shadow(g, x, y, w, h, Ui.px(Theme.RADIUS_LARGE), 8, 0.35f);
		RenderUtils.roundedRect(g, x, y, w, h, Ui.px(Theme.RADIUS_LARGE), Theme.BACKGROUND);

		// Logo (the player's head), drawn at an exact multiple of 8 so the pixels stay square.
		int logo = Math.max(32, Ui.px(56) / 8 * 8);
		g.logo(x + (w - logo) / 2, y + Ui.px(18), logo, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		RenderUtils.rect(g, x + Ui.px(16), y + Ui.px(90), x + w - Ui.px(16), y + Ui.px(91), Theme.DIVIDER);

		String[] labels = {"MOD MENU", "HUD EDITOR", "PERFORMANCE", "SETTINGS", "ACCOUNTS"};
		Icon[] icons = {Icon.GRID, Icon.MOVE, Icon.CHIP, Icon.GEAR, Icon.USER};
		int bw = w - Ui.px(16), bh = Ui.px(70);
		for (int i = 0; i < labels.length; i++) {
			int bx = x + Ui.px(8), by = y + Ui.px(104) + i * (bh + Ui.px(8));
			boolean active = (i == 0 && page == Page.MODS) || (i == 2 && page == Page.PERFORMANCE) || (i == 3 && page == Page.SETTINGS)
					|| (i == 4 && page == Page.ACCOUNTS);
			boolean hover = Performance.hoverEffects() && Widgets.inside(mx, my, bx, by, bw, bh);
			if (active) RenderUtils.roundedRect(g, bx, by, bw, bh, Ui.px(Theme.RADIUS), Theme.ACCENT);
			else if (hover) RenderUtils.roundedRect(g, bx, by, bw, bh, Ui.px(Theme.RADIUS), Theme.HOVER);
			int col = active || hover ? 0xFFFFFFFF : Theme.TEXT_MUTED;
			Icons.draw(g, icons[i], bx + bw / 2, by + Ui.px(28), Ui.px(28), col);
			UIFont.TINY.drawCentered(g, labels[i], bx + bw / 2, by + Ui.px(50), col);
			final int idx = i;
			hit(bx, by, bw, bh, false, () -> {
				if (idx == 0) {
					page = Page.MODS;
					closeSettings();
					view.snap(0f);
				} else if (idx == 1) {
					TatnatClient.game().openScreen(new HudEditorScreen(this));
				} else if (idx == 2) {
					openPerformance();
				} else if (idx == 3) {
					page = Page.SETTINGS;
				} else {
					page = Page.ACCOUNTS;
					AccountSwitcher.refresh();
				}
			}, null);
		}
	}

	/** Red slanted tab in the panel's top-left corner. Returns its right edge. */
	private int drawTab(Gfx g, int x, int y, String text, boolean back, double mx, double my) {
		int th = Ui.px(64), slant = Ui.px(26);
		int textX = x + Ui.px(back ? 52 : 26);
		int tw = textX - x + UIFont.HEADER.width(text) + Ui.px(26) + slant;
		int r = Ui.px(Theme.RADIUS_LARGE);
		boolean hover = back && Widgets.inside(mx, my, x, y, tw, th);
		int col = hover ? Colors.shade(Theme.ACCENT, 1.1f) : Theme.ACCENT;
		// Square-ish body with the panel's rounded top-left corner, then the slanted right edge.
		RenderUtils.roundedRect(g, x, y, tw - slant, th, r, col, true, false, false, false);
		for (int row = 0; row < th; row++) {
			float right = tw - slant * (row + 0.5f) / th;
			int start = x + tw - slant;
			float end = x + right;
			if (end > start) {
				int full = (int) Math.floor(end);
				RenderUtils.rect(g, start, y + row, full, y + row + 1, col);
				float frac = end - full;
				if (frac > 0.02f) RenderUtils.rect(g, full, y + row, full + 1, y + row + 1, Colors.fade(col, frac));
			}
		}
		if (back) Icons.draw(g, Icon.BACK, x + Ui.px(28), y + th / 2, Ui.px(20), Theme.ON_ACCENT);
		UIFont.HEADER.drawMid(g, text, textX, y + th / 2, Theme.ON_ACCENT);
		if (back) hit(x, y, tw, th, false, this::closeSettings, null);
		return x + tw;
	}

	private void drawModsPage(Gfx g, int px, int py, int pw, int ph, double mx, double my) {
		float v = view.get();
		boolean inSettings = v > 0.5f && shown != null;
		int tabRight = drawTab(g, px, py, inSettings ? shown.name : "Mod Menu", inSettings, mx, my);
		int headMid = py + Ui.px(32);

		int contentX = px + Ui.px(18), contentY = py + Ui.px(82), contentW = pw - Ui.px(36), contentBottom = py + ph - Ui.px(18);

		if (!inSettings) {
			// Filter pills.
			int x = tabRight + Ui.px(16);
			int pillH = Ui.px(32);
			Category[] cats = Category.values();
			for (int i = 0; i <= cats.length; i++) {
				String label = i == 0 ? "All" : cats[i - 1].label;
				Category c = i == 0 ? null : cats[i - 1];
				int lw = UIFont.SMALL.width(label) + Ui.px(28);
				boolean active = search.isEmpty() && c == category;
				boolean hover = Widgets.inside(mx, my, x, headMid - pillH / 2, lw, pillH);
				if (active) RenderUtils.roundedRect(g, x, headMid - pillH / 2, lw, pillH, Ui.px(Theme.RADIUS), 0xFFF2F2F2);
				else if (hover) RenderUtils.roundedRect(g, x, headMid - pillH / 2, lw, pillH, Ui.px(Theme.RADIUS), Theme.HOVER);
				UIFont.SMALL.drawCentered(g, label, x + lw / 2, headMid - UIFont.SMALL.size() / 2,
						active ? 0xFF141416 : hover ? Theme.TEXT : Theme.TEXT_MUTED);
				final Category cat = c;
				hit(x, headMid - pillH / 2, lw, pillH, false, () -> {
					category = cat;
					search = "";
					searchFocused = false;
					scrollTarget = 0;
				}, null);
				x += lw + Ui.px(4);
			}
			drawSearch(g, px + pw - Ui.px(18) - Ui.px(230), headMid - Ui.px(21), Ui.px(230), Ui.px(42), mx, my);
		} else {
			UIFont.SMALL.drawMid(g, UIFont.SMALL.trim(shown.description, px + pw - tabRight - Ui.px(170)), tabRight + Ui.px(18), headMid, Theme.TEXT_MUTED);
			int rw = Ui.px(120), rh = Ui.px(38);
			int rx = px + pw - Ui.px(18) - rw, ry = headMid - rh / 2;
			Widgets.button(g, rx, ry, rw, rh, "Reset", UIFont.BODY, Theme.PANEL, Theme.TEXT, Widgets.inside(mx, my, rx, ry, rw, rh));
			hit(rx, ry, rw, rh, false, () -> {
				Module m = shown;
				m.resetToDefaults();
				openSettings(m);
				view.snap(1f);
			}, null);
		}

		// Cross-fade + slide between the grid and the settings page.
		float prev = RenderUtils.alpha;
		clip(g, contentX, contentY, contentW, contentBottom - contentY);
		if (v < 0.999f) {
			RenderUtils.alpha = prev * (1 - v);
			drawModList(g, contentX - Math.round(v * Ui.px(30)), contentY, contentW, contentBottom, mx, my, v < 0.5f);
		}
		if (v > 0.001f && shown != null) {
			RenderUtils.alpha = prev * v;
			drawSettingsList(g, contentX + Math.round((1 - v) * Ui.px(30)), contentY, contentW, contentBottom, mx, my);
		}
		g.endScissor();
		RenderUtils.alpha = prev;
	}

	private void drawSearch(Gfx g, int x, int y, int w, int h, double mx, double my) {
		boolean hover = Widgets.inside(mx, my, x, y, w, h);
		int b = Math.max(1, Ui.px(1));
		RenderUtils.roundedRect(g, x - b, y - b, w + b * 2, h + b * 2, Ui.px(Theme.RADIUS), searchFocused ? Theme.ACCENT : Theme.DIVIDER);
		RenderUtils.roundedRect(g, x, y, w, h, Ui.px(Theme.RADIUS), hover && !searchFocused ? Theme.HOVER : Theme.PANEL);
		Icons.draw(g, Icon.SEARCH, x + Ui.px(20), y + h / 2, Ui.px(16), Theme.TEXT_MUTED);
		int tx = x + Ui.px(38);
		if (search.isEmpty() && !searchFocused) {
			UIFont.SMALL.drawMid(g, "Search Mods", tx, y + h / 2, Theme.TEXT_MUTED);
		} else {
			String s = search;
			while (UIFont.SMALL.width(s) > w - Ui.px(50) && !s.isEmpty()) s = s.substring(1);
			UIFont.SMALL.drawMid(g, s, tx, y + h / 2, Theme.TEXT);
			if (searchFocused && System.currentTimeMillis() / 500 % 2 == 0) {
				int cx = tx + UIFont.SMALL.width(s) + 1;
				RenderUtils.rect(g, cx, y + h / 2 - Ui.px(9), cx + Math.max(1, Ui.px(2)), y + h / 2 + Ui.px(9), Theme.ACCENT);
			}
		}
		hit(x, y, w, h, false, () -> searchFocused = true, () -> {
			search = "";
			searchFocused = false;
		});
	}

	private void drawModList(Gfx g, int x, int y, int w, int bottom, double mx, double my, boolean interactive) {
		List<Module> mods = visibleModules();
		int gap = Ui.px(14);
		int cols = listView ? 1 : 3;
		int cardW = (w - gap * (cols - 1) - Ui.px(10)) / cols;
		int cardH = listView ? Ui.px(78) : Ui.px(190);
		int rows = (mods.size() + cols - 1) / cols;
		contentH = Math.max(0, rows * (cardH + gap) - gap);
		scrollTarget = clampScroll(scrollTarget, contentH, bottom - y);
		scroll += (scrollTarget - scroll) * scrollEase;
		if (Math.abs(scrollTarget - scroll) < 0.5) scroll = scrollTarget;

		boolean inArea = Widgets.inside(mx, my, clipX, clipY, clipW, clipH);
		for (int i = 0; i < mods.size(); i++) {
			int cx = x + (i % cols) * (cardW + gap);
			int cy = y + (i / cols) * (cardH + gap) - (int) Math.round(scroll);
			if (cy + cardH < y || cy > bottom) continue;
			Module m = mods.get(i);
			if (listView) drawRow(g, m, cx, cy, cardW, cardH, mx, my, inArea && interactive);
			else drawCard(g, m, cx, cy, cardW, cardH, mx, my, inArea && interactive);
		}
		if (mods.isEmpty()) {
			String msg = favoritesOnly ? "No favourites yet: click the heart on a mod" : "No mods match \"" + search + "\"";
			UIFont.BODY.drawCentered(g, msg, x + w / 2, y + Ui.px(40), Theme.TEXT_MUTED);
		}
		drawScrollbar(g, x + w - Ui.px(4), y, bottom - y, scroll, contentH);
	}

	private void drawCard(Gfx g, Module m, int x, int y, int w, int h, double mx, double my, boolean interactive) {
		boolean hover = interactive && Widgets.inside(mx, my, x, y, w, h);
		Animation ha = hoverAnim(m);
		ha.animateTo(hover && Performance.hoverEffects() ? 1f : 0f);
		RenderUtils.roundedRect(g, x, y, w, h, Ui.px(Theme.RADIUS), Colors.lerp(Theme.PANEL, Theme.HOVER, ha.get()));

		// Big line-art icon, dimmed while the mod is off.
		int iconCol = m.isEnabled() ? Theme.ICON : Theme.ICON_OFF;
		Icons.draw(g, m.icon(), x + w / 2, y + Ui.px(80), Ui.px(82), iconCol);

		// Favourite heart, top-right.
		int hs = Ui.px(20), hx = x + w - Ui.px(26), hy = y + Ui.px(24);
		boolean heartHover = hover && Widgets.inside(mx, my, hx - hs, hy - hs, hs * 2, hs * 2);
		Icons.draw(g, m.isFavorite() ? Icon.HEART_FILLED : Icon.HEART, hx, hy, hs,
				m.isFavorite() ? Theme.ACCENT : heartHover ? 0xFFFFFFFF : 0xFF77777C);

		// Name + switch along the bottom.
		int mid = y + h - Ui.px(30);
		int tw = Widgets.toggleW();
		if (!m.available()) {
			// This Minecraft version can't run it: greyed out, no switch.
			UIFont.TITLE.drawMid(g, UIFont.TITLE.trim(m.name, w - Ui.px(32)), x + Ui.px(16), mid - Ui.px(8), Theme.TEXT_MUTED);
			UIFont.SMALL.drawMid(g, "Not on this version", x + Ui.px(16), mid + Ui.px(12), 0xFF6E6E73);
			if (interactive) {
				hit(x, y, w, h, true, () -> openSettings(m), null);
				hit(hx - hs, hy - hs, hs * 2, hs * 2, true, () -> m.setFavorite(!m.isFavorite()), null);
			}
			return;
		}
		UIFont.TITLE.drawMid(g, UIFont.TITLE.trim(m.name, w - tw - Ui.px(44)), x + Ui.px(16), mid, Theme.TEXT);
		Animation ta = toggleAnim(m);
		ta.animateTo(m.isEnabled() ? 1f : 0f);
		int tx = x + w - Ui.px(16) - tw, ty = mid - Widgets.toggleH() / 2;
		Widgets.toggle(g, tx, ty, ta, hover && Widgets.inside(mx, my, tx - Ui.px(6), ty - Ui.px(8), tw + Ui.px(12), Widgets.toggleH() + Ui.px(16)));

		if (!interactive) return;
		// Order matters: registered later = checked first.
		hit(x, y, w, h, true, () -> openSettings(m), m::toggle);
		hit(hx - hs, hy - hs, hs * 2, hs * 2, true, () -> m.setFavorite(!m.isFavorite()), null);
		hit(tx - Ui.px(6), ty - Ui.px(8), tw + Ui.px(12), Widgets.toggleH() + Ui.px(16), true, m::toggle, m::toggle);
	}

	private void drawRow(Gfx g, Module m, int x, int y, int w, int h, double mx, double my, boolean interactive) {
		boolean hover = interactive && Widgets.inside(mx, my, x, y, w, h);
		Animation ha = hoverAnim(m);
		ha.animateTo(hover && Performance.hoverEffects() ? 1f : 0f);
		RenderUtils.roundedRect(g, x, y, w, h, Ui.px(Theme.RADIUS), Colors.lerp(Theme.PANEL, Theme.HOVER, ha.get()));
		Icons.draw(g, m.icon(), x + Ui.px(42), y + h / 2, Ui.px(40), m.isEnabled() ? Theme.ICON : Theme.ICON_OFF);

		int tw = Widgets.toggleW();
		int tx = x + w - Ui.px(18) - tw, ty = y + (h - Widgets.toggleH()) / 2;
		int hs = Ui.px(20), hx = tx - Ui.px(30), hy = y + h / 2;
		int textW = hx - hs - (x + Ui.px(80)) - Ui.px(10);
		UIFont.TITLE.draw(g, UIFont.TITLE.trim(m.name, textW), x + Ui.px(80), y + h / 2 - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
		UIFont.SMALL.draw(g, UIFont.SMALL.trim(m.available() ? m.description : "Not available on this Minecraft version", textW),
				x + Ui.px(80), y + h / 2 + Ui.px(4), Theme.TEXT_MUTED);
		boolean heartHover = hover && Widgets.inside(mx, my, hx - hs, hy - hs, hs * 2, hs * 2);
		Icons.draw(g, m.isFavorite() ? Icon.HEART_FILLED : Icon.HEART, hx, hy, hs,
				m.isFavorite() ? Theme.ACCENT : heartHover ? 0xFFFFFFFF : 0xFF77777C);
		if (!m.available()) {
			if (interactive) hit(hx - hs, hy - hs, hs * 2, hs * 2, true, () -> m.setFavorite(!m.isFavorite()), null);
			return;
		}
		Animation ta = toggleAnim(m);
		ta.animateTo(m.isEnabled() ? 1f : 0f);
		Widgets.toggle(g, tx, ty, ta, hover && Widgets.inside(mx, my, tx - Ui.px(6), y, tw + Ui.px(24), h));

		if (!interactive) return;
		hit(x, y, w, h, true, () -> openSettings(m), m::toggle);
		hit(hx - hs, hy - hs, hs * 2, hs * 2, true, () -> m.setFavorite(!m.isFavorite()), null);
		hit(tx - Ui.px(6), y, tw + Ui.px(24), h, true, m::toggle, m::toggle);
	}

	private void drawSettingsList(Gfx g, int x, int y, int w, int bottom, double mx, double my) {
		int total = 0;
		for (SettingComponent<?> c : components) if (c.visible()) total += c.height();
		setContentH = total;
		setScrollTarget = clampScroll(setScrollTarget, setContentH, bottom - y);
		setScroll += (setScrollTarget - setScroll) * scrollEase;
		if (Math.abs(setScrollTarget - setScroll) < 0.5) setScroll = setScrollTarget;

		boolean inArea = Widgets.inside(mx, my, clipX, clipY, clipW, clipH);
		int cy = y - (int) Math.round(setScroll);
		int rowW = w - Ui.px(12);
		for (SettingComponent<?> c : components) {
			if (!c.visible()) continue;
			int ch = c.height();
			if (cy + ch >= y && cy <= bottom) c.render(g, x, cy, rowW, mx, inArea ? my : -1e9);
			cy += ch;
		}
		drawScrollbar(g, x + w - Ui.px(4), y, bottom - y, setScroll, setContentH);
	}

	/** Sidebar "Performance": the switches that make the menus lighter, as a normal settings list. */
	private void openPerformance() {
		page = Page.PERFORMANCE;
		selected = null;
		view.snap(0f);
		components = new ArrayList<>();
		for (Setting<?> s : Performance.INSTANCE.settings()) components.add(SettingComponent.of(s));
		setScroll = setScrollTarget = 0;
	}

	/** Dev test hook. */
	public void devPerformance() {
		openPerformance();
	}

	private void drawPerformancePage(Gfx g, int px, int py, int pw, int ph, double mx, double my) {
		int tabRight = drawTab(g, px, py, "Performance", false, mx, my);
		UIFont.SMALL.drawMid(g, "Turn effects off to make the menus lighter on slow PCs", tabRight + Ui.px(18), py + Ui.px(32), Theme.TEXT_MUTED);
		int contentX = px + Ui.px(18), contentY = py + Ui.px(82), contentW = pw - Ui.px(36), contentBottom = py + ph - Ui.px(18);
		clip(g, contentX, contentY, contentW, contentBottom - contentY);
		drawSettingsList(g, contentX, contentY, contentW, contentBottom, mx, my);
		g.endScissor();
	}

	/** Sidebar "Settings": about the client, links and reset buttons. */
	/** Switch between the launcher's accounts without restarting the game. */
	private void drawAccountsPage(Gfx g, int px, int py, int pw, int ph, double mx, double my) {
		drawTab(g, px, py, "Accounts", false, mx, my);
		int x = px + Ui.px(18), w = pw - Ui.px(36);
		int y = py + Ui.px(86);

		// Who you are now.
		int ch = Ui.px(96);
		RenderUtils.roundedRect(g, x, y, w, ch, Ui.px(Theme.RADIUS), Theme.PANEL);
		int logo = Math.max(32, Ui.px(56) / 8 * 8);
		g.logo(x + Ui.px(20), y + (ch - logo) / 2, logo, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		int tx = x + Ui.px(20) + logo + Ui.px(20);
		UIFont.SMALL.draw(g, "PLAYING AS", tx, y + Ui.px(22), Theme.TEXT_MUTED);
		UIFont.HUGE.draw(g, AccountSwitcher.currentName(), tx, y + Ui.px(42), Theme.TEXT);
		if (AccountSwitcher.available()) {
			int rw = Ui.px(130), rh = Ui.px(40), rx = x + w - rw - Ui.px(20), ry = y + (ch - rh) / 2;
			boolean rHover = Widgets.inside(mx, my, rx, ry, rw, rh);
			Widgets.button(g, rx, ry, rw, rh, AccountSwitcher.loading() ? "Loading…" : "Refresh", UIFont.BODY, Theme.BACKGROUND, Theme.TEXT, rHover);
			hit(rx, ry, rw, rh, false, AccountSwitcher::refresh, null);
		}
		y += ch + Ui.px(16);

		if (!AccountSwitcher.available()) {
			UIFont.BODY.draw(g, "Start the game from the tatnat launcher to switch accounts here.", x, y, Theme.TEXT_MUTED);
			UIFont.SMALL.draw(g, "Accounts you add in the launcher show up in this list.", x, y + Ui.px(28), Theme.TEXT_MUTED);
			return;
		}
		String status = AccountSwitcher.status();
		if (!status.isEmpty()) {
			UIFont.BODY.draw(g, status, x, y, AccountSwitcher.statusError() ? 0xFFFF6B6B : Theme.ACCENT);
			y += Ui.px(32);
		}
		String current = AccountSwitcher.currentName();
		java.util.List<AccountSwitcher.Account> list = AccountSwitcher.accounts();
		if (list.isEmpty()) {
			UIFont.BODY.draw(g, AccountSwitcher.loading() ? "Loading accounts…" : "No accounts in the launcher yet.", x, y, Theme.TEXT_MUTED);
			return;
		}
		int rh = Ui.px(60), bottom = py + ph - Ui.px(18);
		for (AccountSwitcher.Account a : list) {
			if (y + rh > bottom) break;
			boolean isCurrent = a.name.equalsIgnoreCase(current);
			boolean hover = !isCurrent && Widgets.inside(mx, my, x, y, w, rh);
			RenderUtils.roundedRect(g, x, y, w, rh, Ui.px(Theme.RADIUS), hover ? Theme.HOVER : Theme.PANEL);
			Icons.draw(g, Icon.USER, x + Ui.px(30), y + rh / 2, Ui.px(26), isCurrent ? Theme.ACCENT : Theme.TEXT_MUTED);
			UIFont.TITLE.draw(g, a.name, x + Ui.px(58), y + rh / 2 - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
			UIFont.SMALL.draw(g, a.microsoft() ? "Microsoft account" : "Offline account (singleplayer and offline servers)",
					x + Ui.px(58), y + rh / 2 + Ui.px(4), Theme.TEXT_MUTED);
			int bw = Ui.px(140), bh = Ui.px(38), bx = x + w - bw - Ui.px(14), by = y + (rh - bh) / 2;
			if (isCurrent) {
				UIFont.BODY.drawRight(g, "In use", x + w - Ui.px(24), y + rh / 2 - UIFont.BODY.size() / 2, Theme.ACCENT);
			} else {
				boolean bHover = Widgets.inside(mx, my, bx, by, bw, bh);
				boolean busy = AccountSwitcher.switching();
				Widgets.button(g, bx, by, bw, bh, busy ? "…" : "Switch", UIFont.BODY,
						bHover && !busy ? Colors.shade(Theme.ACCENT, 1.15f) : Theme.ACCENT, 0xFFFFFFFF, bHover);
				hit(x, y, w, rh, false, () -> AccountSwitcher.switchTo(a), null);
			}
			y += rh + Ui.px(8);
		}
		UIFont.SMALL.draw(g, "Switching keeps the game open. On a server, leave and rejoin to play there as the new account.",
				x, Math.min(y + Ui.px(6), bottom - UIFont.SMALL.size()), Theme.TEXT_MUTED);
	}

	private void drawSettingsPage(Gfx g, int px, int py, int pw, int ph, double mx, double my) {
		drawTab(g, px, py, "Settings", false, mx, my);
		int x = px + Ui.px(18), w = pw - Ui.px(36);
		int y = py + Ui.px(86);

		// About card.
		int ch = Ui.px(150);
		RenderUtils.roundedRect(g, x, y, w, ch, Ui.px(Theme.RADIUS), Theme.PANEL);
		int logo = Math.max(32, Ui.px(96) / 8 * 8);
		g.logo(x + Ui.px(28), y + (ch - logo) / 2, logo, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		int tx = x + Ui.px(28) + logo + Ui.px(26);
		UIFont.HUGE.draw(g, "tatnat client", tx, y + Ui.px(30), Theme.TEXT);
		UIFont.BODY.draw(g, "Version " + TatnatClient.VERSION + "   ·   made by tatnat", tx, y + Ui.px(84), Theme.TEXT_MUTED);
		int yw = Ui.px(250), yh = Ui.px(40), yx = x + w - yw - Ui.px(24), yy = y + (ch - yh) / 2;
		boolean yHover = Widgets.inside(mx, my, yx, yy, yw, yh);
		RenderUtils.roundedRect(g, yx, yy, yw, yh, Ui.px(Theme.RADIUS), yHover ? Colors.shade(Theme.ACCENT, 1.15f) : Theme.ACCENT);
		Icons.draw(g, Icon.YOUTUBE, yx + Ui.px(26), yy + yh / 2, Ui.px(24), 0xFFFFFFFF);
		UIFont.BODY.drawMid(g, "Subscribe: @tatnatmc", yx + Ui.px(48), yy + yh / 2, 0xFFFFFFFF);
		hit(yx, yy, yw, yh, false, () -> TatnatClient.game().openUrl(YOUTUBE), null);
		y += ch + Ui.px(16);

		// Info + actions.
		String[][] rows = {
				{"Open the menu", "Right Shift (or the chevron above the menu to close it)"},
				{"Move your HUD", "HUD Editor in the sidebar: drag, scroll to resize, right-click for settings"},
				{"Toggle a mod quickly", "Right-click its card, or give it a Toggle Key in its settings"},
		};
		for (String[] r : rows) {
			int rh = Ui.px(56);
			RenderUtils.roundedRect(g, x, y, w, rh, Ui.px(Theme.RADIUS), Theme.PANEL);
			UIFont.TITLE.drawMid(g, r[0], x + Ui.px(18), y + rh / 2, Theme.TEXT);
			UIFont.SMALL.drawRight(g, r[1], x + w - Ui.px(18), y + rh / 2 - UIFont.SMALL.size() / 2, Theme.TEXT_MUTED);
			y += rh + Ui.px(8);
		}
		// Client options with a switch.
		{
			int rh = Ui.px(56);
			ClientOptions opt = ClientOptions.INSTANCE;
			RenderUtils.roundedRect(g, x, y, w, rh, Ui.px(Theme.RADIUS), Theme.PANEL);
			UIFont.TITLE.draw(g, opt.titleButton.name, x + Ui.px(18), y + rh / 2 - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
			UIFont.SMALL.draw(g, opt.titleButton.description, x + Ui.px(18), y + rh / 2 + Ui.px(4), Theme.TEXT_MUTED);
			int sw = Widgets.toggleW(), sx = x + w - Ui.px(18) - sw, sy = y + (rh - Widgets.toggleH()) / 2;
			titleButtonAnim.animateTo(opt.titleButton.on() ? 1f : 0f);
			Widgets.toggle(g, sx, sy, titleButtonAnim, Widgets.inside(mx, my, x, y, w, rh));
			hit(x, y, w, rh, false, () -> {
				opt.titleButton.set(!opt.titleButton.on());
				TatnatClient.CONFIG.markDirty();
			}, null);
			y += rh + Ui.px(8);
		}
		y += Ui.px(8);
		int bw = Ui.px(220), bh = Ui.px(44);
		boolean h1 = Widgets.inside(mx, my, x, y, bw, bh);
		Widgets.button(g, x, y, bw, bh, "Reset HUD layout", UIFont.BODY, Theme.PANEL, Theme.TEXT, h1);
		hit(x, y, bw, bh, false, () -> {
			for (HudModule m : ModuleManager.get().hud()) {
				m.resetPosition();
				m.scale.reset();
			}
			TatnatClient.CONFIG.markDirty();
		}, null);
		boolean confirming = System.currentTimeMillis() - confirmResetAll < 3000;
		int bx2 = x + bw + Ui.px(12);
		boolean h2 = Widgets.inside(mx, my, bx2, y, bw, bh);
		Widgets.button(g, bx2, y, bw, bh, confirming ? "Click again to confirm" : "Reset all mods", UIFont.BODY,
				confirming ? Theme.ACCENT : Theme.PANEL, Theme.TEXT, h2);
		hit(bx2, y, bw, bh, false, () -> {
			if (System.currentTimeMillis() - confirmResetAll < 3000) {
				for (Module m : ModuleManager.get().all()) m.resetToDefaults();
				confirmResetAll = 0;
			} else {
				confirmResetAll = System.currentTimeMillis();
			}
		}, null);
	}

	private void drawScrollbar(Gfx g, int x, int top, int height, double scroll, int content) {
		if (content <= height) return;
		int barH = Math.max(Ui.px(30), height * height / content);
		int barY = top + (int) Math.round((height - barH) * (scroll / (content - height)));
		RenderUtils.roundedRect(g, x, barY, Math.max(2, Ui.px(4)), barH, Math.max(1, Ui.px(2)), 0x50FFFFFF);
	}

	private void clip(Gfx g, int x, int y, int w, int h) {
		clipX = x;
		clipY = y;
		clipW = w;
		clipH = h;
		g.scissor(x, y, x + w, y + h);
	}

	private void hit(int x, int y, int w, int h, boolean clipped, Runnable left, Runnable right) {
		hits.add(new Hit(x, y, w, h, clipped, left, right));
	}

	private static double clampScroll(double v, int content, int view) {
		return Math.max(0, Math.min(v, Math.max(0, content - view)));
	}

	// ---------------------------------------------------------------- input (GUI coords -> pixels)

	/** True while a settings list (a mod's, or the Performance page) takes the input. */
	private boolean settingsVisible() {
		return page == Page.PERFORMANCE || page == Page.MODS && selected != null && view.target() > 0;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (closing) return true;

		// A key listener / text field that is capturing gets first go (it may bind mouse buttons).
		if (settingsVisible()) {
			for (SettingComponent<?> c : components) {
				if (c.isCapturingKeys() && c.mouseClicked(mx, my, button)) return true;
			}
		}
		boolean wasSearch = searchFocused;
		searchFocused = false;

		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (!Widgets.inside(mx, my, h.x, h.y, h.w, h.h)) continue;
			if (h.clipped && !Widgets.inside(mx, my, clipX, clipY, clipW, clipH)) continue;
			Runnable r = button == 0 ? h.left : button == 1 ? h.right : null;
			if (r != null) {
				r.run();
				return true;
			}
		}
		if (settingsVisible() && Widgets.inside(mx, my, clipX, clipY, clipW, clipH)) {
			for (SettingComponent<?> c : components) {
				if (c.visible() && c.mouseClicked(mx, my, button)) return true;
			}
		}
		if (wasSearch && search.isEmpty()) searchFocused = false;
		return true;
	}

	@Override
	public void mouseReleased(double mx, double my, int button) {
		for (SettingComponent<?> c : components) c.mouseReleased(mx, my, button);
	}

	@Override
	public void mouseDragged(double mx, double my, int button) {
		if (settingsVisible()) for (SettingComponent<?> c : components) c.mouseDragged(mx, my);
	}

	@Override
	public void mouseScrolled(double mx, double my, double amount) {
		double step = Ui.px(90) * amount;
		if (settingsVisible()) setScrollTarget = clampScroll(setScrollTarget - step, setContentH, clipH);
		else scrollTarget = clampScroll(scrollTarget - step, contentH, clipH);
	}

	@Override
	public boolean keyPressed(int key, boolean shift) {
		if (settingsVisible()) {
			for (SettingComponent<?> c : components) {
				if (c.isCapturingKeys() && c.keyPressed(key)) return true;
			}
		}
		if (searchFocused) {
			if (key == KeyCodes.BACKSPACE && !search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				scrollTarget = 0;
				return true;
			}
			if (key == KeyCodes.ESCAPE || key == KeyCodes.ENTER) {
				searchFocused = false;
				return true;
			}
			if (key != TatnatClient.MENU_KEY) return true;
		}
		if (key == KeyCodes.ESCAPE && page == Page.MODS && settingsVisible()) {
			closeSettings();
			return true;
		}
		if (key == KeyCodes.ESCAPE || key == TatnatClient.MENU_KEY) {
			close();
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(String chars) {
		if (settingsVisible()) {
			for (SettingComponent<?> c : components) {
				if (c.isCapturingKeys() && c.charTyped(chars)) return true;
			}
		}
		// Start typing anywhere on the mod grid to search, like Feather.
		if (page != Page.MODS || settingsVisible()) return false;
		if (!searchFocused && !chars.isEmpty() && Character.isLetterOrDigit(chars.codePointAt(0))) searchFocused = true;
		if (searchFocused && search.length() < 32) {
			search += chars;
			scrollTarget = 0;
			return true;
		}
		return false;
	}
}
