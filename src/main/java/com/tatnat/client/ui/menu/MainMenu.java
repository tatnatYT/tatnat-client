package com.tatnat.client.ui.menu;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.ClientOptions;
import com.tatnat.client.platform.Game;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.platform.UiScreen;
import com.tatnat.client.ui.clickgui.ClickGuiScreen;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.Icons.Icon;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.render.UIFont;
import com.tatnat.client.ui.render.Ui;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;

/**
 * The Eclipse main menu, shown instead of Minecraft's title screen (Settings > Eclipse Main Menu).
 * The launcher's background art fills the screen (folder from {@code -Dtatnat.backgrounds}; a
 * plain night-sky gradient without it), drifting slowly with twinkling stars and the odd shooting
 * star. The arrows at the sides switch pictures with a zoom-and-sweep transition. In the middle:
 * the logo, the name and the buttons; along the bottom small icon buttons.
 */
public final class MainMenu implements UiScreen {
	/** Mod list screens of the loaders and of Mod Menu, opened by the "Mods" button when present. */
	public static final String[] MOD_LISTS = {
			"com.terraformersmc.modmenu.gui.ModsScreen",
			"net.neoforged.neoforge.client.gui.ModListScreen",
			"net.minecraftforge.client.gui.ModListScreen",
			"net.minecraftforge.fml.client.GuiModList",
	};

	/** Swap the vanilla title screen for this one? */
	public static boolean replaces() {
		return ClientOptions.customMenu();
	}

	private static List<File> pictures;
	private static final long START = System.nanoTime();

	private int shown = -1, previous = -1;
	private long swapAt;
	private int swapDir = 1;
	private final List<Hit> hits = new ArrayList<>();
	private final float[][] stars;
	private long nextShoot = System.currentTimeMillis() + 2500;
	private float shootX, shootY;
	private long shootAt = -1;
	private final Random random = new Random();
	private String tooltip;

	private static final class Hit {
		final int x, y, w, h;
		final String label;
		final Runnable action;

		Hit(int x, int y, int w, int h, String label, Runnable action) {
			this.label = label;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.action = action;
		}
	}

	/** Dev checks only: the menu last opened, and a click on the button with this label. */
	public static MainMenu last;

	public boolean devClick(String label) {
		for (Hit h : hits) {
			if (h.label.equals(label)) return mouseClicked(h.x + h.w / 2.0, h.y + h.h / 2.0, 0);
		}
		return false;
	}

	public MainMenu() {
		last = this;
		stars = new float[40][];
		for (int i = 0; i < stars.length; i++) {
			stars[i] = new float[] {random.nextFloat(), random.nextFloat() * 0.6f, 2.5f + random.nextFloat() * 4, random.nextFloat() * 7, random.nextFloat() < 0.2f ? 1 : 0};
		}
		List<File> pics = pictures();
		if (!pics.isEmpty()) shown = Math.floorMod(ClientOptions.INSTANCE.menuBackground.get().intValue(), pics.size());
	}

	/** The PNGs in the launcher's background folder, sorted by name. */
	private static List<File> pictures() {
		if (pictures != null) return pictures;
		List<File> out = new ArrayList<>();
		String dir = System.getProperty("tatnat.backgrounds");
		File[] files = dir == null ? null : new File(dir).listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
		if (files != null) {
			Arrays.sort(files);
			out.addAll(Arrays.asList(files));
		}
		return pictures = out;
	}

	private static float now() {
		return (System.nanoTime() - START) / 1e9f;
	}

	@Override
	public Backdrop backdrop() {
		return Backdrop.NONE;
	}

	@Override
	public boolean pausesGame() {
		return false;
	}

