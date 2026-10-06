package com.tatnat.client.mc;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tatnat.client.TatnatClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Inter for the menus on Minecraft versions without TrueType font support (before 1.13): each
 * weight/size is rasterised once with Java2D into a glyph atlas and drawn as textured quads, 1:1
 * on screen pixels like the TTF provider does on newer versions.
 */
public final class LegacyFont {
	private static final Map<Integer, LegacyFont> FONTS = new HashMap<>();
	private static final Map<Integer, Font> BASE = new HashMap<>();
	private static final int FIRST = 32, LAST = 255, SIZE = 1024;

	private final Identifier texture;
	private final int[] gx = new int[LAST + 1], gy = new int[LAST + 1], gw = new int[LAST + 1];
	private final float[] advance = new float[LAST + 1];
	private final int cellH, ascent, px;

	public static LegacyFont get(int weight, int px) {
		return FONTS.computeIfAbsent(weight * 1000 + px, k -> new LegacyFont(weight, px));
	}

	private static Font base(int weight) {
		return BASE.computeIfAbsent(weight, w -> {
			try (InputStream in = LegacyFont.class.getResourceAsStream("/assets/tatnatclient/font/inter-" + w + ".ttf")) {
				return Font.createFont(Font.TRUETYPE_FONT, in);
			} catch (Exception e) {
				TatnatClient.LOG.warn("Could not load Inter {}: {}", w, e.toString());
				return new Font(Font.SANS_SERIF, w >= 700 ? Font.BOLD : Font.PLAIN, 12);
			}
		});
	}

	private LegacyFont(int weight, int px) {
		this.px = px;
		Font font = base(weight).deriveFont((float) px);
		BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(font);
		g.setColor(Color.WHITE);
		FontMetrics fm = g.getFontMetrics();
		ascent = fm.getAscent();
		cellH = fm.getAscent() + fm.getDescent() + 2;
		int x = 1, y = 1;
		for (int c = FIRST; c <= LAST; c++) {
			if (!font.canDisplay((char) c)) continue;
			float adv = (float) font.getStringBounds(String.valueOf((char) c), g.getFontRenderContext()).getWidth();
			int w = (int) Math.ceil(adv) + 2;
			if (x + w >= SIZE) {
				x = 1;
				y += cellH + 1;
			}
			g.drawString(String.valueOf((char) c), x + 1, y + ascent);
			gx[c] = x;
			gy[c] = y;
			gw[c] = w;
			advance[c] = adv;
			x += w + 1;
		}
		g.dispose();
		texture = new Identifier(TatnatClient.ID, "font/legacy_" + weight + "_" + px);
		MinecraftClient.getInstance().getTextureManager().loadTexture(texture, new NativeImageBackedTexture(img));
	}

	private static int glyph(char c) {
		return c >= FIRST && c <= LAST ? c : '?';
	}

	public int width(String s) {
		float w = 0;
		for (int i = 0; i < s.length(); i++) w += advance[glyph(s.charAt(i))];
		return Math.round(w);
	}

	/** Draws {@code s} with its baseline at {@code baseline}. */
	public void draw(String s, float x, float baseline, int argb) {
		MinecraftClient.getInstance().getTextureManager().bindTexture(texture);
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.disableLighting();
		GlStateManager.color(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();
		b.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
		float pen = x;
		float top = baseline - ascent;
		for (int i = 0; i < s.length(); i++) {
			int c = glyph(s.charAt(i));
			float u0 = gx[c] / (float) SIZE, v0 = gy[c] / (float) SIZE;
			float u1 = (gx[c] + gw[c]) / (float) SIZE, v1 = (gy[c] + cellH) / (float) SIZE;
			// Glyphs were drawn 1px into their cell.
			float x0 = Math.round(pen) - 1, y0 = Math.round(top), x1 = x0 + gw[c], y1 = y0 + cellH;
			b.vertex(x0, y1, 0).texture(u0, v1).next();
			b.vertex(x1, y1, 0).texture(u1, v1).next();
			b.vertex(x1, y0, 0).texture(u1, v0).next();
			b.vertex(x0, y0, 0).texture(u0, v0).next();
			pen += advance[c];
		}
		t.draw();
		GlStateManager.color(1f, 1f, 1f, 1f);
	}

	public int size() {
		return px;
	}
}
