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
		@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$hide(CallbackInfo ci) {
			if (OverlayToggles.hideBossBar()) ci.cancel();
		}
	}

	@Mixin(net.minecraft.client.gui.components.SubtitleOverlay.class)
	public static class Subtitles {
		@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$hide(CallbackInfo ci) {
			if (OverlayToggles.hideSubtitles()) ci.cancel();
		}
	}

	@Mixin(net.minecraft.client.gui.components.toasts.ToastComponent.class)
	public static class Toasts {
		@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$hide(CallbackInfo ci) {
			if (OverlayToggles.hideToasts()) ci.cancel();
		}
	}
	/** Camera: third-person distance (the game still pulls the camera in front of walls). */
	@Mixin(net.minecraft.client.Camera.class)
	public static class CameraDistance {
		@org.spongepowered.asm.mixin.injection.ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true, require = 0)
		private double tatnat$distance(double distance) {
			return (double) (distance * com.tatnat.client.modules.impl.visual.CameraTweaks.distanceFactor());
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
	@Mixin(net.minecraft.client.gui.Gui.class)
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
	@Mixin(net.minecraft.client.renderer.entity.layers.ElytraLayer.class)
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
	/** Custom F3: drop the system-information column on the right. */
	@Mixin(net.minecraft.client.gui.components.DebugScreenOverlay.class)
	public static class DebugSpam {
		@Inject(method = "drawSystemInformation", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$spam(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.CustomF3.hideSpam()) ci.cancel();
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
	/** Horses: the vanilla jump bar makes way for the Horses one. */
	@Mixin(net.minecraft.client.gui.Gui.class)
	public static class JumpMeter {
		@Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$jump(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.hud.Horses.replacesVanilla()) ci.cancel();
		}
	}
	/** Custom Fog: push the fog out of sight. */
	@Mixin(net.minecraft.client.renderer.FogRenderer.class)
	public static class NoFog {
		@Inject(method = "setupFog", at = @At("TAIL"), require = 0)
		private static void tatnat$fog(CallbackInfo ci) {
			if (!com.tatnat.client.modules.impl.visual.CustomFog.disabled()) return;
			com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(1.0E6f);
			com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(2.0E6f);
		}
	}
	/** ViewModel: move / scale the held item (undone at the end so nothing leaks). */
	@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
	public static class HeldItem {
		@Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
		private void tatnat$before(net.minecraft.client.player.AbstractClientPlayer player, float partialTick, float pitch, net.minecraft.world.InteractionHand hand, float swing, net.minecraft.world.item.ItemStack stack, float equip, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
			if (!com.tatnat.client.modules.impl.visual.ViewModel.active()) return;
			double side = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? 1 : -1;
			pose.translate(side * com.tatnat.client.modules.impl.visual.ViewModel.x(), com.tatnat.client.modules.impl.visual.ViewModel.y(), com.tatnat.client.modules.impl.visual.ViewModel.z());
			float s = com.tatnat.client.modules.impl.visual.ViewModel.scale();
			pose.scale(s, s, s);
		}

		@Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
		private void tatnat$after(net.minecraft.client.player.AbstractClientPlayer player, float partialTick, float pitch, net.minecraft.world.InteractionHand hand, float swing, net.minecraft.world.item.ItemStack stack, float equip, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
			if (!com.tatnat.client.modules.impl.visual.ViewModel.active()) return;
			float s = com.tatnat.client.modules.impl.visual.ViewModel.scale();
			pose.scale(1 / s, 1 / s, 1 / s);
			double side = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? 1 : -1;
			pose.translate(-side * com.tatnat.client.modules.impl.visual.ViewModel.x(), -com.tatnat.client.modules.impl.visual.ViewModel.y(), -com.tatnat.client.modules.impl.visual.ViewModel.z());
		}
	}
	/** Hearts / Armor Bar: tint the icons while the game draws them. */
	@Mixin(net.minecraft.client.gui.Gui.class)
	public static class BarColors {
		@Inject(method = "renderHearts", at = @At("HEAD"), require = 0)
		private void tatnat$renderHeartsTint(CallbackInfo ci) {
			if (!com.tatnat.client.modules.impl.visual.HudColors.hearts()) return;
			int c = com.tatnat.client.modules.impl.visual.HudColors.heartColor();
			com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 1f);
		}

		@Inject(method = "renderHearts", at = @At("RETURN"), require = 0)
		private void tatnat$renderHeartsReset(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.HudColors.hearts()) com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		}
	}
	/** Item Physic: dropped items lie flat and stop bobbing / spinning. */
	@Mixin(net.minecraft.client.renderer.entity.ItemEntityRenderer.class)
	public static class ItemFlat {
		@org.spongepowered.asm.mixin.injection.Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getSpin(F)F"), require = 0)
		private float tatnat$spin(net.minecraft.world.entity.item.ItemEntity e, float partialTick) {
			return com.tatnat.client.modules.impl.visual.ItemPhysic.on() ? 0f : e.getSpin(partialTick);
		}

		@org.spongepowered.asm.mixin.injection.Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;sin(F)F", ordinal = 0), require = 0)
		private float tatnat$bob(float v) {
			return com.tatnat.client.modules.impl.visual.ItemPhysic.on() ? -1f : net.minecraft.util.Mth.sin(v);
		}

		@org.spongepowered.asm.mixin.injection.Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 0), require = 0)
		private void tatnat$flat(com.mojang.blaze3d.vertex.PoseStack pose, org.joml.Quaternionf q) {
			pose.mulPose(com.tatnat.client.modules.impl.visual.ItemPhysic.on() ? com.mojang.math.Axis.XP.rotationDegrees(90f) : q);
		}
	}
	/** Dark Mode: inventories and containers drawn in dark charcoal. */
	@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
	public static class DarkContainers {
		@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"), require = 0)
		private void tatnat$dark_render(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.DarkMode.on()) {
				float k = com.tatnat.client.modules.impl.visual.DarkMode.shade();
				com.mojang.blaze3d.systems.RenderSystem.setShaderColor(k, k, k * 1.08f, 1f);
			}
		}

		@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V", shift = At.Shift.AFTER), require = 0)
		private void tatnat$light_render(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.DarkMode.on()) com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		}

		@Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"), require = 0)
		private void tatnat$dark_renderBackground(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.DarkMode.on()) {
				float k = com.tatnat.client.modules.impl.visual.DarkMode.shade();
				com.mojang.blaze3d.systems.RenderSystem.setShaderColor(k, k, k * 1.08f, 1f);
			}
		}

		@Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V", shift = At.Shift.AFTER), require = 0)
		private void tatnat$light_renderBackground(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.DarkMode.on()) com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		}
	}
}
