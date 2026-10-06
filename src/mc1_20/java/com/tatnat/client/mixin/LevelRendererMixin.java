package com.tatnat.client.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tatnat.client.mc.EdgeBox;
import com.tatnat.client.modules.impl.visual.BlockOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Block Overlay for this version: replaces vanilla's thin outline with thick bars whose size
 * follows the slider, and tints the face being looked at.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
	private void tatnat$blockOverlay(PoseStack pose, VertexConsumer lines, Entity viewer, double camX, double camY, double camZ,
			BlockPos pos, BlockState block, CallbackInfo ci) {
		BlockOverlay overlay = BlockOverlay.INSTANCE;
		if (overlay == null || !overlay.isEnabled()) return;
		ci.cancel();
		Minecraft mc = Minecraft.getInstance();
		VoxelShape shape = block.getShape(mc.level, pos, CollisionContext.of(viewer));
		if (shape.isEmpty()) return;
		VertexConsumer quads = mc.renderBuffers().bufferSource().getBuffer(RenderType.debugQuads());
		AABB box = shape.bounds().move(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
		double dist = Math.sqrt(box.getCenter().lengthSqr());
		float t = (float) (overlay.thickness.get() * 0.0028 * Math.max(1.0, dist));
		EdgeBox.draw(quads, pose.last().pose(), box, t, overlay.outline());

		if (!overlay.fillFace.on() || overlay.fillOpacity.get() <= 0) return;
		HitResult hit = mc.hitResult;
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
			case UP: EdgeBox.quad(v, m, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); break;
			case DOWN: EdgeBox.quad(v, m, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); break;
			case NORTH: EdgeBox.quad(v, m, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); break;
			case SOUTH: EdgeBox.quad(v, m, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); break;
			case WEST: EdgeBox.quad(v, m, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); break;
			default: EdgeBox.quad(v, m, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); break;
		}
	}
}
