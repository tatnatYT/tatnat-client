package com.tatnat.client.mc;

import java.util.ArrayDeque;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1.14 - 1.15 GUI drawing works on the global GL matrix, not a PoseStack. This keeps a PoseStack
 * for the shared code (push/translate/scale) and applies it to the GL matrix around every draw.
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

	private final PoseStack pose = new PoseStack();
	private final Minecraft mc = Minecraft.getInstance();

	public PoseStack pose() {
		return pose;
	}

	public int guiWidth() {
		return mc.getWindow().getGuiScaledWidth();
	}

	public int guiHeight() {
		return mc.getWindow().getGuiScaledHeight();
	}

	private void begin() {
		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(pose.last().pose());
	}

	private void end() {
		RenderSystem.popMatrix();
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
		double s = mc.getWindow().getGuiScale();
		int h = mc.getWindow().getHeight();
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
		RenderSystem.color4f(r, g, b, a);
	}

	public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texW, int texH) {
		mc.getTextureManager().bind(texture);
		RenderSystem.enableBlend();
		begin();
		GuiComponent.blit(x, y, u, v, w, h, texW, texH);
		end();
	}

	public void blit(int x, int y, int z, int w, int h, TextureAtlasSprite sprite) {
		mc.getTextureManager().bind(sprite.atlas().location());
		RenderSystem.enableBlend();
		begin();
		GuiComponent.blit(x, y, z, w, h, sprite);
		end();
	}

	public void renderItem(ItemStack s, int x, int y) {
		begin();
		mc.getItemRenderer().renderAndDecorateItem(s, x, y);
		end();
	}

	public void renderItemDecorations(Font font, ItemStack s, int x, int y, String text) {
		begin();
		mc.getItemRenderer().renderGuiItemDecorations(font, s, x, y, text);
		end();
	}
}
