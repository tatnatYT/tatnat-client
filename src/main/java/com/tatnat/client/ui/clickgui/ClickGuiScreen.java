package com.tatnat.client.ui.clickgui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.ModuleManager;
import com.tatnat.client.modules.settings.Setting;
import com.tatnat.client.ui.hud.HudEditorScreen;
import com.tatnat.client.ui.render.Animation;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.Icons.Icon;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;

import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
public class ClickGuiScreen extends Screen {
	private static final Identifier LOGO = Identifier.fromNamespaceAndPath(TatnatClient.ID, "logo.png");
	private static final String YOUTUBE = "https://www.youtube.com/@tatnatmc";

	private enum Page { MODS, SETTINGS }

	// Remembered between openings, like Feather.
	private static Category lastCategory = null;
	private static boolean lastFavorites, lastList;

	private final Animation open = new Animation(200, 0f);
	private final Animation view = new Animation(220, 0f);
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
	private int contentH, setContentH;
	private int clipX, clipY, clipW, clipH;

	/** Clickable regions registered while drawing; checked in reverse (topmost first). */
	private record Hit(int x, int y, int w, int h, boolean clipped, Runnable left, Runnable right) {
	}

	private final List<Hit> hits = new ArrayList<>();

	public ClickGuiScreen() {
		super(Component.literal("tatnat client"));
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
			if (c instanceof ColorComponent cc) {
				cc.devOpen();
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
	public boolean isPauseScreen() {
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

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		// Like Feather: the world stays fully visible behind the menu. Only the title screen
		// (no world) gets the usual panorama.
		if (minecraft.level == null) {
			renderPanorama(g, partialTick);
			renderBlurredBackground(g);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (closing && open.isDone()) {
			minecraft.setScreen(null);
			return;
		}
		Ui.update();
		float o = open.get();
		RenderUtils.alpha = o;
		RenderUtils.beginPixels(g);
		hits.clear();
		double mx = Widgets.mouseX(), my = Widgets.mouseY();

		int W = minecraft.getWindow().getWidth(), H = minecraft.getWindow().getHeight();
		int sideW = Ui.px(96), gap = Ui.px(14), panelW = Ui.px(912), panelH = Ui.px(666);
		int groupX = (W - (sideW + gap + panelW)) / 2;
		int panelX = groupX + sideW + gap;
		int panelY = (H - panelH) / 2 + Ui.px(18) + Math.round((1 - o) * Ui.px(10));

		drawToolbar(g, panelX, panelY, panelW, mx, my);
		drawSidebar(g, groupX, panelY, sideW, panelH, mx, my);

		RenderUtils.shadow(g, panelX, panelY, panelW, panelH, Ui.px(Theme.RADIUS_LARGE), 8, 0.35f);
		RenderUtils.roundedRect(g, panelX, panelY, panelW, panelH, Ui.px(Theme.RADIUS_LARGE), Theme.BACKGROUND);
		if (page == Page.MODS) drawModsPage(g, panelX, panelY, panelW, panelH, mx, my);
		else drawSettingsPage(g, panelX, panelY, panelW, panelH, mx, my);

		RenderUtils.end(g);
		RenderUtils.alpha = 1f;
	}

	/** The small bar above the panel: close chevron in the middle, view buttons on the right. */
	private void drawToolbar(GuiGraphics g, int px, int py, int pw, double mx, double my) {
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
				switch (idx) {
					case 0 -> minecraft.setScreen(new HudEditorScreen(this));
					case 1 -> {
						favoritesOnly = !favoritesOnly;
						page = Page.MODS;
						closeSettings();
						scrollTarget = 0;
					}
					case 2 -> listView = false;
					default -> listView = true;
				}
			}, null);
		}
	}

