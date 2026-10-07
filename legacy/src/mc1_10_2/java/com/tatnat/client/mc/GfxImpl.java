package com.tatnat.client.mc;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayDeque;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Gfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * {@link Gfx} for the legacy versions: GUI drawing on the global GL matrix, with a {@link Pose2D}
 * for the shared code's push/translate/scale applied around every draw.
 */
public final class GfxImpl implements Gfx {
	private static final Identifier LOGO = new Identifier(TatnatClient.ID, "logo_legacy");
	private static final Identifier INVENTORY = new Identifier("textures/gui/container/inventory.png");
	private static final ArrayDeque<int[]> CLIPS = new ArrayDeque<>();
	private static boolean logoLoaded;

	private final MinecraftClient mc = MinecraftClient.getInstance();
	private final Pose2D pose = new Pose2D();

	private void begin() {
		GlStateManager.pushMatrix();
		pose.apply();
		// World and item rendering can leave GL lighting on, which greys out flat GUI fills.
		GlStateManager.disableLighting();
	}

	private void end() {
		GlStateManager.popMatrix();
	}

	/** Bold Minecraft-font text: formatting code. */
	static String mcString(String s, boolean bold) {
		return bold ? "§l" + s : s;
	}

	@Override
	public void rect(int x1, int y1, int x2, int y2, int argb) {
		begin();
		DrawableHelper.fill(x1, y1, x2, y2, argb);
		end();
	}

	/** Many fills in one draw call (RectBatch): data = {x1, y1, x2, y2, argb} * n. */
	@Override
	public void rects(int[] d, int n) {
		begin();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();
		b.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
		for (int i = 0; i < n; i++) {
			int o = i * 5, c = d[o + 4];
			int a = c >>> 24, r = c >> 16 & 255, gr = c >> 8 & 255, bl = c & 255;
			b.vertex(d[o], d[o + 3], 0).color(r, gr, bl, a).next();
			b.vertex(d[o + 2], d[o + 3], 0).color(r, gr, bl, a).next();
			b.vertex(d[o + 2], d[o + 1], 0).color(r, gr, bl, a).next();
			b.vertex(d[o], d[o + 1], 0).color(r, gr, bl, a).next();
		}
		t.draw();
		GlStateManager.enableTexture();
		GlStateManager.disableBlend();
		end();
	}