	@Override
	public void render(Gfx g, double mx, double my) {
		Ui.update();
		RenderUtils.beginPixels(g);
		hits.clear();
		tooltip = null;
		Game game = TatnatClient.game();
		int W = game.windowWidth(), H = game.windowHeight();
		float t = now();

		drawBackground(g, W, H, t);

		// Logo and name.
		int logo = Math.max(48, Ui.px(112) / 8 * 8);
		int cy = H / 2 - Ui.px(110);
		RenderUtils.glow(g, W / 2, cy, logo, 0x408A63E8, 8);
		g.logo(W / 2 - logo / 2, cy - logo / 2 - Ui.px(26), logo, 0xFFFFFFFF);
		String name = "E C L I P S E   C L I E N T";
		UIFont.HUGE.drawCentered(g, name, W / 2 + 2, cy + logo / 2 - Ui.px(8) + 2, 0x60000000);
		UIFont.HUGE.drawCentered(g, name, W / 2, cy + logo / 2 - Ui.px(8), 0xFFFFFFFF);

		// Main buttons.
		int bw = Ui.px(330), bh = Ui.px(40), gap = Ui.px(10);
		int by = cy + logo / 2 + Ui.px(56);
		boolean modList = modListAvailable();
		Object[][] buttons = {
				{"SINGLEPLAYER", (Runnable) () -> game.titleAction("singleplayer")},
				{"MULTIPLAYER", (Runnable) () -> game.titleAction("multiplayer")},
				{"ECLIPSE MODS", (Runnable) () -> game.openScreen(new ClickGuiScreen())},
		};
		for (Object[] b : buttons) {
			button(g, W / 2 - bw / 2, by, bw, bh, (String) b[0], mx, my, (Runnable) b[1], b == buttons[2]);
			by += bh + gap;
		}

		// Small icon buttons along the bottom.
		List<Object[]> icons = new ArrayList<>();
		icons.add(new Object[] {Icon.GEAR, "Options", (Runnable) () -> game.titleAction("options")});
		if (modList) icons.add(new Object[] {Icon.BOX, "Mods", (Runnable) () -> game.titleAction("mods")});
		icons.add(new Object[] {Icon.USER, "Accounts", (Runnable) () -> game.openScreen(ClickGuiScreen.accounts())});
		icons.add(new Object[] {Icon.YOUTUBE, "tatnat on YouTube", (Runnable) () -> game.openUrl("https://www.youtube.com/@tatnatmc")});
		icons.add(new Object[] {Icon.CLOSE, "Quit Game", (Runnable) () -> game.titleAction("quit")});
		int is = Ui.px(34), ig = Ui.px(8), total = icons.size() * is + (icons.size() - 1) * ig;
		int ix = W / 2 - total / 2, iy = H - Ui.px(70);
		for (Object[] ic : icons) {
			iconButton(g, ix, iy, is, (Icon) ic[0], (String) ic[1], mx, my, (Runnable) ic[2]);
			ix += is + ig;
		}

		// Background arrows at the sides, Lunar style.
		List<File> pics = pictures();
		if (pics.size() > 1) {
			int as = Ui.px(46), ay = H / 2 - as / 2;
			arrow(g, Ui.px(26), ay, as, false, mx, my, () -> swap(-1));
			arrow(g, W - Ui.px(26) - as, ay, as, true, mx, my, () -> swap(1));
		}

		// Corners: version and credit.
		UIFont.SMALL.draw(g, "Minecraft " + game.minecraftVersion(), Ui.px(16), H - Ui.px(16) - UIFont.SMALL.size(), 0xB0FFFFFF);
		UIFont.SMALL.drawRight(g, "Eclipse Client " + TatnatClient.VERSION + "  ·  made by tatnat", W - Ui.px(16), H - Ui.px(16) - UIFont.SMALL.size(), 0xB0FFFFFF);

		if (tooltip != null) {
			int tw = UIFont.SMALL.width(tooltip) + Ui.px(18), th = Ui.px(28);
			int tx = (int) Math.min(W - tw - 4, Math.max(4, mx - tw / 2.0)), ty = iy - th - Ui.px(10);
			RenderUtils.surface(g, tx, ty, tw, th, Ui.px(Theme.RADIUS), 0xF0201E28, 0xF0181620, 0x40FFFFFF);
			UIFont.SMALL.drawMid(g, tooltip, tx + Ui.px(9), ty + th / 2, Theme.TEXT);
		}
		RenderUtils.end(g);
	}

	// ---------------------------------------------------------------- background