	private void drawSidebar(GuiGraphics g, int x, int y, int w, int h, double mx, double my) {
		RenderUtils.shadow(g, x, y, w, h, Ui.px(Theme.RADIUS_LARGE), 8, 0.35f);
		RenderUtils.roundedRect(g, x, y, w, h, Ui.px(Theme.RADIUS_LARGE), Theme.BACKGROUND);

		// Logo (the player's head), drawn at an exact multiple of 8 so the pixels stay square.
		int logo = Math.max(32, Ui.px(56) / 8 * 8);
		g.pose().pushMatrix();
		g.pose().translate(x + (w - logo) / 2f, y + Ui.px(18));
		g.pose().scale(logo / 64f, logo / 64f);
		g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, 64, 64, 64, 64, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		g.pose().popMatrix();
		RenderUtils.rect(g, x + Ui.px(16), y + Ui.px(90), x + w - Ui.px(16), y + Ui.px(91), Theme.DIVIDER);

		String[] labels = {"MOD MENU", "HUD EDITOR", "SETTINGS"};
		Icon[] icons = {Icon.GRID, Icon.MOVE, Icon.GEAR};
		int bw = w - Ui.px(16), bh = Ui.px(70);
		for (int i = 0; i < labels.length; i++) {
			int bx = x + Ui.px(8), by = y + Ui.px(104) + i * (bh + Ui.px(8));
			boolean active = (i == 0 && page == Page.MODS) || (i == 2 && page == Page.SETTINGS);
			boolean hover = Widgets.inside(mx, my, bx, by, bw, bh);
			if (active) RenderUtils.roundedRect(g, bx, by, bw, bh, Ui.px(Theme.RADIUS), Theme.ACCENT);
			else if (hover) RenderUtils.roundedRect(g, bx, by, bw, bh, Ui.px(Theme.RADIUS), Theme.HOVER);
			int col = active || hover ? 0xFFFFFFFF : Theme.TEXT_MUTED;
			Icons.draw(g, icons[i], bx + bw / 2, by + Ui.px(28), Ui.px(28), col);
			UIFont.TINY.drawCentered(g, labels[i], bx + bw / 2, by + Ui.px(50), col);
			final int idx = i;
			hit(bx, by, bw, bh, false, () -> {
				switch (idx) {
					case 0 -> {
						page = Page.MODS;
						closeSettings();
					}
					case 1 -> minecraft.setScreen(new HudEditorScreen(this));
					default -> page = Page.SETTINGS;
				}
			}, null);
		}
	}

