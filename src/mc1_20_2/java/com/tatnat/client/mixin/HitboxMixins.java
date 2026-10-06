package com.tatnat.client.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tatnat.client.mc.HitboxFilter;
import com.tatnat.client.modules.impl.visual.Hitboxes;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

/**
 * Hitboxes on this version: switches on Minecraft's own F3+B hitboxes for the chosen entity
 * types. Line width, colour and the look line are vanilla here.
 */
public final class HitboxMixins {
	private HitboxMixins() {
	}

	@Mixin(EntityRenderDispatcher.class)
	public static class Enable {
		@ModifyExpressionValue(method = "render*", at = @At(value = "FIELD",
				target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderHitBoxes:Z", opcode = Opcodes.GETFIELD))
		private boolean tatnat$hitboxes(boolean vanilla, @Local(argsOnly = true) Entity entity) {
			return vanilla || Hitboxes.active() && HitboxFilter.wanted(entity);
		}
	}
}
