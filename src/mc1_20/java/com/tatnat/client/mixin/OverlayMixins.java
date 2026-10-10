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
	/** Dark Mode: a dark panel behind the slots, lighter label text. */
	@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
	public abstract static class DarkContainers {
		@org.spongepowered.asm.mixin.Shadow
		protected int leftPos;
		@org.spongepowered.asm.mixin.Shadow
		protected int topPos;
		@org.spongepowered.asm.mixin.Shadow
		protected int imageWidth;
		@org.spongepowered.asm.mixin.Shadow
		protected int imageHeight;

		@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V", shift = At.Shift.AFTER), require = 0)
		private void tatnat$dark_render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.DarkMode.on()) g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, com.tatnat.client.modules.impl.visual.DarkMode.overlay());
		}

		@org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"), index = 4, require = 0)
		private int tatnat$label0(int color) {
			return com.tatnat.client.modules.impl.visual.DarkMode.on() ? com.tatnat.client.modules.impl.visual.DarkMode.label(color) : color;
		}

		@org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 4, require = 0)
		private int tatnat$label1(int color) {
			return com.tatnat.client.modules.impl.visual.DarkMode.on() ? com.tatnat.client.modules.impl.visual.DarkMode.label(color) : color;
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
	/** Dark Mode: the player inventory draws its own labels. */
	@Mixin(net.minecraft.client.gui.screens.inventory.InventoryScreen.class)
	public abstract static class DarkInventoryLabels {
		@org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"), index = 4, require = 0)
		private int tatnat$label0(int color) {
			return com.tatnat.client.modules.impl.visual.DarkMode.on() ? com.tatnat.client.modules.impl.visual.DarkMode.label(color) : color;
		}
		@org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 4, require = 0)
		private int tatnat$label1(int color) {
			return com.tatnat.client.modules.impl.visual.DarkMode.on() ? com.tatnat.client.modules.impl.visual.DarkMode.label(color) : color;
		}
	}
	/** After a post effect: back to texture unit 0, or the HUD draws from the wrong texture. */
	@Mixin(net.minecraft.client.renderer.PostChain.class)
	public static class PostTextureReset {
		@Inject(method = "process", at = @At("TAIL"), require = 0)
		private void tatnat$unit0(CallbackInfo ci) {
			com.mojang.blaze3d.systems.RenderSystem.activeTexture(33984);
		}
	}
	/** Custom Advancements: the advancements screen in dark colours. */
	@Mixin(net.minecraft.client.gui.screens.advancements.AdvancementsScreen.class)
	public static class DarkAdvancements {
		@Inject(method = "renderInside", at = @At("HEAD"), require = 0)
		private void tatnat$dark_renderInside(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.CustomAdvancements.dark()) {
				float k = com.tatnat.client.modules.impl.visual.CustomAdvancements.shade();
				com.mojang.blaze3d.systems.RenderSystem.setShaderColor(k, k, k * 1.08f, 1f);
			}
		}

		@Inject(method = "renderInside", at = @At("RETURN"), require = 0)
		private void tatnat$light_renderInside(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.CustomAdvancements.dark()) com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		}

		@Inject(method = "renderWindow", at = @At("HEAD"), require = 0)
		private void tatnat$dark_renderWindow(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.CustomAdvancements.dark()) {
				float k = com.tatnat.client.modules.impl.visual.CustomAdvancements.shade();
				com.mojang.blaze3d.systems.RenderSystem.setShaderColor(k, k, k * 1.08f, 1f);
			}
		}

		@Inject(method = "renderWindow", at = @At("RETURN"), require = 0)
		private void tatnat$light_renderWindow(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.CustomAdvancements.dark()) com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		}
	}
	/** Death Info: where you died, on the death screen. */
	@Mixin(net.minecraft.client.gui.screens.DeathScreen.class)
	public static class DeathCoords {
		@Inject(method = "render", at = @At("TAIL"), require = 0)
		private void tatnat$deathCoords(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
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
}
