package com.tatnat.client.mc;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Features;
import com.tatnat.client.ui.theme.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** {@link Features} for the legacy versions. */
public final class FeaturesImpl implements Features {
	public static final FeaturesImpl INSTANCE = new FeaturesImpl();

	/** Partial tick of the frame being drawn (set by GameRendererMixin). */
	public static volatile float partialTick;

	/** Hit Color for the overlay hooks; 0 = vanilla red. */
	public static volatile int hurtColor;

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private FeaturesImpl() {
	}

	@Override
	public void setHurtColor(int argb) {
		hurtColor = argb;
	}

	// ------------------------------------------------------------ Enchant Glint

	// One glint texture for items and armour on these versions.
	private static final Identifier GLINT = new Identifier("textures/misc/enchanted_item_glint.png");
	private BufferedImage glintOriginal;
	private boolean glintTinted;

	@Override
	public void tintGlint(int argb) {
		if (argb == 0) {
			if (glintTinted) {
				// Dropping our texture makes the game load the original again on next use.
				mc.getTextureManager().close(GLINT);
				glintTinted = false;
			}
			return;
		}
		try {
			if (glintOriginal == null) {
				try (InputStream in = mc.getResourceManager().getResource(GLINT).getInputStream()) {
					glintOriginal = ImageIO.read(in);
				}
			}
			BufferedImage src = glintOriginal;
			BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
			float r = Colors.red(argb) / 255f, g = Colors.green(argb) / 255f, b = Colors.blue(argb) / 255f;
			for (int y = 0; y < src.getHeight(); y++) {
				for (int x = 0; x < src.getWidth(); x++) {
					int p = src.getRGB(x, y);
					// Brightness of the original purple shimmer drives the new colour.
					float lum = Math.max(Colors.red(p), Math.max(Colors.green(p), Colors.blue(p))) / 255f;
					dst.setRGB(x, y, Colors.argb(Colors.alpha(p), Math.round(r * lum * 255), Math.round(g * lum * 255), Math.round(b * lum * 255)));
				}
			}
			mc.getTextureManager().loadTexture(GLINT, new NativeImageBackedTexture(dst));
			glintTinted = true;
		} catch (Exception ex) {
			TatnatClient.LOG.warn("Could not tint {}: {}", GLINT, ex.toString());
		}
	}

	// ------------------------------------------------------------ Freecam

	private FreeCamera camera;

	public FreeCamera freecam() {
		return camera;
	}

	@Override
	public boolean startFreecam() {
		if (mc.player == null || mc.world == null) return false;
		camera = new FreeCamera(mc.world);
		camera.refreshPositionAndAngles(mc.player.x, mc.player.y + mc.player.getEyeHeight(), mc.player.z, mc.player.yaw, mc.player.pitch);
		camera.prevX = camera.prevTickX = camera.x;
		camera.prevY = camera.prevTickY = camera.y;
		camera.prevZ = camera.prevTickZ = camera.z;
		camera.prevYaw = camera.yaw;
		camera.prevPitch = camera.pitch;
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
		if (camera.world != mc.world) {
			stopFreecam();
			return;
		}
		if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
		camera.moveBy(dx, dy, dz);
	}

	@Override
	public float freecamYaw() {
		return camera == null ? 0f : camera.yaw;
	}

	@Override
	public boolean supports(String feature) {
		// No chunk slide-in or cape physics on these versions.
		return !"chunk_animator".equals(feature) && !"cape_physics".equals(feature);
	}
}
