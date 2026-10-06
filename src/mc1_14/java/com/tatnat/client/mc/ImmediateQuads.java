package com.tatnat.client.mc;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;

/** Block Overlay draws its quads straight away with the fixed-function pipeline on 1.14. */
public final class ImmediateQuads {
	private ImmediateQuads() {
	}

	public static BufferBuilder begin() {
		BufferBuilder b = Tesselator.getInstance().getBuilder();
		b.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
		return b;
	}

	public static void draw(BufferBuilder b) {
		GlStateManager.disableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.disableCull();
		GlStateManager.enableDepthTest();
		Tesselator.getInstance().end();
		GlStateManager.enableCull();
		GlStateManager.enableTexture();
		GlStateManager.disableBlend();
	}
}