	/** Red slanted tab in the panel's top-left corner. Returns its right edge. */
	private int drawTab(GuiGraphics g, int x, int y, String text, boolean back, double mx, double my) {
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

	private void drawModsPage(GuiGraphics g, int px, int py, int pw, int ph, double mx, double my) {
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
		g.disableScissor();
		RenderUtils.alpha = prev;
	}

	private void drawSearch(GuiGraphics g, int x, int y, int w, int h, double mx, double my) {
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

	private void drawModList(GuiGraphics g, int x, int y, int w, int bottom, double mx, double my, boolean interactive) {
		List<Module> mods = visibleModules();
		int gap = Ui.px(14);
		int cols = listView ? 1 : 3;
		int cardW = (w - gap * (cols - 1) - Ui.px(10)) / cols;
		int cardH = listView ? Ui.px(78) : Ui.px(190);
		int rows = (mods.size() + cols - 1) / cols;
		contentH = Math.max(0, rows * (cardH + gap) - gap);
		scrollTarget = clampScroll(scrollTarget, contentH, bottom - y);
		scroll += (scrollTarget - scroll) * 0.3;
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

	private void drawCard(GuiGraphics g, Module m, int x, int y, int w, int h, double mx, double my, boolean interactive) {
		boolean hover = interactive && Widgets.inside(mx, my, x, y, w, h);
		Animation ha = hoverAnim(m);
		ha.animateTo(hover ? 1f : 0f);
		RenderUtils.roundedRect(g, x, y, w, h, Ui.px(Theme.RADIUS), Colors.lerp(Theme.PANEL, Theme.HOVER, ha.get()));

		// Big line-art icon, dimmed while the mod is off.
		int iconCol = m.isEnabled() ? Theme.ICON : 0xFF5C5C62;
		Icons.draw(g, m.icon(), x + w / 2, y + Ui.px(80), Ui.px(82), iconCol);

		// Favourite heart, top-right.
		int hs = Ui.px(20), hx = x + w - Ui.px(26), hy = y + Ui.px(24);
		boolean heartHover = hover && Widgets.inside(mx, my, hx - hs, hy - hs, hs * 2, hs * 2);
		Icons.draw(g, m.isFavorite() ? Icon.HEART_FILLED : Icon.HEART, hx, hy, hs,
				m.isFavorite() ? Theme.ACCENT : heartHover ? 0xFFFFFFFF : 0xFF77777C);

		// Name + switch along the bottom.
		int mid = y + h - Ui.px(30);
		int tw = Widgets.toggleW();
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

	private void drawRow(GuiGraphics g, Module m, int x, int y, int w, int h, double mx, double my, boolean interactive) {
		boolean hover = interactive && Widgets.inside(mx, my, x, y, w, h);
		Animation ha = hoverAnim(m);
		ha.animateTo(hover ? 1f : 0f);
		RenderUtils.roundedRect(g, x, y, w, h, Ui.px(Theme.RADIUS), Colors.lerp(Theme.PANEL, Theme.HOVER, ha.get()));
		Icons.draw(g, m.icon(), x + Ui.px(42), y + h / 2, Ui.px(40), m.isEnabled() ? Theme.ICON : 0xFF5C5C62);

		int tw = Widgets.toggleW();
		int tx = x + w - Ui.px(18) - tw, ty = y + (h - Widgets.toggleH()) / 2;
		int hs = Ui.px(20), hx = tx - Ui.px(30), hy = y + h / 2;
		int textW = hx - hs - (x + Ui.px(80)) - Ui.px(10);
		UIFont.TITLE.draw(g, UIFont.TITLE.trim(m.name, textW), x + Ui.px(80), y + h / 2 - UIFont.TITLE.size() + Ui.px(1), Theme.TEXT);
		UIFont.SMALL.draw(g, UIFont.SMALL.trim(m.description, textW), x + Ui.px(80), y + h / 2 + Ui.px(4), Theme.TEXT_MUTED);
		boolean heartHover = hover && Widgets.inside(mx, my, hx - hs, hy - hs, hs * 2, hs * 2);
		Icons.draw(g, m.isFavorite() ? Icon.HEART_FILLED : Icon.HEART, hx, hy, hs,
				m.isFavorite() ? Theme.ACCENT : heartHover ? 0xFFFFFFFF : 0xFF77777C);
		Animation ta = toggleAnim(m);
		ta.animateTo(m.isEnabled() ? 1f : 0f);
		Widgets.toggle(g, tx, ty, ta, hover && Widgets.inside(mx, my, tx - Ui.px(6), y, tw + Ui.px(24), h));

		if (!interactive) return;
		hit(x, y, w, h, true, () -> openSettings(m), m::toggle);
		hit(hx - hs, hy - hs, hs * 2, hs * 2, true, () -> m.setFavorite(!m.isFavorite()), null);
		hit(tx - Ui.px(6), y, tw + Ui.px(24), h, true, m::toggle, m::toggle);
	}

	private void drawSettingsList(GuiGraphics g, int x, int y, int w, int bottom, double mx, double my) {
		int total = 0;
		for (SettingComponent<?> c : components) if (c.visible()) total += c.height();
		setContentH = total;
		setScrollTarget = clampScroll(setScrollTarget, setContentH, bottom - y);
		setScroll += (setScrollTarget - setScroll) * 0.3;
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

	/** Sidebar "Settings": about the client, links and reset buttons. */
	private void drawSettingsPage(GuiGraphics g, int px, int py, int pw, int ph, double mx, double my) {
		drawTab(g, px, py, "Settings", false, mx, my);
		int x = px + Ui.px(18), w = pw - Ui.px(36);
		int y = py + Ui.px(86);

		// About card.
		int ch = Ui.px(150);
		RenderUtils.roundedRect(g, x, y, w, ch, Ui.px(Theme.RADIUS), Theme.PANEL);
		int logo = Math.max(32, Ui.px(96) / 8 * 8);
		g.pose().pushMatrix();
		g.pose().translate(x + Ui.px(28), y + (ch - logo) / 2f);
		g.pose().scale(logo / 64f, logo / 64f);
		g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, 64, 64, 64, 64, Colors.fade(0xFFFFFFFF, RenderUtils.alpha));
		g.pose().popMatrix();
		int tx = x + Ui.px(28) + logo + Ui.px(26);
		UIFont.HUGE.draw(g, "tatnat client", tx, y + Ui.px(30), Theme.TEXT);
		UIFont.BODY.draw(g, "Version " + TatnatClient.VERSION + "   ·   made by tatnat", tx, y + Ui.px(84), Theme.TEXT_MUTED);
		int yw = Ui.px(250), yh = Ui.px(40), yx = x + w - yw - Ui.px(24), yy = y + (ch - yh) / 2;
		boolean yHover = Widgets.inside(mx, my, yx, yy, yw, yh);
		RenderUtils.roundedRect(g, yx, yy, yw, yh, Ui.px(Theme.RADIUS), yHover ? Colors.shade(Theme.ACCENT, 1.15f) : Theme.ACCENT);
		Icons.draw(g, Icon.YOUTUBE, yx + Ui.px(26), yy + yh / 2, Ui.px(24), 0xFFFFFFFF);
		UIFont.BODY.drawMid(g, "Subscribe: @tatnatmc", yx + Ui.px(48), yy + yh / 2, 0xFFFFFFFF);
		hit(yx, yy, yw, yh, false, () -> Util.getPlatform().openUri(URI.create(YOUTUBE)), null);
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

	private void drawScrollbar(GuiGraphics g, int x, int top, int height, double scroll, int content) {
		if (content <= height) return;
		int barH = Math.max(Ui.px(30), height * height / content);
		int barY = top + (int) Math.round((height - barH) * (scroll / (content - height)));
		RenderUtils.roundedRect(g, x, barY, Math.max(2, Ui.px(4)), barH, Math.max(1, Ui.px(2)), 0x50FFFFFF);
	}

	private void clip(GuiGraphics g, int x, int y, int w, int h) {
		clipX = x;
		clipY = y;
		clipW = w;
		clipH = h;
		g.enableScissor(x, y, x + w, y + h);
	}

	private void hit(int x, int y, int w, int h, boolean clipped, Runnable left, Runnable right) {
		hits.add(new Hit(x, y, w, h, clipped, left, right));
	}

	private static double clampScroll(double v, int content, int view) {
		return Math.max(0, Math.min(v, Math.max(0, content - view)));
	}

	// ---------------------------------------------------------------- input (GUI coords -> pixels)

	private static double px(double gui) {
		return gui * RenderUtils.guiScale();
	}

	private boolean settingsVisible() {
		return page == Page.MODS && selected != null && view.target() > 0;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) return true;
		double mx = px(event.x()), my = px(event.y());
		int button = event.button();

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
	public boolean mouseReleased(MouseButtonEvent event) {
		double mx = px(event.x()), my = px(event.y());
		for (SettingComponent<?> c : components) c.mouseReleased(mx, my, event.button());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		double mx = px(event.x()), my = px(event.y());
		if (settingsVisible()) for (SettingComponent<?> c : components) c.mouseDragged(mx, my);
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double step = Ui.px(90) * scrollY;
		if (settingsVisible()) setScrollTarget = clampScroll(setScrollTarget - step, setContentH, clipH);
		else scrollTarget = clampScroll(scrollTarget - step, contentH, clipH);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (settingsVisible()) {
			for (SettingComponent<?> c : components) {
				if (c.isCapturingKeys() && c.keyPressed(key)) return true;
			}
		}
		if (searchFocused) {
			if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				scrollTarget = 0;
				return true;
			}
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
				searchFocused = false;
				return true;
			}
			if (key != TatnatClient.MENU_KEY) return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE && settingsVisible()) {
			closeSettings();
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE || key == TatnatClient.MENU_KEY) {
			close();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		String chars = event.codepointAsString();
		if (settingsVisible()) {
			for (SettingComponent<?> c : components) {
				if (c.isCapturingKeys() && c.charTyped(chars)) return true;
			}
		}
		// Start typing anywhere on the mod grid to search, like Feather.
		if (page != Page.MODS || settingsVisible()) return false;
		if (!searchFocused && Character.isLetterOrDigit(event.codepoint())) searchFocused = true;
		if (searchFocused && search.length() < 32) {
			search += chars;
			scrollTarget = 0;
			return true;
		}
		return false;
	}
}