	private void drawBackground(Gfx g, int W, int H, float t) {
		List<File> pics = pictures();
		if (shown < 0 || pics.isEmpty()) {
			RenderUtils.verticalGradient(g, 0, 0, W, H, 0xFF0E0C16, 0xFF2A1F4A);
		} else {
			float p = Math.min(1f, (System.currentTimeMillis() - swapAt) / 900f);
			float e = 1 - (1 - p) * (1 - p) * (1 - p); // ease out
			// Slow drift: a little zoom and pan, back and forth.
			float drift = (float) (Math.sin(t / 26f * Math.PI * 2) * 0.5 + 0.5);
			if (previous >= 0 && p < 1) {
				picture(g, pics, previous, W, H, drift, 1f - 0.08f * e, 1f - e);
				picture(g, pics, shown, W, H, drift, 1.12f - 0.12f * e, e);
				// The light sweep that crosses the screen while swapping.
				int band = W / 5, bx = (int) ((swapDir > 0 ? -band + (W + band) * e : W - (W + band) * e));
				int a = (int) (140 * Math.sin(Math.PI * p));
				RenderUtils.horizontalGradient(g, bx, 0, bx + band / 2, H, 0x00B49CFF, (a << 24) | 0xB49CFF);
				RenderUtils.horizontalGradient(g, bx + band / 2, 0, bx + band, H, (a << 24) | 0xB49CFF, 0x00B49CFF);
			} else {
				picture(g, pics, shown, W, H, drift, 1f, 1f);
			}
		}
		// Readability: darker towards the bottom and in the middle behind the buttons.
		RenderUtils.verticalGradient(g, 0, H / 2, W, H, 0x00000000, 0x90000000);
		RenderUtils.glow(g, W / 2, H / 2, Math.min(W, H) / 2, 0x30000000, 6);

		// Twinkling stars.
		for (float[] s : stars) {
			float a = (float) Math.max(0, Math.sin((t + s[3]) / s[2] * Math.PI * 2));
			int size = Math.max(2, Ui.px(s[4] > 0 ? 4 : 3));
			int x = (int) (s[0] * W), y = (int) (s[1] * H);
			int c = ((int) (a * 220) << 24) | 0xE9E2FF;
			RenderUtils.rect(g, x, y, x + size, y + size, c);
			if (a > 0.6f) RenderUtils.glow(g, x + size / 2, y + size / 2, size * 2, ((int) (a * 70) << 24) | 0xB49CFF, 3);
		}
		// The odd shooting star.
		long ms = System.currentTimeMillis();
		if (shootAt < 0 && ms > nextShoot) {
			shootAt = ms;
			shootX = 0.45f + random.nextFloat() * 0.5f;
			shootY = random.nextFloat() * 0.3f;
		}
		if (shootAt >= 0) {
			float p = (ms - shootAt) / 1100f;
			if (p >= 1) {
				shootAt = -1;
				nextShoot = ms + 6000 + random.nextInt(9000);
			} else {
				float x0 = shootX * W - p * Ui.px(420), y0 = shootY * H + p * Ui.px(220);
				float fade = (float) Math.sin(Math.PI * p);
				int seg = Math.max(2, Ui.px(3));
				for (int i = 0; i < 26; i++) {
					int a = (int) (fade * 230 * (1 - i / 26f));
					int x = (int) (x0 + i * seg * 1.9f), y = (int) (y0 - i * seg);
					RenderUtils.rect(g, x, y, x + seg, y + seg, (a << 24) | (i < 3 ? 0xFFFFFF : 0xD9CCFF));
				}
			}
		}
	}

	/** One background picture covering the screen, zoomed by {@code zoom} around the middle. */
	private static void picture(Gfx g, List<File> pics, int i, int W, int H, float drift, float zoom, float alpha) {
		if (alpha <= 0.01f) return;
		File f = pics.get(i);
		float z = zoom * (1f + 0.035f * drift);
		// Cover the window (pictures are 16:9 like most screens).
		float scale = Math.max(W / 1600f, H / 900f) * z;
		int w = Math.round(1600 * scale), h = Math.round(900 * scale);
		int x = (W - w) / 2 - Math.round(drift * W * 0.012f), y = (H - h) / 2 - Math.round(drift * H * 0.008f);
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
		g.picture("bg:" + f.getName(), () -> read(f), x, y, w, h, (a << 24) | 0xFFFFFF);
	}

	private static byte[] read(File f) {
		try {
			return Files.readAllBytes(f.toPath());
		} catch (Exception e) {
			return null;
		}
	}

	private void swap(int dir) {
		List<File> pics = pictures();
		if (pics.size() < 2 || System.currentTimeMillis() - swapAt < 900) return;
		previous = shown;
		shown = Math.floorMod(shown + dir, pics.size());
		swapDir = dir;
		swapAt = System.currentTimeMillis();
		ClientOptions.INSTANCE.menuBackground.set((double) shown);
		TatnatClient.CONFIG.markDirty();
	}

