package com.tatnat.client.mc;

import org.joml.Matrix3x2f;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.render.state.GuiElementRenderState;

/**
 * Many solid rectangles as ONE GUI element. Each fill becomes its own render-state object on
 * these versions, and the menu draws ~30k tiny fills a frame (anti-aliased corners, line-art
 * icons), which made it crawl; one element per batch keeps it smooth.
 */
final class GuiRects implements GuiElementRenderState {
	private final Matrix3x2f pose;
	private final int[] data;
	private final int count;
	private final ScreenRectangle scissor, bounds;

	GuiRects(Matrix3x2f pose, int[] data, int count, ScreenRectangle scissor) {
		this.pose = pose;
		this.data = data;
		this.count = count;
		this.scissor = scissor;
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		for (int i = 0; i < count; i++) {
			int o = i * 5;
			minX = Math.min(minX, data[o]);
			minY = Math.min(minY, data[o + 1]);
			maxX = Math.max(maxX, data[o + 2]);
			maxY = Math.max(maxY, data[o + 3]);
		}
		ScreenRectangle b = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY).transformMaxBounds(pose);
		this.bounds = scissor != null ? scissor.intersection(b) : b;
	}

	@Override
	public void buildVertices(VertexConsumer vc, float z) {
		for (int i = 0; i < count; i++) {
			int o = i * 5, c = data[o + 4];
			float x0 = data[o], y0 = data[o + 1], x1 = data[o + 2], y1 = data[o + 3];
			vc.addVertexWith2DPose(pose, x0, y0, z).setColor(c);
			vc.addVertexWith2DPose(pose, x0, y1, z).setColor(c);
			vc.addVertexWith2DPose(pose, x1, y1, z).setColor(c);
			vc.addVertexWith2DPose(pose, x1, y0, z).setColor(c);
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI;
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}

	@Override
	public ScreenRectangle scissorArea() {
		return scissor;
	}

	@Override
	public ScreenRectangle bounds() {
		return bounds;
	}
}
