package com.tatnat.client.modules.impl.cosmetic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.NativeImage;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.ClientAsset;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * A cape on your own player, drawn on your screen only (other players can't see client-side
 * capes; for a real cape everyone sees, use the launcher's Skins tab). Pick a built-in design or
 * drop 64x32 cape PNGs into the capes folder. Physics scales how much the cape swings.
 */
public class CustomCapes extends Module {
	public static CustomCapes INSTANCE;

	private static final String[] BUILT_IN = {"tatnat", "Crimson", "Midnight", "Ocean", "Forest", "Gold"};
	private static final Path FOLDER = FabricLoader.getInstance().getConfigDir().resolve("tatnat-client").resolve("capes");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(TatnatClient.ID, "cape/current");

	public final ModeSetting cape = add(new ModeSetting("Cape", "Built-in design or a PNG from your capes folder", "tatnat", BUILT_IN));
	public final SliderSetting physics = add(new SliderSetting("Physics", "How much the cape swings as you move", 100, 0, 250, 5, "%"));
	private final ActionSetting openFolder = add(new ActionSetting("Your Own Capes", "Put 64x32 cape PNGs in this folder, then press Reload",
			() -> "Open folder", this::openFolder));
	private final ActionSetting reload = add(new ActionSetting("Reload", "Look for new cape files", () -> "Reload", this::rescan));

	private String loaded;
	private ClientAsset.Texture asset;

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
		List<String> modes = new ArrayList<>(List.of(BUILT_IN));
		try {
			Files.createDirectories(FOLDER);
			try (Stream<Path> files = Files.list(FOLDER)) {
				files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
						.map(p -> "File: " + p.getFileName().toString())
						.sorted().forEach(modes::add);
			}
		} catch (IOException e) {
			TatnatClient.LOG.warn("Could not read capes folder", e);
		}
		cape.setModes(modes);
		loaded = null;
	}

	private void openFolder() {
		try {
			Files.createDirectories(FOLDER);
		} catch (IOException ignored) {
		}
		Util.getPlatform().openPath(FOLDER);
	}

	/** Your skin with the chosen cape (and matching elytra) swapped in. */
	public PlayerSkin apply(PlayerSkin skin) {
		ClientAsset.Texture t = texture();
		if (t == null) return skin;
		return new PlayerSkin(skin.body(), t, t, skin.model(), skin.secure());
	}

	private ClientAsset.Texture texture() {
		if (cape.get().equals(loaded)) return asset;
		loaded = cape.get();
		NativeImage img = null;
		try {
			img = cape.get().startsWith("File: ") ? fromFile(cape.get().substring(6)) : CapeArt.draw(cape.get());
		} catch (Exception e) {
			TatnatClient.LOG.warn("Could not load cape {}", cape.get(), e);
		}
		if (img == null) {
			asset = null;
			return null;
		}
		DynamicTexture tex = new DynamicTexture(() -> "tatnat cape", img);
		mc.getTextureManager().register(TEXTURE, tex);
		asset = new ClientAsset.ResourceTexture(TEXTURE, TEXTURE);
		return asset;
	}

	private static NativeImage fromFile(String name) throws IOException {
		try (InputStream in = Files.newInputStream(FOLDER.resolve(name))) {
			NativeImage raw = NativeImage.read(in);
			// Old 22x17 capes: copy onto a proper 64x32 canvas.
			if (raw.getWidth() < 64) {
				NativeImage fixed = new NativeImage(64, 32, true);
				for (int y = 0; y < Math.min(32, raw.getHeight()); y++)
					for (int x = 0; x < Math.min(64, raw.getWidth()); x++) fixed.setPixel(x, y, raw.getPixel(x, y));
				raw.close();
				return fixed;
			}
			return raw;
		}
	}
}
