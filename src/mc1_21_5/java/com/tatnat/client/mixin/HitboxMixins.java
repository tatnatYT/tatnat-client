package com.tatnat.client.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tatnat.client.modules.impl.visual.Hitboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Hitboxes for 1.21.6 - 1.21.8 (no gizmo system yet): switches on Minecraft's own F3+B hitboxes
 * for the chosen entity types and recolours them. Line width is fixed on these versions.
 */
public final class HitboxMixins {
	private HitboxMixins() {
	}

	@Mixin(EntityRenderer.class)
	public static class Enable {
		@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRenderHitBoxes()Z"))
		private boolean tatnat$hitboxes(EntityRenderDispatcher dispatcher, Operation<Boolean> original, @Local(argsOnly = true) Entity entity) {
			boolean vanilla = original.call(dispatcher);
			return vanilla || Hitboxes.active() && com.tatnat.client.mc.HitboxFilter.wanted(entity);
		}
	}

	@Mixin(EntityRenderDispatcher.class)
	public static class Colors {
		@ModifyArgs(method = "renderHitbox", at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/renderer/ShapeRenderer;renderLineBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDDDDFFFF)V"))
		private static void tatnat$boxColor(Args args) {
			if (!Hitboxes.active()) return;
			int c = Hitboxes.INSTANCE.boxColor.color();
			args.set(8, ((c >> 16) & 255) / 255f);
			args.set(9, ((c >> 8) & 255) / 255f);
			args.set(10, (c & 255) / 255f);
			args.set(11, ((c >>> 24) & 255) / 255f);
		}

		@WrapOperation(method = "renderHitboxesAndViewVector", at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/renderer/ShapeRenderer;renderVector(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Vector3f;Lnet/minecraft/world/phys/Vec3;I)V"))
		private static void tatnat$lookLine(PoseStack pose, VertexConsumer lines, Vector3f from, Vec3 dir, int color, Operation<Void> original) {
			if (!Hitboxes.active()) {
				original.call(pose, lines, from, dir, color);
			} else if (Hitboxes.INSTANCE.lookLine.on()) {
				original.call(pose, lines, from, dir, Hitboxes.INSTANCE.lookColor.color());
			}
		}
	}
}
