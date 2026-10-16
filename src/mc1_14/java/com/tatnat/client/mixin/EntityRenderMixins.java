package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.mc.PlayerLooks;
import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.Minecraft;
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
			if (tier != null && name != null) name = tier.legacyIcon() + tier.legacy() + name;
			if (entity instanceof net.minecraft.world.entity.player.Player && name != null) name = com.tatnat.client.plus.EclipseBadge.glyphText(com.tatnat.client.plus.EclipseNet.badge(entity.getUUID(), entity == net.minecraft.client.Minecraft.getInstance().player)) + name;
			String pop = com.tatnat.client.modules.impl.hud.TotemPops.tag(entity instanceof net.minecraft.world.entity.player.Player ? entity.getName().getString() : null);
			if (pop != null && name != null) name = name + " §c" + pop;
			return name;
		}
	}

	@Mixin(EntityRenderDispatcher.class)
	public static class HideBody {
		@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
		private <E extends Entity> void tatnat$hideBody(E entity, net.minecraft.client.renderer.culling.Culler frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
			if (Freecam.active() && Freecam.INSTANCE.hideBody() && entity == Minecraft.getInstance().player) cir.setReturnValue(false);
		}

		/** Built-in Entity Culling: entities the game would draw but that are hidden behind blocks. */
		@Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, require = 0)
		private <E extends Entity> void tatnat$cull(E entity, net.minecraft.client.renderer.culling.Culler frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
			if (!cir.getReturnValueZ() || !com.tatnat.client.perf.Optimizations.enabled()) return;
			Minecraft mc = Minecraft.getInstance();
			boolean exempt = entity instanceof net.minecraft.world.entity.player.Player || entity == mc.getCameraEntity() || entity.isGlowing();
			net.minecraft.world.phys.AABB b = entity.getBoundingBox();
			if (com.tatnat.client.perf.Optimizations.entityHidden(entity, exempt, x, y, z, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ)) cir.setReturnValue(false);
		}
	}

	/** Built-in Block Entity Culling: chests, signs, banners... hidden behind blocks aren't drawn. */
	@Mixin(net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher.class)
	public static class CullBlockEntities {
		@Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FI)V", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$cullBlockEntity(net.minecraft.world.level.block.entity.BlockEntity be, float pt, int destroy, CallbackInfo ci) {
			// Beams and outlines reach far outside the block: always drawn.
			if (be instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity || be instanceof net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity || be instanceof net.minecraft.world.level.block.entity.StructureBlockEntity) return;
			net.minecraft.core.BlockPos p = be.getBlockPos();
			if (com.tatnat.client.perf.Optimizations.blockEntityHidden(be, p.getX(), p.getY(), p.getZ())) ci.cancel();
		}
	}
}
