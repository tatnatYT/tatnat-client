package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.platform.Gfx;

/**
 * Hearts and Armor Bar: your health hearts and armor icons drawn by the client in your own
 * colours, in vanilla's spots, so texture packs can't change them. The game's own icons are
 * hidden while these are on (hearts 1.17+, armor 1.20.5+).
 */
public final class HudColors {
	private HudColors() {
	}

	static Hearts hearts;
	static ArmorBar armor;

	public static boolean hearts() {
		return hearts != null && hearts.isEnabled();
	}

	public static int heartColor() {
		return hearts.color.color(0);
	}

	public static boolean armor() {
		return armor != null && armor.isEnabled();
	}

	public static int armorColor() {
		return armor.color.color(0);
	}

	// 9 x 8 icons: '#' outline, 'f' fill, 'h' highlight.
	private static final String[] HEART = {
			".###.###.",
			"#hhf#fff#",
			"#hffffff#",
			"#fffffff#",
			".#fffff#.",
			"..#fff#..",
			"...#f#...",
			"....#....",
	};
	private static final String[] CHEST = {
			".###.###.",
			"#hff#fff#",
			"#hffffff#",
			".#fffff#.",
			".#fffff#.",
			".#fffff#.",
			".#fffff#.",
			".#######.",
	};

	/**
	 * One icon at (x, y). {@code part}: 0 empty, 1 left half filled, 2 full. Empty parts show a dark
	 * inside so the container stays visible.
	 */
	private static void drawIcon(Gfx g, String[] art, int x, int y, int part, int fill) {
		int light = blend(fill, 0xFFFFFFFF, 0.45f), empty = 0xFF2B1E22;
		for (int row = 0; row < art.length; row++) {
			String line = art[row];
			int runStart = -1, runColor = 0;
			for (int col = 0; col <= line.length(); col++) {
				int color = 0;
				if (col < line.length()) {
					char c = line.charAt(col);
					if (c == '#') {
						color = 0xFF000000;
					} else if (c != '.') {
						boolean filled = part == 2 || part == 1 && col <= 4;
						color = !filled ? empty : c == 'h' ? light : fill;
					}
				}
				if (runStart >= 0 && color != runColor) {
					g.rect(x + runStart, y + row, x + col, y + row + 1, runColor);
					runStart = -1;
				}
				if (runStart < 0 && color != 0) {
					runStart = col;
					runColor = color;
				}
			}
		}
	}

	private static int blend(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int gr = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | r << 16 | gr << 8 | bl;
	}

	/** Rows of hearts and the gap between them, like vanilla (rows squeeze together past 2). */
	private static int[] heartRows(float[] s) {
		int rows = (int) Math.ceil((s[1] + s[2]) / 2f / 10f);
		return new int[] {Math.max(1, rows), Math.max(10 - (rows - 2), 3)};
	}

	/** Your health hearts in your colour (absorption in gold). */
	public static class Hearts extends Module {
		final ColorSetting color = add(new ColorSetting("Custom Color", "Colour for your hearts", 0xFFFF55AA, true));

		public Hearts() {
			super("Hearts", "Your hearts in your own colour, over any texture pack", Category.VISUAL, false);
			icon = com.tatnat.client.ui.render.Icons.Icon.HEART;
			HudColors.hearts = this;
		}

		@Subscribe
		public void onRender(Events.Render2D e) {
			float[] s = game().barStats();
			if (s == null || game().hudHidden()) return;
			com.tatnat.client.ui.render.RectBatch batch = com.tatnat.client.ui.render.RectBatch.of(e.gfx);
			int left = game().guiWidth() / 2 - 91, base = game().guiHeight() - 39;
			int rowH = heartRows(s)[1];
			int healthHearts = (int) Math.ceil(s[1] / 2f), absorbHearts = (int) Math.ceil(s[2] / 2f);
			int health = (int) Math.ceil(s[0]), absorb = (int) Math.ceil(s[2]);
			int fill = color.color(0);
			for (int i = 0; i < healthHearts + absorbHearts; i++) {
				int x = left + (i % 10) * 8, y = base - (i / 10) * rowH;
				// Low health: hearts shake a little, like vanilla.
				if (health <= 4) y += (int) ((System.currentTimeMillis() / 50 + i * 7) % 2);
				if (i < healthHearts) {
					int v = health - i * 2;
					drawIcon(batch, HEART, x, y, v >= 2 ? 2 : v == 1 ? 1 : 0, fill);
				} else {
					int v = absorb - (i - healthHearts) * 2;
					drawIcon(batch, HEART, x, y, v >= 2 ? 2 : 1, 0xFFFFD23C);
				}
			}
			batch.flush();
		}
	}

	/** The armor icons above your health in your colour. */
	public static class ArmorBar extends Module {
		final ColorSetting color = add(new ColorSetting("Color", "Colour for the armor icons", 0xFF4EB1FF, true));

		public ArmorBar() {
			super("Armor Bar", "Your armor icons in your own colour, over any texture pack", Category.VISUAL, false);
			icon = com.tatnat.client.ui.render.Icons.Icon.SHIELD;
			HudColors.armor = this;
		}

		@Subscribe
		public void onRender(Events.Render2D e) {
			float[] s = TatnatClient.game().barStats();
			if (s == null || game().hudHidden() || s[3] <= 0) return;
			com.tatnat.client.ui.render.RectBatch batch = com.tatnat.client.ui.render.RectBatch.of(e.gfx);
			int[] rows = heartRows(s);
			int left = game().guiWidth() / 2 - 91, y = game().guiHeight() - 39 - (rows[0] - 1) * rows[1] - 10;
			int armorValue = (int) s[3];
			for (int i = 0; i < 10; i++) {
				int v = armorValue - i * 2;
				drawIcon(batch, CHEST, left + i * 8, y, v >= 2 ? 2 : v == 1 ? 1 : 0, color.color(i * 0.05));
			}
			batch.flush();
		}
	}
}
