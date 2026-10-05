package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.mc.PlayerLooks;
import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** Name tags (Nick Hider) and whether your own body is drawn (Freecam's Hide Body). */
public final class EntityRenderMixins {
	private EntityRenderMixins() {
	}

	@Mixin(EntityRenderer.class)
	public static class NameTag {
		@Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
		private void tatnat$nameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
			if (NickHider.active() && entity == Minecraft.getInstance().player && cir.getReturnValue() != null) {
				cir.setReturnValue(PlayerLooks.replaceName(cir.getReturnValue()));
			}
		}
	}

	@Mixin(EntityRenderDispatcher.class)
	public static class HideBody {
		@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
		private <E extends Entity> void tatnat$hideBody(E entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
			if (Freecam.active() && Freecam.INSTANCE.hideBody() && entity == Minecraft.getInstance().player) cir.setReturnValue(false);
		}
	}
}
