package com.tatnat.client.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1.18.2 has no GuiGraphics: GUI drawing is static GuiComponent helpers on a PoseStack. This gives
 * the rest of the platform the same small surface the newer versions have.
 */
public final class Graphics extends GuiComponent {
	private final PoseStack pose;
	private final Minecraft mc = Minecraft.getInstance();

	public Graphics(PoseStack pose) {
		this.pose = pose;
	}

	public PoseStack pose() {
		return pose;
	}

	public int guiWidth() {
		return mc.getWindow().getGuiScaledWidth();
	}

	public int guiHeight() {
		return mc.getWindow().getGuiScaledHeight();
	}

	public void fill(int x1, int y1, int x2, int y2, int argb) {
		GuiComponent.fill(pose, x1, y1, x2, y2, argb);
	}

	public void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom) {
		GuiComponent.fillGradient(pose, x1, y1, x2, y2, top, bottom, 0);
	}

	private static final ArrayDeque<int[]> CLIPS = new ArrayDeque<>();

	/** Scissor in GUI units, nested boxes intersect (this version has no scissor helpers). */
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
		if (top == null) RenderSystem.disableScissor();
		else applyScissor(top);
	}

	private void applyScissor(int[] r) {
		double s = mc.getWindow().getGuiScale();
		int h = mc.getWindow().getHeight();
		RenderSystem.enableScissor((int) (r[0] * s), (int) (h - r[3] * s), Math.max(0, (int) ((r[2] - r[0]) * s)),
				Math.max(0, (int) ((r[3] - r[1]) * s)));
	}

	public void drawString(Font font, Component s, int x, int y, int argb, boolean shadow) {
		if (shadow) font.drawShadow(pose, s, x, y, argb);
		else font.draw(pose, s, x, y, argb);
	}

	public void drawString(Font font, String s, int x, int y, int argb, boolean shadow) {
		if (shadow) font.drawShadow(pose, s, x, y, argb);
		else font.draw(pose, s, x, y, argb);
	}

	public void setColor(float r, float g, float b, float a) {
		RenderSystem.setShaderColor(r, g, b, a);
	}

	public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texW, int texH) {
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		GuiComponent.blit(pose, x, y, u, v, w, h, texW, texH);
	}

	public void blit(int x, int y, int z, int w, int h, TextureAtlasSprite sprite) {
		RenderSystem.setShaderTexture(0, sprite.atlas().location());
		RenderSystem.enableBlend();
		GuiComponent.blit(pose, x, y, z, w, h, sprite);
	}

	// Item rendering here reads the global model-view stack, not our pose, so apply it there.
	private void withPose(Runnable r) {
		PoseStack mv = RenderSystem.getModelViewStack();
		mv.pushPose();
		mv.mulPoseMatrix(pose.last().pose());
		RenderSystem.applyModelViewMatrix();
		r.run();
		mv.popPose();
		RenderSystem.applyModelViewMatrix();
	}

	public void renderItem(ItemStack s, int x, int y) {
		withPose(() -> mc.getItemRenderer().renderAndDecorateItem(s, x, y));
	}

	public void renderItemDecorations(Font font, ItemStack s, int x, int y, String text) {
		withPose(() -> mc.getItemRenderer().renderGuiItemDecorations(font, s, x, y, text));
	}
}
