package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.ui.render.Icons;

/**
 * Your position, one value per line, plus the way you're facing and the biome you're in:
 * <pre>
 * X: 124
 * Y: 64
 * Z: -453
 * F: N (-Z)
 * B: Dark Forest
 * </pre>
 */
public class Coordinates extends HudModule {
	private final BooleanSetting direction = add(new BooleanSetting("Direction", "Show which way you're facing (N/S/E/W)", true));
	private final BooleanSetting biome = add(new BooleanSetting("Biome", "Show the biome you're in", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the text", true));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the text", true));
	private final ColorSetting labelColor = add(new ColorSetting("Label Color", "Colour of X / Y / Z", 0xFF4EB1FF, true));
	private final ColorSetting valueColor = add(new ColorSetting("Value Color", "Colour of the numbers", 0xFFFFFFFF, true));

	private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private static final String[] AXES = {"+Z", "+Z -X", "-X", "-X -Z", "-Z", "-Z +X", "+X", "+X +Z"};

	public Coordinates() {
		super("Coordinates", "Shows your X / Y / Z position, facing and biome", false, 0.38, 0.40);
		icon = Icons.Icon.MAP;
	}

	/** "minecraft:dark_forest" -> "Dark Forest"; old versions already give a plain name. */
	static String prettyBiome(String id) {
		if (id == null || id.isEmpty()) return "";
		String s = id.substring(id.indexOf(':') + 1).replace('_', ' ');
		if (!id.contains(":") && !id.contains("_")) return s;
		StringBuilder out = new StringBuilder();
		for (String w : s.split(" ")) {
			if (w.isEmpty()) continue;
			if (out.length() > 0) out.append(' ');
			out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return out.toString();
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		int x = 0, y = 0, z = 0, dir = 4;
		String biomeName = "";
		if (game().inWorld()) {
			x = (int) Math.floor(game().x());
			y = (int) Math.floor(game().y());
			z = (int) Math.floor(game().z());
			dir = Math.floorMod(Math.round(game().yaw() / 45f), 8);
			try {
				biomeName = prettyBiome(game().biome());
			} catch (RuntimeException e) {
				biomeName = "";
			}
		} else if (preview) {
			x = 124;
			y = 64;
			z = -453;
			biomeName = "Dark Forest";
		}
		java.util.List<String[]> lines = new java.util.ArrayList<>();
		lines.add(new String[] {"X: ", String.valueOf(x)});
		lines.add(new String[] {"Y: ", String.valueOf(y)});
		lines.add(new String[] {"Z: ", String.valueOf(z)});
		if (direction.on()) lines.add(new String[] {"F: ", DIRS[dir] + " (" + AXES[dir] + ")"});
		if (biome.on() && !biomeName.isEmpty()) lines.add(new String[] {"B: ", biomeName});
		int pad = background.on() ? 5 : 0;

		// Measure first so the background can go underneath.
		int max = 0;
		for (String[] p : lines) max = Math.max(max, g.mcTextWidth(p[0] + p[1], false));
		int w = max + pad * 2, h = lines.size() * 10 - 2 + pad * 2;
		if (background.on()) g.rect(0, 0, w, h, 0x6F000000);

		int cy = pad;
		for (int i = 0; i < lines.size(); i++) {
			int cx = drawPart(g, lines.get(i)[0], pad, cy, labelColor.color(i * 0.1));
			drawPart(g, lines.get(i)[1], cx, cy, valueColor.color(i * 0.1 + 0.05));
			cy += 10;
		}
		return size(w, h);
	}

	private int drawPart(Gfx g, String s, int x, int y, int color) {
		g.mcText(s, x, y, color, shadow.on(), false);
		return x + g.mcTextWidth(s, false);
	}
}
