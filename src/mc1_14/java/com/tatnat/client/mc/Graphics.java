package com.tatnat.client.mc;

import java.util.ArrayDeque;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1.14 GUI drawing works on the global GL matrix. This keeps a {@link Pose2D} for the shared
 * code (push/translate/scale) and applies it to the GL matrix around every draw.
 */
public final class Graphics {
	/** Only here to reach GuiComponent's protected gradient fill. */
	private static final class Gradients extends GuiComponent {
		void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
			fillGradient(x1, y1, x2, y2, top, bottom);
		}
	}

	private static final Gradients GRADIENTS = new Gradients();
	private static final ArrayDeque<int[]> CLIPS = new ArrayDeque<>();

	private final Pose2D pose = new Pose2D();
	private final Minecraft mc = Minecraft.getInstance();

	public Pose2D pose() {
		return pose;
	}

	public int guiWidth() {
		return mc.window.getGuiScaledWidth();
	}

	public int guiHeight() {
		return mc.window.getGuiScaledHeight();
	}

	private void begin() {
		GlStateManager.pushMatrix();
		pose.apply();
		// World and item rendering can leave GL lighting on, which greys out flat GUI fills.
		GlStateManager.disableLighting();
	}

	private void end() {
		GlStateManager.popMatrix();
	}

	public void fill(int x1, int y1, int x2, int y2, int argb) {
		begin();
		GuiComponent.fill(x1, y1, x2, y2, argb);
		end();
	}

	public void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom) {
		begin();
		GRADIENTS.gradient(x1, y1, x2, y2, top, bottom);
		end();
	}

	/** Scissor in GUI units, nested boxes intersect. */
	public void enableScissor(int x1, int y1, int x2, int y2) {
		int[] r = {x1, y1, x2, y2};
		int[] top = CLIPS.peek();
		if (top != null) r = new int[] {Math.max(r[0], top[0]), Math.max(r[1], top[1]), Math.min(r[2], top[2]), Math.min(r[3], top[3])};
		CLIPS.push(r);
		applyScissor(r);
	}

	public void disableScissor() {
		CLIPS.poll();
		int[] top = CLIPS.peek();
		if (top == null) GL11.glDisable(GL11.GL_SCISSOR_TEST);
		else applyScissor(top);
	}

	private void applyScissor(int[] r) {
		double s = mc.window.getGuiScale();
		int h = mc.window.getHeight();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor((int) (r[0] * s), (int) (h - r[3] * s), Math.max(0, (int) ((r[2] - r[0]) * s)), Math.max(0, (int) ((r[3] - r[1]) * s)));
	}

	public void drawString(Font font, String s, int x, int y, int argb, boolean shadow) {
		begin();
		if (shadow) font.drawShadow(s, x, y, argb);
		else font.draw(s, x, y, argb);
		end();
	}

	public void setColor(float r, float g, float b, float a) {
		GlStateManager.color4f(r, g, b, a);
	}

	public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texW, int texH) {
		mc.getTextureManager().bind(texture);
		GlStateManager.enableBlend();
		begin();
		GuiComponent.blit(x, y, u, v, w, h, texW, texH);
		end();
	}

	public void blit(int x, int y, int z, int w, int h, TextureAtlasSprite sprite) {
		mc.getTextureManager().bind(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_MOB_EFFECTS);
		GlStateManager.enableBlend();
		begin();
		GuiComponent.blit(x, y, z, w, h, sprite);
		end();
	}

	// GUI items on 1.14 need the GUI lighting set up around them (and switched off after, or every
	// later fill comes out lit and grey), like vanilla does for hotbar slots.
	private void itemState(boolean on) {
		if (on) {
			GlStateManager.enableRescaleNormal();
			com.mojang.blaze3d.platform.Lighting.turnOnGui();
		} else {
			com.mojang.blaze3d.platform.Lighting.turnOff();
			GlStateManager.disableRescaleNormal();
			GlStateManager.disableLighting();
			GlStateManager.color4f(1f, 1f, 1f, 1f);
		}
	}

	public void renderItem(ItemStack s, int x, int y) {
		begin();
		itemState(true);
		mc.getItemRenderer().renderAndDecorateItem(s, x, y);
		itemState(false);
		end();
	}

	public void renderItemDecorations(Font font, ItemStack s, int x, int y, String text) {
		begin();
		mc.getItemRenderer().renderGuiItemDecorations(font, s, x, y, text);
		itemState(false);
		end();
	}
}
