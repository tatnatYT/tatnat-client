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
		@org.spongepowered.asm.mixin.injection.ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true)
		private String tatnat$nameTag(String name, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) Entity entity) {
			if (NickHider.active() && entity == Minecraft.getInstance().player && name != null) name = PlayerLooks.replaceName(name);
			com.tatnat.client.modules.impl.utility.TierTagger.Tag tier = entity instanceof net.minecraft.world.entity.player.Player ? com.tatnat.client.modules.impl.utility.TierTagger.nameTag(entity.getUUID(), entity.getName().getString()) : null;
			if (tier != null && name != null) name = tier.legacy() + name;
			String pop = com.tatnat.client.modules.impl.hud.TotemPops.tag(entity instanceof net.minecraft.world.entity.player.Player ? entity.getName().getString() : null);
			if (pop != null && name != null) name = name + " §c" + pop;
			return name;
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
