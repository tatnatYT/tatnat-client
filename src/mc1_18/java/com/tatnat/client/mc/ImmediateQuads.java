package com.tatnat.client.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;

/**
 * 1.18.2 has no see-through quad render type (debugQuads arrived later) and RenderType.create
 * is not public, so Block Overlay draws its quads straight away with the same state.
 */
public final class ImmediateQuads {
	private ImmediateQuads() {
	}

	public static BufferBuilder begin() {
		BufferBuilder b = Tesselator.getInstance().getBuilder();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		return b;
	}

	public static void draw(BufferBuilder b) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		b.end();
		BufferUploader.end(b);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}
}
