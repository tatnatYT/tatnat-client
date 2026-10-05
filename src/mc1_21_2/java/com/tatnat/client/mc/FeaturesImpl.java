package com.tatnat.client.mc;

import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Features;
import com.tatnat.client.ui.theme.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Version-specific parts of individual mods, for 1.21.11. */
public final class FeaturesImpl implements Features {
	public static final FeaturesImpl INSTANCE = new FeaturesImpl();

	/** Implemented by the overlay-texture mixin (Hit Color). */
	public interface HurtOverlay {
		void tatnat$setHurtColor(int argb);
	}

	public static HurtOverlay overlay;

	private final Minecraft mc = Minecraft.getInstance();

	private FeaturesImpl() {
	}

	// ------------------------------------------------------------ Hit Color

	@Override
	public void setHurtColor(int argb) {
		if (overlay != null) overlay.tatnat$setHurtColor(argb);
	}

	// ------------------------------------------------------------ Enchant Glint

	private static final ResourceLocation[] GLINT = {
			ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_item.png"),
			ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_armor.png")};
	// Sized literally: these are created with INSTANCE, before the static GLINT array exists.
	private final NativeImage[] glintOriginals = new NativeImage[2];
	private final DynamicTexture[] glintTinted = new DynamicTexture[2];

	/**
	 * Re-tints vanilla's glint textures with {@code argb} (brightness kept) and registers them in
	 * place of the originals; 0 releases them so vanilla's own load again.
	 */
	@Override
	public void tintGlint(int argb) {
		if (argb == 0) {
			for (int i = 0; i < GLINT.length; i++) {
				if (glintTinted[i] != null) {
					mc.getTextureManager().release(GLINT[i]);
					glintTinted[i] = null;
				}
			}
			return;
		}
		for (int i = 0; i < GLINT.length; i++) {
			try {
				if (glintOriginals[i] == null) {
					try (InputStream in = mc.getResourceManager().open(GLINT[i])) {
						glintOriginals[i] = NativeImage.read(in);
					}
				}
				NativeImage src = glintOriginals[i];
				if (glintTinted[i] == null) {
					glintTinted[i] = new DynamicTexture(new NativeImage(src.getWidth(), src.getHeight(), true));
					mc.getTextureManager().register(GLINT[i], glintTinted[i]);
				}
				NativeImage dst = glintTinted[i].getPixels();
				float r = Colors.red(argb) / 255f, g = Colors.green(argb) / 255f, b = Colors.blue(argb) / 255f;
				for (int y = 0; y < src.getHeight(); y++) {
					for (int x = 0; x < src.getWidth(); x++) {
						int p = src.getPixel(x, y);
						// Brightness of the original purple shimmer drives the new colour.
						float lum = Math.max(Colors.red(p), Math.max(Colors.green(p), Colors.blue(p))) / 255f;
						dst.setPixel(x, y, Colors.argb(Colors.alpha(p), Math.round(r * lum * 255), Math.round(g * lum * 255), Math.round(b * lum * 255)));
					}
				}
				glintTinted[i].upload();
			} catch (Exception ex) {
				TatnatClient.LOG.warn("Could not tint {}: {}", GLINT[i], ex.toString());
			}
		}
	}

	// ------------------------------------------------------------ Freecam

	private FreeCamera camera;

	public FreeCamera freecam() {
		return camera;
	}

	@Override
	public boolean startFreecam() {
		if (mc.player == null || mc.level == null) return false;
		camera = new FreeCamera(mc.level);
		camera.setPos(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
		camera.xo = camera.getX();
		camera.yo = camera.getY();
		camera.zo = camera.getZ();
		camera.setYRot(mc.player.getYRot());
		camera.setXRot(mc.player.getXRot());
		camera.yRotO = camera.getYRot();
		camera.xRotO = camera.getXRot();
		mc.setCameraEntity(camera);
		return true;
	}

	@Override
	public void stopFreecam() {
		camera = null;
		if (mc.player != null) mc.setCameraEntity(mc.player);
	}

	@Override
	public void moveFreecam(double dx, double dy, double dz) {
		if (camera == null) return;
		if (camera.level() != mc.level) {
			stopFreecam();
			return;
		}
		if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
		camera.moveBy(dx, dy, dz);
	}

	@Override
	public float freecamYaw() {
		return camera == null ? 0f : camera.getYRot();
	}

	@Override
	public boolean supports(String feature) {
		// Chunk Animator hooks a renderer that only exists from 1.21.5 on.
		return !"chunk_animator".equals(feature);
	}
}
