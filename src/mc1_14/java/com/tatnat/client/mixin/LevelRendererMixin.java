package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.tatnat.client.mc.EdgeBox;
import com.tatnat.client.mc.ImmediateQuads;
import com.tatnat.client.modules.impl.visual.BlockOverlay;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Block Overlay for 1.14: replaces vanilla's thin outline with thick bars whose size follows the
 * slider, and tints the face being looked at. Drawn straight away (the GL matrix is the camera).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
	private void tatnat$blockOverlay(Camera camera, HitResult hit, int pass, CallbackInfo ci) {
		BlockOverlay overlay = BlockOverlay.INSTANCE;
		if (overlay == null || !overlay.isEnabled() || pass != 0 || !(hit instanceof BlockHitResult) || hit.getType() != HitResult.Type.BLOCK) return;
		ci.cancel();
		Minecraft mc = Minecraft.getInstance();
		BlockHitResult bhr = (BlockHitResult) hit;
		BlockPos pos = bhr.getBlockPos();
		BlockState block = mc.level.getBlockState(pos);
		if (block.isAir() || !mc.level.getWorldBorder().isWithinBounds(pos)) return;
		VoxelShape shape = block.getShape(mc.level, pos, CollisionContext.of(camera.getEntity()));
		if (shape.isEmpty()) return;
		Vec3 cam = camera.getPosition();
		AABB box = shape.bounds().move(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
		double dist = Math.sqrt(box.getCenter().lengthSqr());
		float t = (float) (overlay.thickness.get() * 0.0028 * Math.max(1.0, dist));
		BufferBuilder quads = ImmediateQuads.begin();
		EdgeBox.draw(quads, box, t, overlay.outline());
		if (overlay.fillFace.on() && overlay.fillOpacity.get() > 0) face(quads, box, bhr.getDirection(), overlay.fill());
		ImmediateQuads.draw(quads);
	}

	/** One quad on the given side of {@code b}, pushed out a hair to avoid z-fighting. */
	private static void face(BufferBuilder v, AABB b, Direction side, int argb) {
		final float e = 0.002f;
		float x0 = (float) b.minX - e, y0 = (float) b.minY - e, z0 = (float) b.minZ - e;
		float x1 = (float) b.maxX + e, y1 = (float) b.maxY + e, z1 = (float) b.maxZ + e;
		switch (side) {
			case UP: EdgeBox.quad(v, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); break;
			case DOWN: EdgeBox.quad(v, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); break;
			case NORTH: EdgeBox.quad(v, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); break;
			case SOUTH: EdgeBox.quad(v, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); break;
			case WEST: EdgeBox.quad(v, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); break;
			default: EdgeBox.quad(v, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); break;
		}
	}
}