	// ---------------------------------------------------------------- widgets

	private void button(Gfx g, int x, int y, int w, int h, String text, double mx, double my, Runnable action, boolean accent) {
		boolean hover = inside(mx, my, x, y, w, h);
		int r = Ui.px(6);
		if (hover) RenderUtils.glow(g, x + w / 2, y + h / 2, w / 2, 0x288A63E8, 5);
		int top = accent ? (hover ? 0xE09873F0 : 0xC08A63E8) : (hover ? 0xB0302848 : 0x80181520);
		int bottom = accent ? (hover ? 0xE07A55DA : 0xC06F4CC9) : (hover ? 0xB0241E38 : 0x80100E18);
		RenderUtils.surface(g, x, y, w, h, r, top, bottom, hover ? 0x90B49CFF : 0x38FFFFFF);
		UIFont.SMALL.drawCentered(g, spaced(text), x + w / 2, y + (h - UIFont.SMALL.size()) / 2, hover || accent ? 0xFFFFFFFF : 0xFFD8D4E6);
		hits.add(new Hit(x, y, w, h, text, action));
	}

	private void iconButton(Gfx g, int x, int y, int s, Icon icon, String tip, double mx, double my, Runnable action) {
		boolean hover = inside(mx, my, x, y, s, s);
		RenderUtils.surface(g, x, y, s, s, Ui.px(6), hover ? 0xB0302848 : 0x80181520, hover ? 0xB0241E38 : 0x80100E18,
				hover ? 0x90B49CFF : 0x38FFFFFF);
		Icons.draw(g, icon, x + s / 2, y + s / 2, Math.round(s * 0.5f), hover ? 0xFFFFFFFF : 0xFFC9C2E0);
		if (hover) tooltip = tip;
		hits.add(new Hit(x, y, s, s, tip, action));
	}

	private void arrow(Gfx g, int x, int y, int s, boolean right, double mx, double my, Runnable action) {
		boolean hover = inside(mx, my, x, y, s, s);
		if (hover) RenderUtils.glow(g, x + s / 2, y + s / 2, s, 0x308A63E8, 5);
		RenderUtils.roundedRect(g, x, y, s, s, s / 2, hover ? 0x907A55DA : 0x50100E18);
		RenderUtils.roundedOutline(g, x, y, s, s, s / 2, 1, hover ? 0xA0B49CFF : 0x40FFFFFF);
		Icons.draw(g, right ? Icon.FORWARD : Icon.BACK, x + s / 2, y + s / 2, Math.round(s * 0.46f), 0xFFFFFFFF);
		hits.add(new Hit(x, y, s, s, right ? ">" : "<", action));
	}

	/** "SINGLEPLAYER" -> "S I N G L E P L A Y E R" (the wide-letter look of the reference). */
	private static String spaced(String s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == ' ') {
				b.append("  ");
				continue;
			}
			if (i > 0) b.append(' ');
			b.append(s.charAt(i));
		}
		return b.toString();
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	private static Boolean modList;

	private static boolean modListAvailable() {
		if (modList == null) {
			modList = false;
			for (String c : MOD_LISTS) {
				try {
					Class.forName(c, false, MainMenu.class.getClassLoader());
					modList = true;
					break;
				} catch (Throwable ignored) {
				}
			}
		}
		return modList;
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (button != 0) return false;
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (inside(x, y, h.x, h.y, h.w, h.h)) {
				h.action.run();
				return true;
			}
		}
		return false;
	}

	@Override
	public void mouseReleased(double x, double y, int button) {
	}

	@Override
	public void mouseDragged(double x, double y, int button) {
	}

	@Override
	public void mouseScrolled(double x, double y, double amount) {
	}

	@Override
	public boolean keyPressed(int key, boolean shift) {
		if (key == com.tatnat.client.util.KeyCodes.LEFT) swap(-1);
		else if (key == com.tatnat.client.util.KeyCodes.RIGHT) swap(1);
		return true; // Escape does nothing on the main menu
	}

	@Override
	public boolean charTyped(String chars) {
		return true;
	}

	@Override
	public void removed() {
	}
}
