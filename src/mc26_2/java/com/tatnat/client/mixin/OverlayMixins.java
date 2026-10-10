package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tatnat.client.modules.impl.visual.OverlayToggles;

/** Boss Bar, Toast Control and Subtitles: skip drawing those overlays while their mod is on. */
public final class OverlayMixins {
	private OverlayMixins() {
	}

	@Mixin(net.minecraft.client.gui.components.BossHealthOverlay.class)
	public static class Boss {
		@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$hide(CallbackInfo ci) {
			if (OverlayToggles.hideBossBar()) ci.cancel();
		}
	}

	@Mixin(net.minecraft.client.gui.components.SubtitleOverlay.class)
	public static class Subtitles {
		@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$hide(CallbackInfo ci) {
			if (OverlayToggles.hideSubtitles()) ci.cancel();
		}
	}

	@Mixin(net.minecraft.client.gui.components.toasts.ToastManager.class)
	public static class Toasts {
		@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$hide(CallbackInfo ci) {
			if (OverlayToggles.hideToasts()) ci.cancel();
		}
	}
	/** Camera: third-person distance (the game still pulls the camera in front of walls). */
	@Mixin(net.minecraft.client.Camera.class)
	public static class CameraDistance {
		@org.spongepowered.asm.mixin.injection.ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true, require = 0)
		private float tatnat$distance(float distance) {
			return (float) (distance * com.tatnat.client.modules.impl.visual.CameraTweaks.distanceFactor());
		}
	}
	/** Mob Overlay: the game's own glowing outline on the chosen kinds of entities. */
	@Mixin(net.minecraft.client.Minecraft.class)
	public static class MobGlow {
		@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$glow(net.minecraft.world.entity.Entity e, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
			int kind = e instanceof net.minecraft.world.entity.player.Player ? 3 : e instanceof net.minecraft.world.entity.monster.Enemy ? 1
					: e instanceof net.minecraft.world.entity.LivingEntity ? 2 : 0;
			if (kind != 0 && e != net.minecraft.client.Minecraft.getInstance().player && com.tatnat.client.modules.impl.visual.MobOverlay.glows(kind)) cir.setReturnValue(true);
		}
	}
	/** Title Tweaker: drop titles / subtitles before they show. */
	@Mixin(net.minecraft.client.gui.Hud.class)
	public static class Titles {
		@Inject(method = "setTitle", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$title(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.TitleTweaker.hideTitles()) ci.cancel();
		}

		@Inject(method = "setSubtitle", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$subtitle(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.TitleTweaker.hideSubtitles()) ci.cancel();
		}
	}
	/** Inventory: no player model next to your inventory. */
	@Mixin(net.minecraft.client.gui.screens.inventory.InventoryScreen.class)
	public static class InventoryModel {
		@Inject(method = "renderEntityInInventoryFollowsMouse", at = @At("HEAD"), cancellable = true, require = 0)
		private static void tatnat$hideFollow(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.InventoryTweaks.hideModel()) ci.cancel();
		}

		@Inject(method = "renderEntityInInventory", at = @At("HEAD"), cancellable = true, require = 0)
		private static void tatnat$hide(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.InventoryTweaks.hideModel()) ci.cancel();
		}
	}
	/** Player Model: no armor drawn. */
	@Mixin(net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer.class)
	public static class ArmorLayerHide {
		@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$armor_render(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.PlayerModel.hideArmor()) ci.cancel();
		}

		@Inject(method = "submit", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$armor_submit(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.PlayerModel.hideArmor()) ci.cancel();
		}
	}

	/** Elytras: no elytra drawn. */
	@Mixin(net.minecraft.client.renderer.entity.layers.WingsLayer.class)
	public static class ElytraLayerHide {
		@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$elytra_render(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.PlayerModel.hideElytra()) ci.cancel();
		}

		@Inject(method = "submit", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$elytra_submit(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.PlayerModel.hideElytra()) ci.cancel();
		}
	}
	/** Tablist: friends in your colour. */
	@Mixin(net.minecraft.client.gui.components.PlayerTabOverlay.class)
	public static class TabFriends {
		@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true, require = 0)
		private void tatnat$friend(net.minecraft.client.multiplayer.PlayerInfo info, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.network.chat.Component> cir) {
			String text = com.tatnat.client.modules.impl.visual.Tablist.highlight(cir.getReturnValue().getString());
			if (text != null) cir.setReturnValue(net.minecraft.network.chat.Component.literal(text));
		}
	}
	/** Totem Pop Counter: entity event 35 is the totem animation (the network handler plays it itself). */
	@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
	public abstract static class TotemPop {
		@Inject(method = "handleEntityEvent", at = @At("HEAD"), require = 0)
		private void tatnat$pop(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet, CallbackInfo ci) {
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			// Packets arrive on the network thread first and are handled again on the game thread.
			if (packet.getEventId() != 35 || !mc.isSameThread() || mc.level == null) return;
			net.minecraft.world.entity.Entity e = packet.getEntity(mc.level);
			if (e != null) com.tatnat.client.modules.impl.hud.TotemPops.popped(e.getName().getString(), e == mc.player);
		}
	}
	/** Death Info: where you died, on the death screen. */
	@Mixin(net.minecraft.client.gui.screens.DeathScreen.class)
	public static class DeathCoords {
		@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
		private void tatnat$deathCoords(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
			com.tatnat.client.modules.impl.hud.DeathInfo.onDeathScreen(new com.tatnat.client.mc.GfxImpl(graphics));
		}
	}
	/** Hearts / Armor Bar: the vanilla icons are hidden; the mods draw their own over any texture pack. */
	@Mixin(net.minecraft.client.gui.Gui.class)
	public static class HideBars {
		@Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$hideHearts(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.HudColors.hearts()) ci.cancel();
		}

		@Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true, require = 0)
		private static void tatnat$hideArmor(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.HudColors.armor()) ci.cancel();
		}
	}
	/** Item Physic (1.21.2+): dropped items lie flat and stop bobbing / spinning. */
	@Mixin(net.minecraft.client.renderer.entity.ItemEntityRenderer.class)
	public static class ItemFlatState {
		@org.spongepowered.asm.mixin.injection.Redirect(method = {"render", "submit"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getSpin(FF)F"), require = 0)
		private float tatnat$spin(float age, float bob) {
			return com.tatnat.client.modules.impl.visual.ItemPhysic.on() ? 0f : net.minecraft.world.entity.item.ItemEntity.getSpin(age, bob);
		}

		@org.spongepowered.asm.mixin.injection.Redirect(method = {"render", "submit"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;sin(F)F", ordinal = 0), require = 0)
		private float tatnat$bobF(float v) {
			return com.tatnat.client.modules.impl.visual.ItemPhysic.on() ? -1f : net.minecraft.util.Mth.sin(v);
		}

		@org.spongepowered.asm.mixin.injection.Redirect(method = {"render", "submit"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;sin(D)F", ordinal = 0), require = 0)
		private float tatnat$bobD(double v) {
			return com.tatnat.client.modules.impl.visual.ItemPhysic.on() ? -1f : (float) Math.sin(v);
		}

		@org.spongepowered.asm.mixin.injection.Redirect(method = {"render", "submit"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 0), require = 0)
		private void tatnat$flat(com.mojang.blaze3d.vertex.PoseStack pose, org.joml.Quaternionf q) {
			pose.mulPose(com.tatnat.client.modules.impl.visual.ItemPhysic.on() ? com.mojang.math.Axis.XP.rotationDegrees(90f) : q);
		}

		@org.spongepowered.asm.mixin.injection.Redirect(method = {"render", "submit"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", ordinal = 0), require = 0)
		private void tatnat$flatC(com.mojang.blaze3d.vertex.PoseStack pose, org.joml.Quaternionfc q) {
			pose.mulPose(com.tatnat.client.modules.impl.visual.ItemPhysic.on() ? com.mojang.math.Axis.XP.rotationDegrees(90f) : new org.joml.Quaternionf(q));
		}
	}
}
