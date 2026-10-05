package com.tatnat.client.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tatnat.client.mc.EdgeBox;
import com.tatnat.client.modules.impl.visual.BlockOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Block Overlay for 1.21.6 - 1.21.8. Line width is fixed on these versions, so instead of
 * vanilla's outline (the second {@code renderHitOutline} call; the first is the high-contrast
 * backdrop) we draw the edges as thin filled bars whose thickness follows the slider, then fill
 * the face being looked at.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@WrapOperation(method = "renderBlockOutline", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)V",
			ordinal = 1))
	private void tatnat$blockOverlay(LevelRenderer self, PoseStack pose, VertexConsumer lines, Entity viewer, double camX, double camY, double camZ,
			BlockPos pos, BlockState block, int color, Operation<Void> original,
			@Local(argsOnly = true) MultiBufferSource.BufferSource buffers) {
		BlockOverlay overlay = BlockOverlay.INSTANCE;
		if (overlay == null || !overlay.isEnabled()) {
			original.call(self, pose, lines, viewer, camX, camY, camZ, pos, block, color);
			return;
		}
		VoxelShape shape = block.getShape(Minecraft.getInstance().level, pos, CollisionContext.of(viewer));
		if (shape.isEmpty()) return;
		VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
		AABB box = shape.bounds().move(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
		// Thickness in blocks grows with distance so it looks about the same size on screen.
		double dist = Math.sqrt(box.getCenter().lengthSqr());
		float t = (float) (overlay.thickness.get() * 0.0028 * Math.max(1.0, dist));
		EdgeBox.draw(quads, pose.last().pose(), box, t, overlay.outline());

		if (!overlay.fillFace.on() || overlay.fillOpacity.get() <= 0) return;
		HitResult hit = Minecraft.getInstance().hitResult;
		if (!(hit instanceof BlockHitResult) || hit.getType() != HitResult.Type.BLOCK) return;
		BlockHitResult bhr = (BlockHitResult) hit;
		if (!bhr.getBlockPos().equals(pos)) return;
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
