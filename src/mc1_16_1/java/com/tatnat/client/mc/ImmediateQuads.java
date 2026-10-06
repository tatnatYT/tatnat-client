package com.tatnat.client.mc;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;

/**
 * 1.16 has no see-through quad render type, so Block Overlay draws its quads straight away
 * with the fixed-function pipeline.
 */
public final class ImmediateQuads {
	private ImmediateQuads() {
	}

	public static BufferBuilder begin() {
		BufferBuilder b = Tesselator.getInstance().getBuilder();
		b.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
		return b;
	}

	public static void draw(BufferBuilder b) {
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		b.end();
		BufferUploader.end(b);
		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}
}
