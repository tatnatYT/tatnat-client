package com.tatnat.client.modules.impl.hud;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.Gfx;

/**
 * The texture pack you're using, with its icon: the top one, or every pack that's on. Long
 * names wrap onto the next line instead of running across the screen.
 */
public class PackDisplay extends HudModule {
	private static final int ICON = 16, MAX_TEXT = 110, LINE = 10;

	private final BooleanSetting all = add(new BooleanSetting("Show All", "List every pack that's on, not just the top one", false));
	private final BooleanSetting icons = add(new BooleanSetting("Show Icon", "The pack's picture next to its name", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind it", true));

	public PackDisplay() {
		super("Pack Display", "Shows which texture pack is active, with its icon", false, 0.75, 0.30);
		icon = com.tatnat.client.ui.render.Icons.Icon.PALETTE;
	}

	private static String clean(String id) {
		String s = id.startsWith("file/") ? id.substring(5) : id;
		if (s.toLowerCase(Locale.ROOT).endsWith(".zip")) s = s.substring(0, s.length() - 4);
		// Strip Minecraft colour codes some packs put in their file names.
		return s.replaceAll("§.", "").trim();
	}

	private static boolean builtIn(String id) {
		return id.equals("vanilla") || id.equals("fabric") || id.startsWith("fabric") || id.equals("mod_resources")
				|| id.equals("programmer_art") || id.equals("high_contrast") || id.startsWith("minecraft");
	}

	/** The pack's pack.png scaled to 64x64 as PNG bytes, or null (built-in pack, no icon, unreadable). */
	private byte[] iconBytes(String id) {
		if (!id.startsWith("file/")) return null;
		try {
			Path pack = game().configDir().getParent().resolve("resourcepacks").resolve(id.substring(5));
			BufferedImage src = null;
			if (Files.isDirectory(pack)) {
				Path png = pack.resolve("pack.png");
				if (Files.exists(png)) try (InputStream in = Files.newInputStream(png)) {
					src = ImageIO.read(in);
				}
			} else if (Files.exists(pack)) {
				try (ZipFile zip = new ZipFile(pack.toFile())) {
					ZipEntry e = zip.getEntry("pack.png");
					if (e != null) try (InputStream in = zip.getInputStream(e)) {
						src = ImageIO.read(in);
					}
				}
			}
			if (src == null) return null;
			BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = out.createGraphics();
			// Pixel-art icons stay crisp; big icons get smoothed down.
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, src.getWidth() <= 64
					? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.drawImage(src, 0, 0, 64, 64, null);
			g.dispose();
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ImageIO.write(out, "png", bytes);
			return bytes.toByteArray();
		} catch (Exception e) {
			return null;
		}
	}

	/** Splits {@code s} into lines no wider than {@code max}, at spaces where possible. */
	private static List<String> wrap(Gfx g, String s, int max) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : s.split(" ")) {
			String next = line.length() == 0 ? word : line + " " + word;
			if (g.mcTextWidth(next, false) <= max) {
				line.setLength(0);
				line.append(next);
				continue;
			}
			if (line.length() > 0) lines.add(line.toString());
			// A single word that's too long on its own gets cut.
			String w = word;
			while (g.mcTextWidth(w, false) > max && w.length() > 1) {
				int cut = w.length();
				while (cut > 1 && g.mcTextWidth(w.substring(0, cut), false) > max) cut--;
				lines.add(w.substring(0, cut));
				w = w.substring(cut);
			}
			line.setLength(0);
			line.append(w);
		}
		if (line.length() > 0) lines.add(line.toString());
		return lines;
	}

	@Override
	protected long draw(Gfx g, boolean preview) {
		List<String> ids = new ArrayList<>();
		if (preview) {
			ids.add("Faithful 32x - the long name of a pack");
		} else {
			List<String> packs = game().resourcePacks();
			for (int i = packs.size() - 1; i >= 0; i--) {
				if (builtIn(packs.get(i))) continue;
				ids.add(packs.get(i));
				if (!all.on()) break;
			}
		}
		if (ids.isEmpty()) ids.add("");

		int pad = background.on() ? 4 : 0;
		boolean showIcon = icons.on();
		int textX = pad + (showIcon ? ICON + 5 : 0);
		// Lay out first so the background can go underneath.
		List<List<String>> texts = new ArrayList<>();
		int w = 0, h = pad;
		for (String id : ids) {
			List<String> lines = wrap(g, id.isEmpty() ? "Default" : preview ? id : clean(id), MAX_TEXT);
			texts.add(lines);
			int tw = 0;
			for (String l : lines) tw = Math.max(tw, g.mcTextWidth(l, false));
			w = Math.max(w, textX + tw + pad);
			h += Math.max(showIcon ? ICON : 0, lines.size() * LINE - 1) + 4;
		}
		h += pad - 4;
		if (background.on()) g.rect(0, 0, w, h, 0x6F000000);

		int y = pad;
		for (int i = 0; i < ids.size(); i++) {
			String id = ids.get(i);
			List<String> lines = texts.get(i);
			int rowH = Math.max(showIcon ? ICON : 0, lines.size() * LINE - 1);
			if (showIcon) {
				if (id.startsWith("file/")) g.image("pack:" + id, () -> iconBytes(id), pad, y + (rowH - ICON) / 2, ICON);
				else g.rect(pad, y + (rowH - ICON) / 2, pad + ICON, y + (rowH - ICON) / 2 + ICON, 0x40FFFFFF);
			}
			int ty = y + (rowH - (lines.size() * LINE - 1)) / 2;
			for (String l : lines) {
				g.mcText(l, textX, ty, 0xFFFFFFFF, true, false);
				ty += LINE;
			}
			y += rowH + 4;
		}
		return size(w, h);
	}
}
