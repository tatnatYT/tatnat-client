package com.tatnat.client.modules.impl.cosmetic;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;


/**
 * A cape on your own player, drawn on your screen only (other players can't see client-side
 * capes; for a real cape everyone sees, use the launcher's Skins tab). Pick a built-in design or
 * drop cape PNGs (64x32 or any HD size, e.g. 512x256) into the capes folder. Physics scales how much the cape swings.
 */
public class CustomCapes extends Module {
	public static CustomCapes INSTANCE;

	private static final String[] BUILT_IN = CapeArt.DESIGNS;
	private static Path folder() {
		return game().configDir().resolve("tatnat-client").resolve("capes");
	}

	public final ModeSetting cape = add(new ModeSetting("Cape", "Built-in design or a PNG from your capes folder", "Signature", BUILT_IN));
	public final ColorSetting mainColor = add(new ColorSetting("Custom Color", "Colour of the Custom cape (Chroma cycles the rainbow)", 0xFF2F6FE0, true));
	public final ColorSetting accentColor = add(new ColorSetting("Custom Accent", "Logo and hem of the Custom cape", 0xFFE0E0E0, true));
	public final SliderSetting physics = add(new SliderSetting("Physics", "How much the cape swings as you move", 100, 0, 250, 5, "%"));
	private final ActionSetting openFolder = add(new ActionSetting("Your Own Capes", "Put cape PNGs (64x32 or HD) in this folder, then press Reload",
			() -> "Open folder", this::openFolder));
	private final ActionSetting reload = add(new ActionSetting("Reload", "Look for new cape files", () -> "Reload", this::rescan));

	/** Bumped whenever the picture changes so platforms know to re-upload their texture. */
	private int revision;
	private String pixelsFor;
	private int[] pixels;

	public CustomCapes() {
		super("Custom Capes", "Wear a cape (visible to you)", Category.COSMETIC, false);
		icon = Icons.Icon.CAPE;
		INSTANCE = this;
		rescan();
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	private void rescan() {
		List<String> modes = new ArrayList<>(Arrays.asList(BUILT_IN));
		List<String> files = new ArrayList<>();
		try {
			Files.createDirectories(folder());
			try (DirectoryStream<Path> dir = Files.newDirectoryStream(folder())) {
				for (Path p : dir) {
					String n = p.getFileName().toString();
					if (n.toLowerCase(Locale.ROOT).endsWith(".png")) files.add("File: " + n);
				}
			}
		} catch (IOException e) {
			TatnatClient.LOG.warn("Could not read capes folder: {}", e.toString());
		}
		Collections.sort(files);
		modes.addAll(files);
		cape.setModes(modes);
		pixelsFor = null;
	}

	private void openFolder() {
		try {
			Files.createDirectories(folder());
		} catch (IOException ignored) {
		}
		game().openPath(folder());
	}

	/** Change counter for the current picture (platforms re-upload when it changes). */
	public int revision() {
		pixels();
		return revision;
	}

	/** The chosen cape as CapeArt.W x CapeArt.H ARGB pixels (see CapeArt), or null if it couldn't be loaded. */
	public int[] pixels() {
		String design = cape.get();
		boolean custom = design.equals("Custom");
		boolean anim = CapeArt.animated(design, mainColor.chroma() || accentColor.chroma());
		long now = System.currentTimeMillis();
		// Animated capes are re-drawn ~10 times a second; the rest only when a setting changes.
		String key = design + (custom ? "|" + mainColor.get() + "|" + accentColor.get() : "") + (anim ? "|" + now / 100 : "");
		if (key.equals(pixelsFor)) return pixels;
		pixelsFor = key;
		revision++;
		try {
			if (cape.get().startsWith("File: ")) {
				BufferedImage img = ImageIO.read(folder().resolve(cape.get().substring(6)).toFile());
				pixels = img == null ? null : CapeArt.fromImage(img);
			} else {
				pixels = CapeArt.draw(design, mainColor.color(0), accentColor.color(0.5), now);
			}
		} catch (Exception e) {
			TatnatClient.LOG.warn("Could not load cape {}: {}", cape.get(), e.toString());
			pixels = null;
		}
		return pixels;
	}
}
