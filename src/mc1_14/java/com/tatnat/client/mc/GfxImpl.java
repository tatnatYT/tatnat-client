package com.tatnat.client.mc;

import java.util.HashMap;
import java.util.Map;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Gfx;

import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

/** {@link Gfx} on top of the 1.14 - 1.15 Graphics shim. One per frame (it just wraps the graphics). */
public final class GfxImpl implements Gfx {
	private static final ResourceLocation LOGO = new ResourceLocation(TatnatClient.ID, "logo.png");

	private final Graphics g;
	private final Minecraft mc = Minecraft.getInstance();

	public GfxImpl(Graphics g) {
		this.g = g;
	}

	private static final Map<Integer, ResourceLocation> UI_FONTS = new HashMap<>();

	/**
	 * Inter at an exact pixel size: one font definition per size (assets/tatnatclient/font). Text
	 * styles can't pick a font before 1.16, so each size is its own Font.
	 */
	static Font uiFont(int weight, int px) {
		ResourceLocation id = UI_FONTS.computeIfAbsent(weight * 100 + px, k -> new ResourceLocation(TatnatClient.ID, "w" + weight + "_" + px));
		return Minecraft.getInstance().getFontManager().get(id);
	}

	/** Bold Minecraft-font text: formatting code, as text styles only came with 1.16. */
	static String mcString(String s, boolean bold) {
		return bold ? "\u00a7l" + s : s;
	}

	@Override
	public void rect(int x1, int y1, int x2, int y2, int argb) {
		g.fill(x1, y1, x2, y2, argb);
	}

	@Override
	public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
		g.fillGradient(x1, y1, x2, y2, top, bottom);
	}

	@Override
	public void push() {
		g.pose().pushPose();
	}

	@Override
	public void pop() {
		g.pose().popPose();
	}

	@Override
	public void translate(float x, float y) {
		g.pose().translate(x, y, 0f);
	}

	@Override
	public void scale(float x, float y) {
		g.pose().scale(x, y, 1f);
	}

	@Override
	public void scissor(int x1, int y1, int x2, int y2) {
		// This version's scissor ignores the pose; the menu draws at 1/guiScale, so map the box first.
		Pose2D p = g.pose();
		float ax = p.mapX(x1), ay = p.mapY(y1), bx = p.mapX(x2), by = p.mapY(y2);
		g.enableScissor((int) Math.floor(Math.min(ax, bx)), (int) Math.floor(Math.min(ay, by)),
				(int) Math.ceil(Math.max(ax, bx)), (int) Math.ceil(Math.max(ay, by)));
	}

	@Override
	public void endScissor() {
		g.disableScissor();
	}

	@Override
	public void uiText(int weight, int px, String s, int x, int y, int argb) {
		// stb_truetype (before 1.20.5) puts the baseline at y - 3 + ascent (Inter: 0.969 em); move it
		// to ~80% of the em box like the FreeType versions.
		g.drawString(uiFont(weight, px), s, x, y + Math.round(3 - px * 0.169f), argb, false);
	}

	@Override
	public int uiTextWidth(int weight, int px, String s) {
		return uiFont(weight, px).width(s);
	}

	@Override
	public void mcText(String s, int x, int y, int argb, boolean shadow, boolean bold) {
		g.drawString(mc.font, mcString(s, bold), x, y, argb, shadow);
	}

	@Override
	public int mcTextWidth(String s, boolean bold) {
		return mc.font.width(mcString(s, bold));
	}

	@Override
	public void logo(int x, int y, int size, int argb) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0f);
		g.pose().scale(size / 64f, size / 64f, 1f);
		g.setColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
		g.blit(LOGO, 0, 0, 0f, 0f, 64, 64, 64, 64);
		g.setColor(1f, 1f, 1f, 1f);
		g.pose().popPose();
	}

	private static final java.util.Map<String, ResourceLocation> IMAGES = new java.util.HashMap<>();

	@Override
	public void image(String key, java.util.function.Supplier<byte[]> png, int x, int y, int size) {
		int argb = 0xFFFFFFFF;
		ResourceLocation id = IMAGES.get(key);
		if (id == null) {
			if (IMAGES.containsKey(key)) return;
			byte[] b = png.get();
			if (b == null) {
				IMAGES.put(key, null);
				return;
			}
			try {
				com.mojang.blaze3d.platform.NativeImage img = com.mojang.blaze3d.platform.NativeImage.read(new java.io.ByteArrayInputStream(b));
				id = new ResourceLocation(TatnatClient.ID, "image/" + IMAGES.size());
				net.minecraft.client.Minecraft.getInstance().getTextureManager().register(id, new net.minecraft.client.renderer.texture.DynamicTexture(img));
			} catch (Exception e) {
				IMAGES.put(key, null);
				return;
			}
			IMAGES.put(key, id);
		}
		g.pose().pushPose();
		g.pose().translate(x, y, 0f);
		g.pose().scale(size / 64f, size / 64f, 1f);
		g.setColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
		g.blit(id, 0, 0, 0f, 0f, 64, 64, 64, 64);
		g.setColor(1f, 1f, 1f, 1f);
		g.pose().popPose();
	}

	@Override
	public void item(Object stack, int x, int y) {
		ItemStack s = (ItemStack) stack;
		g.renderItem(s, x, y);
		g.renderItemDecorations(mc.font, s, x, y, "");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void effectIcon(Object effect, int x, int y, int size) {
		g.blit(x, y, 0, size, size, mc.getMobEffectTextures().get((MobEffect) effect));
	}
}
