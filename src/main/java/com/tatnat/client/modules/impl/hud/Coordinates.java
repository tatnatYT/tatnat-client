package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.ModeSetting;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** {@code X: 124 / Y: 64 / Z: -453} plus the compass direction you're facing. */
public class Coordinates extends HudModule {
	private final ModeSetting layout = add(new ModeSetting("Layout", "One line or one value per line", "Horizontal", "Horizontal", "Vertical"));
	private final BooleanSetting direction = add(new BooleanSetting("Direction", "Show which way you're facing (N/S/E/W)", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the text", true));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the text", true));
	private final ColorSetting labelColor = add(new ColorSetting("Label Color", "Colour of X / Y / Z", 0xFF4EB1FF, true));
	private final ColorSetting valueColor = add(new ColorSetting("Value Color", "Colour of the numbers", 0xFFFFFFFF, true));

	private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private static final String[] AXES = {"+Z", "+Z -X", "-X", "-X -Z", "-Z", "-Z +X", "+X", "+X +Z"};

	public Coordinates() {
		super("Coordinates", "Shows your X / Y / Z position and facing", false, 0.0, 0.15);
		icon = com.tatnat.client.ui.render.Icons.Icon.MAP;
	}

	@Override
	protected long draw(GuiGraphics g, boolean preview) {
		int x = 0, y = 0, z = 0, dir = 4;
		if (mc.player != null) {
			x = Mth.floor(mc.player.getX());
			y = Mth.floor(mc.player.getY());
			z = Mth.floor(mc.player.getZ());
			dir = Math.floorMod(Math.round(mc.player.getYRot() / 45f), 8);
		} else if (preview) {
			x = 124;
			y = 64;
			z = -453;
		}
		String[][] parts = {{"X: ", String.valueOf(x)}, {"Y: ", String.valueOf(y)}, {"Z: ", String.valueOf(z)}};
		boolean vertical = layout.is("Vertical");
		int pad = background.on() ? 5 : 0;

		// Measure first so the background can go underneath.
		int w, h;
		String dirText = direction.on() ? DIRS[dir] + " (" + AXES[dir] + ")" : null;
		if (vertical) {
			int max = 0;
			for (String[] p : parts) max = Math.max(max, mc.font.width(p[0] + p[1]));
			if (dirText != null) max = Math.max(max, mc.font.width("F: " + dirText));
			int lines = dirText != null ? 4 : 3;
			w = max + pad * 2;
			h = lines * 10 - 2 + pad * 2;
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 3; i++) sb.append(parts[i][0]).append(parts[i][1]).append(i < 2 ? " / " : "");
			if (dirText != null) sb.append("  ").append(DIRS[dir]);
			w = mc.font.width(sb.toString()) + pad * 2;
			h = background.on() ? 16 : 8;
		}
		if (background.on()) g.fill(0, 0, w, h, 0x6F000000);

		int cx = pad, cy = background.on() ? (vertical ? pad : 4) : 0;
		for (int i = 0; i < 3; i++) {
			cx = drawPart(g, parts[i][0], cx, cy, labelColor.color(i * 0.1));
			cx = drawPart(g, parts[i][1], cx, cy, valueColor.color(i * 0.1 + 0.05));
			if (vertical) {
				cx = pad;
				cy += 10;
			} else if (i < 2) {
				cx = drawPart(g, " / ", cx, cy, 0xFFA0A0A0);
			}
		}
		if (dirText != null) {
			if (vertical) {
				cx = drawPart(g, "F: ", cx, cy, labelColor.color(0.3));
				drawPart(g, dirText, cx, cy, valueColor.color(0.35));
			} else {
				drawPart(g, "  " + DIRS[dir], cx, cy, labelColor.color(0.3));
			}
		}
		return size(w, h);
	}

	private int drawPart(GuiGraphics g, String s, int x, int y, int color) {
		g.drawString(mc.font, s, x, y, color, shadow.on());
		return x + mc.font.width(s);
	}
}