	@Override
	public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
		begin();
		GlStateManager.disableTexture();
		GlStateManager.enableBlend();
		GlStateManager.disableAlphaTest();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();
		b.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
		b.vertex(x2, y1, 0).color((top >> 16) & 255, (top >> 8) & 255, top & 255, top >>> 24).next();
		b.vertex(x1, y1, 0).color((top >> 16) & 255, (top >> 8) & 255, top & 255, top >>> 24).next();
		b.vertex(x1, y2, 0).color((bottom >> 16) & 255, (bottom >> 8) & 255, bottom & 255, bottom >>> 24).next();
		b.vertex(x2, y2, 0).color((bottom >> 16) & 255, (bottom >> 8) & 255, bottom & 255, bottom >>> 24).next();
		t.draw();
		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.disableBlend();
		GlStateManager.enableAlphaTest();
		GlStateManager.enableTexture();
		end();
	}

	@Override
	public void push() {
		pose.pushPose();
	}

	@Override
	public void pop() {
		pose.popPose();
	}

	@Override
	public void translate(float x, float y) {
		pose.translate(x, y, 0f);
	}

	@Override
	public void scale(float x, float y) {
		pose.scale(x, y, 1f);
	}

	@Override
	public void scissor(int x1, int y1, int x2, int y2) {
		// GUI units through the current pose, nested boxes intersect.
		int[] r = {(int) Math.floor(Math.min(pose.mapX(x1), pose.mapX(x2))), (int) Math.floor(Math.min(pose.mapY(y1), pose.mapY(y2))),
				(int) Math.ceil(Math.max(pose.mapX(x1), pose.mapX(x2))), (int) Math.ceil(Math.max(pose.mapY(y1), pose.mapY(y2)))};
		int[] top = CLIPS.peek();
		if (top != null) r = new int[] {Math.max(r[0], top[0]), Math.max(r[1], top[1]), Math.min(r[2], top[2]), Math.min(r[3], top[3])};
		CLIPS.push(r);
		applyScissor(r);
	}

	@Override
	public void endScissor() {
		CLIPS.poll();
		int[] top = CLIPS.peek();
		if (top == null) GL11.glDisable(GL11.GL_SCISSOR_TEST);
		else applyScissor(top);
	}

	private void applyScissor(int[] r) {
		int s = new Window(mc).getScaleFactor();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(r[0] * s, mc.height - r[3] * s, Math.max(0, (r[2] - r[0]) * s), Math.max(0, (r[3] - r[1]) * s));
	}

	@Override
	public void uiText(int weight, int px, String s, int x, int y, int argb) {
		begin();
		// Baseline at ~80% of the em box, like the TrueType versions.
		LegacyFont.get(weight, px).draw(s, x, y + Math.round(px * 0.8f), argb);
		end();
	}

	@Override
	public int uiTextWidth(int weight, int px, String s) {
		return LegacyFont.get(weight, px).width(s);
	}

	@Override
	public void mcText(String s, int x, int y, int argb, boolean shadow, boolean bold) {
		begin();
		mc.textRenderer.draw(mcString(s, bold), x, y, argb, shadow);
		GlStateManager.color(1f, 1f, 1f, 1f);
		end();
	}

	@Override
	public int mcTextWidth(String s, boolean bold) {
		return mc.textRenderer.getStringWidth(mcString(s, bold));
	}

	/** Mod assets aren't in the game's resource packs here (no resource loader), so load the logo directly. */
	private void loadLogo() {
		logoLoaded = true;
		try (InputStream in = GfxImpl.class.getResourceAsStream("/assets/tatnatclient/logo.png")) {
			BufferedImage img = ImageIO.read(in);
			mc.getTextureManager().loadTexture(LOGO, new NativeImageBackedTexture(img));
		} catch (Exception e) {
			TatnatClient.LOG.warn("Could not load the logo: {}", e.toString());
		}
	}

	@Override
	public void logo(int x, int y, int size, int argb) {
		if (!logoLoaded) loadLogo();
		begin();
		GlStateManager.translate(x, y, 0f);
		GlStateManager.scale(size / 64f, size / 64f, 1f);
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.color(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
		mc.getTextureManager().bindTexture(LOGO);
		DrawableHelper.drawTexture(0, 0, 0f, 0f, 64, 64, 64f, 64f);
		GlStateManager.color(1f, 1f, 1f, 1f);
		end();
	}

	/** Uploaded coverage masks (icons, rounded corners) by key. */
	private static final java.util.Map<String, Identifier> MASKS = new java.util.HashMap<>();

	@Override
	public boolean masks() {
		return true;
	}

	@Override
	public void mask(String key, int w, int h, java.util.function.Supplier<byte[]> alpha, int x, int y, int argb) {
		Identifier id = MASKS.get(key);
		if (id == null) {
			byte[] a = alpha.get();
			BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			for (int i = 0; i < w * h; i++) img.setRGB(i % w, i / w, ((a[i] & 255) << 24) | 0xFFFFFF);
			id = new Identifier(TatnatClient.ID, "mask/" + MASKS.size());
			mc.getTextureManager().loadTexture(id, new NativeImageBackedTexture(img));
			MASKS.put(key, id);
		}
		begin();
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.color(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
		mc.getTextureManager().bindTexture(id);
		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();
		b.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
		b.vertex(x, y + h, 0).texture(0, 1).next();
		b.vertex(x + w, y + h, 0).texture(1, 1).next();
		b.vertex(x + w, y, 0).texture(1, 0).next();
		b.vertex(x, y, 0).texture(0, 0).next();
		t.draw();
		GlStateManager.color(1f, 1f, 1f, 1f);
		end();
	}

	@Override
	public void item(Object stack, int x, int y) {
		ItemStack s = (ItemStack) stack;
		begin();
		GlStateManager.enableRescaleNormal();
		DiffuseLighting.enable();
		mc.getItemRenderer().method_10249(mc.player, s, x, y);
		mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, s, x, y, "");
		DiffuseLighting.disable();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.color(1f, 1f, 1f, 1f);
		end();
	}

	@Override
	public void effectIcon(Object effect, int x, int y, int size) {
		StatusEffect e = (StatusEffect) effect;
		if (!e.hasIcon()) return;
		int i = e.getIconLevel();
		begin();
		GlStateManager.translate(x, y, 0f);
		GlStateManager.scale(size / 18f, size / 18f, 1f);
		GlStateManager.enableBlend();
		GlStateManager.color(1f, 1f, 1f, 1f);
		mc.getTextureManager().bindTexture(INVENTORY);
		// Effect icons are an 8-wide grid of 18px squares starting at (0, 198) in the inventory texture.
		DrawableHelper.drawTexture(0, 0, i % 8 * 18, 198 + i / 8 * 18, 18, 18, 256f, 256f);
		end();
	}
}
