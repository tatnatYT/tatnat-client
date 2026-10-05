package com.tatnat.client.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tatnat.client.modules.impl.visual.BlockOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Block Overlay. Wraps vanilla's normal block-outline draw (the second {@code renderHitOutline}
 * call; the first is the high-contrast backdrop) to swap in our colour and line width, then
 * fills the face being looked at.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@WrapOperation(method = "renderBlockOutline", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/BlockOutlineRenderState;IF)V",
			ordinal = 1))
	private void tatnat$blockOverlay(LevelRenderer self, PoseStack pose, VertexConsumer lines, double camX, double camY, double camZ,
			BlockOutlineRenderState state, int color, float width, Operation<Void> original,
			@Local(argsOnly = true) MultiBufferSource.BufferSource buffers) {
		BlockOverlay overlay = BlockOverlay.INSTANCE;
		if (overlay == null || !overlay.isEnabled()) {
			original.call(self, pose, lines, camX, camY, camZ, state, color, width);
			return;
		}
		// Vanilla's width already scales with window size; the slider multiplies it (2.5 ~ "thick").
		float w = width * overlay.thickness.floatValue() / 1.25f;
		original.call(self, pose, lines, camX, camY, camZ, state, overlay.outline(), w);

		if (!overlay.fillFace.on() || overlay.fillOpacity.get() <= 0) return;
		HitResult hit = Minecraft.getInstance().hitResult;
		if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) return;
		BlockPos pos = state.pos();
		if (!bhr.getBlockPos().equals(pos) || state.shape().isEmpty()) return;

		// Getting a new buffer ends the lines batch, which is why this runs after the outline.
		VertexConsumer quads = buffers.getBuffer(RenderTypes.debugQuads());
		AABB box = state.shape().bounds().move(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
		face(quads, pose.last().pose(), box, bhr.getDirection(), overlay.fill());
	}

	/** One quad on the given side of {@code b}, pushed out a hair to avoid z-fighting. */
	private static void face(VertexConsumer v, Matrix4f m, AABB b, Direction side, int argb) {
		final float e = 0.002f;
		float x0 = (float) b.minX - e, y0 = (float) b.minY - e, z0 = (float) b.minZ - e;
		float x1 = (float) b.maxX + e, y1 = (float) b.maxY + e, z1 = (float) b.maxZ + e;
		switch (side) {
			case UP -> quad(v, m, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
			case DOWN -> quad(v, m, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
			case NORTH -> quad(v, m, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
			case SOUTH -> quad(v, m, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
			case WEST -> quad(v, m, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
			case EAST -> quad(v, m, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
		}
	}

	private static void quad(VertexConsumer v, Matrix4f m, int argb, float... p) {
		for (int i = 0; i < 12; i += 3) v.addVertex(m, p[i], p[i + 1], p[i + 2]).setColor(argb);
	}
}
