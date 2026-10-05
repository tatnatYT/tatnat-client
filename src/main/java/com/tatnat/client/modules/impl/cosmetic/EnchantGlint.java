package com.tatnat.client.modules.impl.cosmetic;

import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.theme.Colors;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Recolours the enchantment shimmer. Vanilla's purple glint textures are re-tinted with your
 * colour (brightness kept) and registered in their place; turning the mod off releases them so
 * vanilla's own textures load again. Speed multiplies vanilla's glint speed
 * ({@code TextureTransformMixin}).
 */
public class EnchantGlint extends Module {
	public static EnchantGlint INSTANCE;

	private static final Identifier[] TEXTURES = {
			Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png"),
			Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png")};

	private final ColorSetting color = add(new ColorSetting("Color", "Glint colour (Chroma cycles the rainbow)", 0xFFE5323E, true));
	public final SliderSetting speed = add(new SliderSetting("Speed", "How fast the shimmer moves", 1.0, 0.1, 5, 0.1, "x"));

	private final NativeImage[] originals = new NativeImage[TEXTURES.length];
	private final DynamicTexture[] tinted = new DynamicTexture[TEXTURES.length];
	private int appliedColor;
	private int frame;

	public EnchantGlint() {
		super("Enchant Glint", "Change the colour and speed of the enchant shimmer", Category.COSMETIC, false);
		icon = Icons.Icon.SPARKLE;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		// Chroma needs a re-tint a few times a second; a fixed colour only when it changes.
		if (color.chroma() && ++frame % 2 != 0) return;
		int c = color.color() | 0xFF000000;
		if (c != appliedColor) tint(c);
	}

	@Override
	protected void onEnable() {
		appliedColor = 0;
		tint(color.color() | 0xFF000000);
	}

	@Override
	protected void onDisable() {
		for (int i = 0; i < TEXTURES.length; i++) {
			if (tinted[i] != null) {
				mc.getTextureManager().release(TEXTURES[i]);
				tinted[i] = null;
			}
		}
		appliedColor = 0;
	}

	private void tint(int argb) {
		for (int i = 0; i < TEXTURES.length; i++) {
			try {
				if (originals[i] == null) {
					try (InputStream in = mc.getResourceManager().open(TEXTURES[i])) {
						originals[i] = NativeImage.read(in);
					}
				}
				NativeImage src = originals[i];
				if (tinted[i] == null) {
					tinted[i] = new DynamicTexture(() -> "tatnat glint", new NativeImage(src.getWidth(), src.getHeight(), true));
					mc.getTextureManager().register(TEXTURES[i], tinted[i]);
				}
				NativeImage dst = tinted[i].getPixels();
				float r = Colors.red(argb) / 255f, g = Colors.green(argb) / 255f, b = Colors.blue(argb) / 255f;
				for (int y = 0; y < src.getHeight(); y++) {
					for (int x = 0; x < src.getWidth(); x++) {
						int p = src.getPixel(x, y);
						// Brightness of the original purple shimmer drives the new colour.
						float lum = Math.max(Colors.red(p), Math.max(Colors.green(p), Colors.blue(p))) / 255f;
						dst.setPixel(x, y, Colors.argb(Colors.alpha(p), Math.round(r * lum * 255), Math.round(g * lum * 255), Math.round(b * lum * 255)));
					}
				}
				tinted[i].upload();
			} catch (Exception ex) {
				TatnatClient.LOG.warn("Could not tint {}", TEXTURES[i], ex);
			}
		}
		appliedColor = argb;
	}
}
