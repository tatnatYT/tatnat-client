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
 * The Right Shift mod menu: one window with a sidebar (brand, navigation with a sliding
 * highlight, your account) and a content area (page title, search, header actions, category
 * tabs and a grid or list of mod cards). Clicking a card opens that mod's settings in place.
 * Everything is drawn in real pixels, laid out for 1920x1080 and scaled by {@link Ui}. The
 * window fades in and rises, cards cascade in, and toggles, hovers and indicators all glide.
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

	// Window layout in design pixels (1920x1080 reference, scaled by Ui).
	private static final int WIN_W = 1160, WIN_H = 712, RAIL_W = 236, PAD = 32;

	/** When the menu opened (cards fade in one after another from here). */
	private final long openedAt = System.currentTimeMillis();
	/** Sliding indicators: the sidebar highlight and the category pill. */
	private float navY = -1, pillX = -1, pillW;
	private double dt = 1 / 60.0;

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
		dt = lastFrame == 0 ? 1 / 60.0 : Math.min(0.1, (now - lastFrame) / 1e9);
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
		// Dim the world so the window stands out (darker towards the bottom).
		RenderUtils.verticalGradient(g, 0, 0, W, H, 0x66050508, 0xB0050508);

		int ww = Ui.px(WIN_W), wh = Ui.px(WIN_H);
		int wx = (W - ww) / 2, wy = (H - wh) / 2 + Math.round((1 - o) * Ui.px(14));
		int wr = Ui.px(Theme.RADIUS_WINDOW);
		RenderUtils.shadow(g, wx, wy, ww, wh, wr, 14, 0.6f);
		RenderUtils.roundedGradient(g, wx, wy, ww, wh, wr, Theme.WINDOW_TOP, Theme.WINDOW_BOTTOM);
		// A faint red light in the top-left corner, behind the brand.
		RenderUtils.glow(g, wx + Ui.px(120), wy + Ui.px(60), Ui.px(220), 0x18E5323E, 8);
		RenderUtils.roundedOutline(g, wx, wy, ww, wh, wr, 1, Theme.BORDER);
		RenderUtils.rect(g, wx + wr, wy + 1, wx + ww - wr, wy + 2, 0x14FFFFFF);

		drawRail(g, wx, wy, Ui.px(RAIL_W), wh, mx, my);

		int cx = wx + Ui.px(RAIL_W) + Ui.px(PAD), cw = ww - Ui.px(RAIL_W) - Ui.px(PAD) * 2;
		int top = wy + Ui.px(28), bottom = wy + wh - Ui.px(24);
		if (page == Page.MODS) drawModsPage(g, cx, top, cw, bottom, mx, my);
		else if (page == Page.PERFORMANCE) drawPerformancePage(g, cx, top, cw, bottom, mx, my);
		else if (page == Page.ACCOUNTS) drawAccountsPage(g, cx, top, cw, bottom, mx, my);
		else drawSettingsPage(g, cx, top, cw, bottom, mx, my);

		RenderUtils.end(g);
		RenderUtils.alpha = 1f;
	}

	/** Eases {@code from} towards {@code to}, time-based (same feel at any frame rate). */
	private float glide(float from, float to) {
		if (from < 0 || !Performance.animations()) return to;
		float k = (float) (1 - Math.exp(-dt * 16));
		float v = from + (to - from) * k;
		return Math.abs(to - v) < 0.5f ? to : v;
	}

	/** The sidebar: brand, navigation with a sliding highlight, and who you're playing as. */
	private void drawRail(Gfx g, int x, int y, int w, int h, double mx, double my) {
		int r = Ui.px(Theme.RADIUS_WINDOW);
		RenderUtils.roundedRect(g, x, y, w, h, r, Theme.RAIL, true, false, true, false);
		RenderUtils.rect(g, x + w - 1, y + Ui.px(20), x + w, y + h - Ui.px(20), Theme.BORDER);

		// Brand: the player's head (a multiple of 8 so the pixels stay square) and the wordmark.
		int logo = Math.max(24, Ui.px(44) / 8 * 8);
		int lx = x + Ui.px(24), ly = y + Ui.px(30);
		RenderUtils.glow(g, lx + logo / 2, ly + logo / 2, logo, 0x30E5323E, 6);
		RenderUtils.roundedRect(g, lx - Ui.px(3), ly - Ui.px(3), logo + Ui.px(6), logo + Ui.px(6), Ui.px(8), 0x30FFFFFF);
		g.logo(lx, ly, logo, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		int tx = lx + logo + Ui.px(14);
		UIFont.HEADER.draw(g, "Eclipse", tx, ly - Ui.px(2), Theme.TEXT);
		UIFont.TINY.draw(g, "C L I E N T", tx + Ui.px(1), ly + Ui.px(28), Theme.ACCENT);

		UIFont.TINY.draw(g, "MENU", x + Ui.px(28), y + Ui.px(118), 0xFF6C6E76);
		String[] labels = {"Mods", "HUD Editor", "Performance", "Settings", "Accounts"};
		Icon[] icons = {Icon.GRID, Icon.MOVE, Icon.CHIP, Icon.GEAR, Icon.USER};
		int bx = x + Ui.px(14), bw = w - Ui.px(28), bh = Ui.px(46), step = bh + Ui.px(6), first = y + Ui.px(140);
		int active = page == Page.MODS ? 0 : page == Page.PERFORMANCE ? 2 : page == Page.SETTINGS ? 3 : 4;
		navY = glide(navY, first + active * step);
		int ny = Math.round(navY);
		RenderUtils.roundedRect(g, bx, ny, bw, bh, Ui.px(Theme.RADIUS_LARGE), Theme.ACCENT_SOFT);
		RenderUtils.roundedRect(g, bx, ny + bh / 2 - Ui.px(11), Math.max(2, Ui.px(4)), Ui.px(22), Math.max(1, Ui.px(2)), Theme.ACCENT);
		for (int i = 0; i < labels.length; i++) {
			int by = first + i * step;
			boolean on = i == active;
			boolean hover = !on && Performance.hoverEffects() && Widgets.inside(mx, my, bx, by, bw, bh);
			if (hover) RenderUtils.roundedRect(g, bx, by, bw, bh, Ui.px(Theme.RADIUS_LARGE), 0x0DFFFFFF);
			int col = on ? 0xFFFFFFFF : hover ? Theme.TEXT : Theme.TEXT_MUTED;
			Icons.draw(g, icons[i], bx + Ui.px(28), by + bh / 2, Ui.px(20), on ? Theme.ACCENT : col);
			UIFont.BODY.drawMid(g, labels[i], bx + Ui.px(52), by + bh / 2, col);
			if (i == 0) {
				String n = String.valueOf(enabledCount());
				int pw = UIFont.TINY.width(n) + Ui.px(14), ph = Ui.px(20);
				int px = bx + bw - pw - Ui.px(12), py = by + (bh - ph) / 2;
				RenderUtils.roundedRect(g, px, py, pw, ph, ph / 2, on ? Theme.ACCENT : 0x1AFFFFFF);
				UIFont.TINY.drawCentered(g, n, px + pw / 2, py + (ph - UIFont.TINY.size()) / 2, 0xFFFFFFFF);
			}
			final int idx = i;
			hit(bx, by, bw, bh, false, () -> nav(idx), null);
		}

		// Who you're playing as, at the bottom.
		int ch = Ui.px(64), cy = y + h - ch - Ui.px(16), cxl = x + Ui.px(14), cwid = w - Ui.px(28);
		boolean cHover = Widgets.inside(mx, my, cxl, cy, cwid, ch);
		RenderUtils.surface(g, cxl, cy, cwid, ch, Ui.px(Theme.RADIUS_LARGE), cHover ? Theme.CARD_HOVER_TOP : Theme.CARD_TOP,
				cHover ? Theme.CARD_HOVER_BOTTOM : Theme.CARD_BOTTOM, cHover ? Theme.BORDER_HOVER : Theme.BORDER);
		int head = Math.max(16, Ui.px(32) / 8 * 8);
		g.logo(cxl + Ui.px(14), cy + (ch - head) / 2, head, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		int nx = cxl + Ui.px(14) + head + Ui.px(12), nw = cwid - (nx - cxl) - Ui.px(10);
		UIFont.BODY.draw(g, UIFont.BODY.trim(AccountSwitcher.currentName(), nw), nx, cy + ch / 2 - UIFont.BODY.size() + Ui.px(1), Theme.TEXT);
		RenderUtils.circle(g, nx + Ui.px(4), cy + ch / 2 + Ui.px(11), Math.max(2, Ui.px(4)), 0xFF3DDC84);
		UIFont.SMALL.draw(g, "v" + TatnatClient.VERSION, nx + Ui.px(14), cy + ch / 2 + Ui.px(3), Theme.TEXT_MUTED);
		hit(cxl, cy, cwid, ch, false, () -> nav(4), null);
	}

	private void nav(int idx) {
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
	}

	private static int enabledCount() {
		int n = 0;
		for (Module m : ModuleManager.get().all()) if (m.isEnabled()) n++;
		return n;
	}

	/** Page title and a muted line under it. Returns the x where the title block ends. */
	private int drawTitle(Gfx g, int x, int y, String title, String subtitle, int maxW) {
		UIFont.HEADER.draw(g, UIFont.HEADER.trim(title, maxW), x, y, Theme.TEXT);
		if (subtitle != null) UIFont.SMALL.draw(g, UIFont.SMALL.trim(subtitle, maxW), x, y + Ui.px(34), Theme.TEXT_MUTED);
		return x + Math.min(maxW, UIFont.HEADER.width(title));
	}

	/** The close button in the window's top-right corner. Returns its left edge. */
	private int drawClose(Gfx g, int right, int y, double mx, double my) {
		int s = Ui.px(40), x = right - s;
		boolean hover = Widgets.inside(mx, my, x, y, s, s);
		Widgets.iconButton(g, x, y, s, Icon.CLOSE, hover, false, 0);
		hit(x, y, s, s, false, this::close, null);
		return x;
	}

	private void drawModsPage(Gfx g, int x, int top, int w, int bottom, double mx, double my) {
		float v = view.get();
		boolean inSettings = v > 0.5f && shown != null;
		int right = x + w;
		int bs = Ui.px(40), gap = Ui.px(8);
		int left = drawClose(g, right, top, mx, my) - gap;

		if (!inSettings) {
			// Header actions: HUD editor, grid / list, favourites; then the search box.
			Icon[] icons = {Icon.MOVE, listView ? Icon.GRID : Icon.LIST, favoritesOnly ? Icon.HEART_FILLED : Icon.HEART};
			for (int i = 0; i < icons.length; i++) {
				int bx = left - bs;
				boolean hover = Widgets.inside(mx, my, bx, top, bs, bs);
				Widgets.iconButton(g, bx, top, bs, icons[i], hover, i == 2 && favoritesOnly, Theme.ACCENT);
				final int idx = i;
				hit(bx, top, bs, bs, false, () -> {
					if (idx == 0) {
						TatnatClient.game().openScreen(new HudEditorScreen(this));
					} else if (idx == 1) {
						listView = !listView;
					} else {
						favoritesOnly = !favoritesOnly;
						scrollTarget = 0;
					}
				}, null);
				left = bx - gap;
			}
			int sw = Ui.px(250);
			drawSearch(g, left - sw - Ui.px(4), top, sw, bs, mx, my);
			int n = ModuleManager.get().all().size();
			drawTitle(g, x, top - Ui.px(2), favoritesOnly ? "Favourites" : "Mods", enabledCount() + " of " + n + " enabled", left - sw - x - Ui.px(24));
			drawCategories(g, x, top + Ui.px(66), w, mx, my);
		} else {
			// Back button, the mod's icon, name and description; Reset on the right.
			boolean bHover = Widgets.inside(mx, my, x, top, bs, bs);
			Widgets.iconButton(g, x, top, bs, Icon.BACK, bHover, false, 0);
			hit(x, top, bs, bs, false, this::closeSettings, null);
			int ix = x + bs + Ui.px(14);
			int tile = Ui.px(44);
			RenderUtils.roundedRect(g, ix, top - Ui.px(2), tile, tile, Ui.px(Theme.RADIUS_LARGE), shown.isEnabled() ? Theme.ACCENT_SOFT : 0x10FFFFFF);
			Icons.draw(g, shown.icon(), ix + tile / 2, top - Ui.px(2) + tile / 2, Ui.px(26), shown.isEnabled() ? Theme.ICON : Theme.ICON_OFF);
			int rw = Ui.px(110);
			int rx = left - rw;
			Widgets.button(g, rx, top, rw, bs, "Reset", UIFont.BODY, 0xFF22242A, Theme.TEXT, Widgets.inside(mx, my, rx, top, rw, bs));
			hit(rx, top, rw, bs, false, () -> {
				Module m = shown;
				m.resetToDefaults();
				openSettings(m);
				view.snap(1f);
			}, null);
			int tx = ix + tile + Ui.px(16);
			drawTitle(g, tx, top - Ui.px(2), shown.name, shown.available() ? shown.description : "Not available on this Minecraft version", rx - tx - Ui.px(20));
		}

		int contentY = top + (inSettings ? Ui.px(68) : Ui.px(124));
		// Cross-fade + slide between the grid and the settings page.
		float prev = RenderUtils.alpha;
		clip(g, x - Ui.px(8), contentY, w + Ui.px(16), bottom - contentY);
		if (v < 0.999f) {
			RenderUtils.alpha = prev * (1 - v);
			drawModList(g, x - Math.round(v * Ui.px(30)), contentY, w, bottom, mx, my, v < 0.5f);
		}
		if (v > 0.001f && shown != null) {
			RenderUtils.alpha = prev * v;
			drawSettingsList(g, x + Math.round((1 - v) * Ui.px(30)), contentY, w, bottom, mx, my);
		}
		g.endScissor();
		RenderUtils.alpha = prev;
		// Fade the list out under the header and at the bottom edge.
		RenderUtils.verticalGradient(g, x - Ui.px(8), bottom - Ui.px(18), x + w + Ui.px(8), bottom, 0x000D0E11, Colors.withAlpha(Theme.WINDOW_BOTTOM, 0xF0));
	}

	/** Segmented category control with a highlight that slides to the selected one. */
	private void drawCategories(Gfx g, int x, int y, int maxW, double mx, double my) {
		int h = Ui.px(38), pad = Ui.px(4);
		Category[] cats = Category.values();
		int[] counts = new int[cats.length];
		for (Module m : ModuleManager.get().all()) counts[m.category.ordinal()]++;
		int n = cats.length + 1;
		String[] labels = new String[n], nums = new String[n];
		int[] widths = new int[n];
		int total = pad * 2;
		for (int i = 0; i < n; i++) {
			labels[i] = i == 0 ? "All" : cats[i - 1].label;
			nums[i] = String.valueOf(i == 0 ? ModuleManager.get().all().size() : counts[i - 1]);
			widths[i] = UIFont.SMALL.width(labels[i]) + Ui.px(8) + UIFont.TINY.width(nums[i]) + Ui.px(32);
			total += widths[i];
		}
		RenderUtils.roundedRect(g, x, y, Math.min(maxW, total), h, h / 2, 0x0FFFFFFF);
		RenderUtils.roundedOutline(g, x, y, Math.min(maxW, total), h, h / 2, 1, 0x10FFFFFF);

		int activeIdx = search.isEmpty() ? (category == null ? 0 : category.ordinal() + 1) : -1;
		int px = x + pad;
		int ax = px, aw = widths[0];
		for (int i = 0; i < n; i++) {
			if (i == activeIdx) {
				ax = px;
				aw = widths[i];
			}
			px += widths[i];
		}
		if (activeIdx >= 0) {
			pillX = glide(pillX, ax);
			pillW = pillW <= 0 ? aw : glide(pillW, aw);
			int ih = h - pad * 2;
			RenderUtils.surface(g, Math.round(pillX), y + pad, Math.round(pillW), ih, ih / 2, Theme.ACCENT_LIGHT, Theme.ACCENT, 0x40FFFFFF);
		}
		px = x + pad;
		for (int i = 0; i < n; i++) {
			int iw = widths[i], ih = h - pad * 2;
			boolean on = i == activeIdx;
			boolean hover = !on && Widgets.inside(mx, my, px, y + pad, iw, ih);
			if (hover) RenderUtils.roundedRect(g, px, y + pad, iw, ih, ih / 2, 0x0DFFFFFF);
			int col = on ? 0xFFFFFFFF : hover ? Theme.TEXT : Theme.TEXT_MUTED;
			int lx = px + Ui.px(16);
			UIFont.SMALL.drawMid(g, labels[i], lx, y + h / 2, col);
			UIFont.TINY.drawMid(g, nums[i], lx + UIFont.SMALL.width(labels[i]) + Ui.px(8), y + h / 2, on ? 0xC0FFFFFF : 0xFF6C6E76);
			final Category cat = i == 0 ? null : cats[i - 1];
			hit(px, y + pad, iw, ih, false, () -> {
				category = cat;
				search = "";
				searchFocused = false;
				scrollTarget = 0;
			}, null);
			px += iw;
		}
	}

	private void drawSearch(Gfx g, int x, int y, int w, int h, double mx, double my) {
		boolean hover = Widgets.inside(mx, my, x, y, w, h);
		int r = h / 2;
		RenderUtils.surface(g, x, y, w, h, r, hover || searchFocused ? 0xFF22242B : 0xFF1B1D22, 0xFF16171B,
				searchFocused ? Colors.withAlpha(Theme.ACCENT, 0xB0) : hover ? Theme.BORDER_HOVER : Theme.BORDER);
		Icons.draw(g, Icon.SEARCH, x + Ui.px(22), y + h / 2, Ui.px(16), searchFocused ? Theme.ACCENT : Theme.TEXT_MUTED);
		int tx = x + Ui.px(42);
		if (search.isEmpty() && !searchFocused) {
			UIFont.SMALL.drawMid(g, "Search mods...", tx, y + h / 2, 0xFF7A7C84);
		} else {
			String s = search;
			while (UIFont.SMALL.width(s) > w - Ui.px(60) && !s.isEmpty()) s = s.substring(1);
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

	/** 0 -> 1 as card {@code i} fades in after the menu opens (a quick cascade). */
	private float appear(int i) {
		if (!Performance.animations()) return 1f;
		long t = System.currentTimeMillis() - openedAt - Math.min(i, 14) * 22L;
		float k = Math.max(0f, Math.min(1f, t / 260f));
		return 1 - (1 - k) * (1 - k) * (1 - k);
	}

	private void drawModList(Gfx g, int x, int y, int w, int bottom, double mx, double my, boolean interactive) {
		List<Module> mods = visibleModules();
		int gap = Ui.px(16);
		int cols = listView ? 1 : 3;
		int cardW = (w - gap * (cols - 1) - Ui.px(12)) / cols;
		int cardH = listView ? Ui.px(76) : Ui.px(188);
		int rows = (mods.size() + cols - 1) / cols;
		int pad = Ui.px(4);
		contentH = Math.max(0, rows * (cardH + gap) - gap + pad * 2);
		scrollTarget = clampScroll(scrollTarget, contentH, bottom - y);
		scroll += (scrollTarget - scroll) * scrollEase;
		if (Math.abs(scrollTarget - scroll) < 0.5) scroll = scrollTarget;

		boolean inArea = Widgets.inside(mx, my, clipX, clipY, clipW, clipH);
		float base = RenderUtils.alpha;
		for (int i = 0; i < mods.size(); i++) {
			int cx = x + (i % cols) * (cardW + gap);
			int cy = y + pad + (i / cols) * (cardH + gap) - (int) Math.round(scroll);
			if (cy + cardH < y || cy > bottom) continue;
			float a = appear(i);
			RenderUtils.alpha = base * a;
			cy += Math.round((1 - a) * Ui.px(16));
			Module m = mods.get(i);
			if (listView) drawRow(g, m, cx, cy, cardW, cardH, mx, my, inArea && interactive);
			else drawCard(g, m, cx, cy, cardW, cardH, mx, my, inArea && interactive);
		}
		RenderUtils.alpha = base;
		if (mods.isEmpty()) {
			int my0 = y + Ui.px(90);
			Icons.draw(g, favoritesOnly ? Icon.HEART : Icon.SEARCH, x + w / 2, my0, Ui.px(48), 0xFF4A4C54);
			String msg = favoritesOnly ? "No favourites yet" : "No mods match \"" + search + "\"";
			String sub = favoritesOnly ? "Click the heart on a mod to pin it here" : "Try a shorter or different word";
			UIFont.TITLE.drawCentered(g, msg, x + w / 2, my0 + Ui.px(44), Theme.TEXT);
			UIFont.SMALL.drawCentered(g, sub, x + w / 2, my0 + Ui.px(72), Theme.TEXT_MUTED);
		}
		drawScrollbar(g, x + w - Ui.px(4), y, bottom - y, scroll, contentH);
	}

	/** Small rounded label (the card's category). */
	private static int chip(Gfx g, String text, int x, int y, int bg, int fg) {
		int h = Ui.px(20), w = UIFont.TINY.width(text) + Ui.px(14);
		RenderUtils.roundedRect(g, x, y, w, h, h / 2, bg);
		UIFont.TINY.drawCentered(g, text, x + w / 2, y + (h - UIFont.TINY.size()) / 2, fg);
		return w;
	}

	private void drawCard(Gfx g, Module m, int x, int y, int w, int h, double mx, double my, boolean interactive) {
		boolean hover = interactive && Widgets.inside(mx, my, x, y, w, h);
		Animation ha = hoverAnim(m);
		ha.animateTo(hover && Performance.hoverEffects() ? 1f : 0f);
		float hv = ha.get();
		Animation ta = toggleAnim(m);
		ta.animateTo(m.isEnabled() ? 1f : 0f);
		float on = ta.get();
		y -= Math.round(hv * Ui.px(3)); // lift on hover
		int r = Ui.px(Theme.RADIUS_CARD);

		if (hv > 0.01f) RenderUtils.shadow(g, x, y, w, h, r, 6, 0.35f * hv);
		int border = Colors.lerp(Colors.lerp(Theme.BORDER, Theme.BORDER_HOVER, hv), 0x80E5323E, on * 0.75f);
		// Enabled: the card is lit red from the top, with a glow behind the icon.
		RenderUtils.surface(g, x, y, w, h, r, Colors.lerp(Colors.lerp(Theme.CARD_TOP, Theme.CARD_HOVER_TOP, hv), Theme.CARD_ON_TOP, on * 0.9f),
				Colors.lerp(Colors.lerp(Theme.CARD_BOTTOM, Theme.CARD_HOVER_BOTTOM, hv), Theme.CARD_ON_BOTTOM, on * 0.6f), border);
		int iconY = y + Ui.px(74);
		if (on > 0.01f) RenderUtils.glow(g, x + w / 2, iconY, Ui.px(58), Colors.fade(0x50E5323E, on), 8);
		int iconCol = Colors.lerp(Theme.ICON_OFF, Theme.ICON, on);
		if (!m.available()) iconCol = 0xFF3A3C42;
		Icons.draw(g, m.icon(), x + w / 2, iconY, Ui.px(62), iconCol);

		chip(g, m.category.label.toUpperCase(Locale.ROOT), x + Ui.px(14), y + Ui.px(14), 0x12FFFFFF, 0xFF8A8C94);

		// Favourite heart, top-right: always shown when set, otherwise only on hover.
		int hs = Ui.px(18), hx = x + w - Ui.px(26), hy = y + Ui.px(24);
		boolean heartHover = hover && Widgets.inside(mx, my, hx - hs, hy - hs, hs * 2, hs * 2);
		if (m.isFavorite() || hv > 0.01f) {
			float prev = RenderUtils.alpha;
			if (!m.isFavorite()) RenderUtils.alpha = prev * hv;
			Icons.draw(g, m.isFavorite() ? Icon.HEART_FILLED : Icon.HEART, hx, hy, hs,
					m.isFavorite() ? Theme.ACCENT : heartHover ? 0xFFFFFFFF : 0xFF8A8C94);
			RenderUtils.alpha = prev;
		}

		// Divider, then name + status on the left and the switch on the right.
		RenderUtils.rect(g, x + Ui.px(16), y + h - Ui.px(66), x + w - Ui.px(16), y + h - Ui.px(65), 0x0DFFFFFF);
		int mid = y + h - Ui.px(33);
		int tw = Widgets.toggleW();
		int textW = w - tw - Ui.px(48);
		UIFont.TITLE.draw(g, UIFont.TITLE.trim(m.name, textW), x + Ui.px(18), mid - UIFont.TITLE.size() + Ui.px(1), m.available() ? Theme.TEXT : Theme.TEXT_MUTED);
		int sy = mid + Ui.px(5);
		if (!m.available()) {
			UIFont.SMALL.draw(g, "Not on this version", x + Ui.px(18), sy, 0xFF6E6E73);
			if (interactive) {
				hit(x, y, w, h, true, () -> openSettings(m), null);
				hit(hx - hs, hy - hs, hs * 2, hs * 2, true, () -> m.setFavorite(!m.isFavorite()), null);
			}
			return;
		}
		int dot = Math.max(2, Ui.px(4));
		RenderUtils.circle(g, x + Ui.px(18) + dot, sy + UIFont.SMALL.size() / 2, dot, m.isEnabled() ? Theme.ACCENT : 0xFF55575F);
		UIFont.SMALL.draw(g, m.isEnabled() ? "Enabled" : "Disabled", x + Ui.px(18) + dot * 2 + Ui.px(8), sy, m.isEnabled() ? 0xFFF07B83 : 0xFF7A7C84);

		int tx = x + w - Ui.px(18) - tw, ty = mid - Widgets.toggleH() / 2;
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
		float hv = ha.get();
		Animation ta = toggleAnim(m);
		ta.animateTo(m.isEnabled() ? 1f : 0f);
		float on = ta.get();
		int r = Ui.px(Theme.RADIUS_CARD);
		int border = Colors.lerp(Colors.lerp(Theme.BORDER, Theme.BORDER_HOVER, hv), 0x80E5323E, on * 0.6f);
		RenderUtils.surface(g, x, y, w, h, r, Colors.lerp(Colors.lerp(Theme.CARD_TOP, Theme.CARD_HOVER_TOP, hv), Theme.CARD_ON_TOP, on * 0.6f),
				Colors.lerp(Colors.lerp(Theme.CARD_BOTTOM, Theme.CARD_HOVER_BOTTOM, hv), Theme.CARD_ON_BOTTOM, on * 0.4f), border);

		// Icon tile.
		int tile = Ui.px(48), tlx = x + Ui.px(14), tly = y + (h - tile) / 2;
		if (on > 0.01f) RenderUtils.glow(g, tlx + tile / 2, tly + tile / 2, Ui.px(34), Colors.fade(0x40E5323E, on), 6);
		RenderUtils.roundedRect(g, tlx, tly, tile, tile, Ui.px(Theme.RADIUS_LARGE), Colors.lerp(0x10FFFFFF, Theme.ACCENT_SOFT, on));
		Icons.draw(g, m.icon(), tlx + tile / 2, tly + tile / 2, Ui.px(28), m.available() ? Colors.lerp(Theme.ICON_OFF, Theme.ICON, on) : 0xFF3A3C42);

		int tw = Widgets.toggleW();
		int tx = x + w - Ui.px(20) - tw, ty = y + (h - Widgets.toggleH()) / 2;
		int hs = Ui.px(18), hx = tx - Ui.px(34), hy = y + h / 2;
		int textX = tlx + tile + Ui.px(16);
		String cat = m.category.label.toUpperCase(Locale.ROOT);
		int chipW = UIFont.TINY.width(cat) + Ui.px(14);
		int textW = hx - hs - textX - chipW - Ui.px(28);
		UIFont.TITLE.draw(g, UIFont.TITLE.trim(m.name, textW), textX, y + h / 2 - UIFont.TITLE.size() + Ui.px(1), m.available() ? Theme.TEXT : Theme.TEXT_MUTED);
		UIFont.SMALL.draw(g, UIFont.SMALL.trim(m.available() ? m.description : "Not available on this Minecraft version", textW + chipW),
				textX, y + h / 2 + Ui.px(5), Theme.TEXT_MUTED);
		chip(g, cat, hx - hs - Ui.px(14) - chipW, y + (h - Ui.px(20)) / 2, 0x12FFFFFF, 0xFF8A8C94);
		boolean heartHover = hover && Widgets.inside(mx, my, hx - hs, hy - hs, hs * 2, hs * 2);
		Icons.draw(g, m.isFavorite() ? Icon.HEART_FILLED : Icon.HEART, hx, hy, hs,
				m.isFavorite() ? Theme.ACCENT : heartHover ? 0xFFFFFFFF : 0xFF6C6E76);
		if (!m.available()) {
			if (interactive) hit(hx - hs, hy - hs, hs * 2, hs * 2, true, () -> m.setFavorite(!m.isFavorite()), null);
			return;
		}
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
		int rowW = w - Ui.px(14);
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

	private void drawPerformancePage(Gfx g, int x, int top, int w, int bottom, double mx, double my) {
		int left = drawClose(g, x + w, top, mx, my);
		drawTitle(g, x, top - Ui.px(2), "Performance", "Turn effects off to make the menus lighter on slow PCs", left - x - Ui.px(20));
		int contentY = top + Ui.px(68);
		clip(g, x - Ui.px(8), contentY, w + Ui.px(16), bottom - contentY);
		drawSettingsList(g, x, contentY, w, bottom, mx, my);
		g.endScissor();
	}

	/** A plain information card (surface) used by the Settings and Accounts pages. */
	private static void card(Gfx g, int x, int y, int w, int h, boolean hover) {
		RenderUtils.surface(g, x, y, w, h, Ui.px(Theme.RADIUS_CARD), hover ? Theme.CARD_HOVER_TOP : Theme.CARD_TOP,
				hover ? Theme.CARD_HOVER_BOTTOM : Theme.CARD_BOTTOM, hover ? Theme.BORDER_HOVER : Theme.BORDER);
	}

	/** Switch between the launcher's accounts without restarting the game. */
	private void drawAccountsPage(Gfx g, int x, int top, int w, int bottom, double mx, double my) {
		int left = drawClose(g, x + w, top, mx, my);
		drawTitle(g, x, top - Ui.px(2), "Accounts", "Switch accounts without restarting the game", left - x - Ui.px(20));
		int y = top + Ui.px(72);

		// Who you are now.
		int ch = Ui.px(104);
		RenderUtils.surface(g, x, y, w, ch, Ui.px(Theme.RADIUS_CARD), Theme.CARD_ON_TOP, Theme.CARD_BOTTOM, 0x50E5323E);
		int logo = Math.max(32, Ui.px(56) / 8 * 8);
		RenderUtils.glow(g, x + Ui.px(24) + logo / 2, y + ch / 2, logo, 0x40E5323E, 6);
		g.logo(x + Ui.px(24), y + (ch - logo) / 2, logo, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		int tx = x + Ui.px(24) + logo + Ui.px(22);
		UIFont.TINY.draw(g, "PLAYING AS", tx, y + Ui.px(26), Theme.ACCENT);
		UIFont.HUGE.draw(g, AccountSwitcher.currentName(), tx, y + Ui.px(42), Theme.TEXT);
		if (AccountSwitcher.available()) {
			int rw = Ui.px(130), rh = Ui.px(40), rx = x + w - rw - Ui.px(24), ry = y + (ch - rh) / 2;
			boolean rHover = Widgets.inside(mx, my, rx, ry, rw, rh);
			Widgets.button(g, rx, ry, rw, rh, AccountSwitcher.loading() ? "Loading…" : "Refresh", UIFont.BODY, 0xFF22242A, Theme.TEXT, rHover);
			hit(rx, ry, rw, rh, false, AccountSwitcher::refresh, null);
		}
		y += ch + Ui.px(18);

		if (!AccountSwitcher.available()) {
			UIFont.BODY.draw(g, "Start the game from the Eclipse Client launcher to switch accounts here.", x, y, Theme.TEXT_MUTED);
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
		int rh = Ui.px(64);
		for (AccountSwitcher.Account a : list) {
			if (y + rh > bottom) break;
			boolean isCurrent = a.name.equalsIgnoreCase(current);
			boolean hover = !isCurrent && Widgets.inside(mx, my, x, y, w, rh);
			card(g, x, y, w, rh, hover);
			int tile = Ui.px(40);
			RenderUtils.roundedRect(g, x + Ui.px(14), y + (rh - tile) / 2, tile, tile, Ui.px(Theme.RADIUS_LARGE), isCurrent ? Theme.ACCENT_SOFT : 0x10FFFFFF);
			Icons.draw(g, Icon.USER, x + Ui.px(14) + tile / 2, y + rh / 2, Ui.px(22), isCurrent ? Theme.ACCENT : Theme.TEXT_MUTED);
			UIFont.TITLE.draw(g, a.name, x + Ui.px(68), y + rh / 2 - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
			UIFont.SMALL.draw(g, a.microsoft() ? "Microsoft account" : "Offline account (singleplayer and offline servers)",
					x + Ui.px(68), y + rh / 2 + Ui.px(5), Theme.TEXT_MUTED);
			int bw = Ui.px(130), bh = Ui.px(38), bx = x + w - bw - Ui.px(14), by = y + (rh - bh) / 2;
			if (isCurrent) {
				chip(g, "IN USE", x + w - Ui.px(24) - UIFont.TINY.width("IN USE") - Ui.px(14), y + (rh - Ui.px(20)) / 2, Theme.ACCENT_SOFT, 0xFFF07B83);
			} else {
				boolean bHover = Widgets.inside(mx, my, bx, by, bw, bh);
				boolean busy = AccountSwitcher.switching();
				Widgets.button(g, bx, by, bw, bh, busy ? "…" : "Switch", UIFont.BODY, Theme.ACCENT, 0xFFFFFFFF, bHover && !busy);
				hit(x, y, w, rh, false, () -> AccountSwitcher.switchTo(a), null);
			}
			y += rh + Ui.px(10);
		}
		UIFont.SMALL.draw(g, "Switching keeps the game open. On a server, leave and rejoin to play there as the new account.",
				x, Math.min(y + Ui.px(6), bottom - UIFont.SMALL.size()), Theme.TEXT_MUTED);
	}

	/** Sidebar "Settings": about the client, links and reset buttons. */
	private void drawSettingsPage(Gfx g, int x, int top, int w, int bottom, double mx, double my) {
		int left = drawClose(g, x + w, top, mx, my);
		drawTitle(g, x, top - Ui.px(2), "Settings", "About the client, tips and resets", left - x - Ui.px(20));
		int y = top + Ui.px(72);

		// About card.
		int ch = Ui.px(140);
		RenderUtils.surface(g, x, y, w, ch, Ui.px(Theme.RADIUS_CARD), Theme.CARD_ON_TOP, Theme.CARD_BOTTOM, 0x50E5323E);
		int logo = Math.max(32, Ui.px(88) / 8 * 8);
		RenderUtils.glow(g, x + Ui.px(28) + logo / 2, y + ch / 2, logo, 0x45E5323E, 7);
		g.logo(x + Ui.px(28), y + (ch - logo) / 2, logo, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		int tx = x + Ui.px(28) + logo + Ui.px(26);
		UIFont.HUGE.draw(g, "Eclipse Client", tx, y + Ui.px(30), Theme.TEXT);
		UIFont.BODY.draw(g, "Version " + TatnatClient.VERSION + "   ·   made by tatnat", tx, y + Ui.px(82), Theme.TEXT_MUTED);
		int yw = Ui.px(240), yh = Ui.px(42), yx = x + w - yw - Ui.px(24), yy = y + (ch - yh) / 2;
		boolean yHover = Widgets.inside(mx, my, yx, yy, yw, yh);
		if (yHover) RenderUtils.glow(g, yx + yw / 2, yy + yh / 2, yw / 2 + Ui.px(10), 0x30E5323E, 6);
		RenderUtils.surface(g, yx, yy, yw, yh, yh / 2, yHover ? Colors.shade(Theme.ACCENT_LIGHT, 1.1f) : Theme.ACCENT_LIGHT, Theme.ACCENT, 0x40FFFFFF);
		Icons.draw(g, Icon.YOUTUBE, yx + Ui.px(30), yy + yh / 2, Ui.px(24), 0xFFFFFFFF);
		UIFont.BODY.drawMid(g, "Subscribe: @tatnatmc", yx + Ui.px(52), yy + yh / 2, 0xFFFFFFFF);
		hit(yx, yy, yw, yh, false, () -> TatnatClient.game().openUrl(YOUTUBE), null);
		y += ch + Ui.px(18);

		// Tips.
		String[][] rows = {
				{"Open the menu", "Right Shift"},
				{"Move your HUD", "HUD Editor: drag, scroll to resize, right-click for settings"},
				{"Toggle a mod quickly", "Right-click its card, or give it a Toggle Key"},
		};
		Icon[] tipIcons = {Icon.KEYBOARD, Icon.MOVE, Icon.MOUSE};
		int rh = Ui.px(56);
		for (int i = 0; i < rows.length; i++) {
			card(g, x, y, w, rh, false);
			Icons.draw(g, tipIcons[i], x + Ui.px(30), y + rh / 2, Ui.px(20), Theme.ACCENT);
			UIFont.TITLE.drawMid(g, rows[i][0], x + Ui.px(56), y + rh / 2, Theme.TEXT);
			UIFont.SMALL.drawRight(g, rows[i][1], x + w - Ui.px(20), y + rh / 2 - UIFont.SMALL.size() / 2, Theme.TEXT_MUTED);
			y += rh + Ui.px(8);
		}
		// Client options with a switch.
		{
			ClientOptions opt = ClientOptions.INSTANCE;
			boolean hover = Widgets.inside(mx, my, x, y, w, rh);
			card(g, x, y, w, rh, hover);
			Icons.draw(g, Icon.MONITOR, x + Ui.px(30), y + rh / 2, Ui.px(20), Theme.ACCENT);
			UIFont.TITLE.draw(g, opt.titleButton.name, x + Ui.px(56), y + rh / 2 - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
			UIFont.SMALL.draw(g, opt.titleButton.description, x + Ui.px(56), y + rh / 2 + Ui.px(4), Theme.TEXT_MUTED);
			int sw = Widgets.toggleW(), sx = x + w - Ui.px(20) - sw, sy = y + (rh - Widgets.toggleH()) / 2;
			titleButtonAnim.animateTo(opt.titleButton.on() ? 1f : 0f);
			Widgets.toggle(g, sx, sy, titleButtonAnim, hover);
			hit(x, y, w, rh, false, () -> {
				opt.titleButton.set(!opt.titleButton.on());
				TatnatClient.CONFIG.markDirty();
			}, null);
			y += rh + Ui.px(8);
		}
		y += Ui.px(10);
		int bw = Ui.px(220), bh = Ui.px(44);
		boolean h1 = Widgets.inside(mx, my, x, y, bw, bh);
		Widgets.button(g, x, y, bw, bh, "Reset HUD layout", UIFont.BODY, 0xFF22242A, Theme.TEXT, h1);
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
				confirming ? Theme.ACCENT : 0xFF22242A, Theme.TEXT, h2);
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
		int bw = Math.max(2, Ui.px(4));
		RenderUtils.roundedRect(g, x, top, bw, height, bw / 2, 0x0AFFFFFF);
		int barH = Math.max(Ui.px(36), height * height / content);
		int barY = top + (int) Math.round((height - barH) * (scroll / (content - height)));
		RenderUtils.roundedRect(g, x, barY, bw, barH, bw / 2, 0x45FFFFFF);
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
