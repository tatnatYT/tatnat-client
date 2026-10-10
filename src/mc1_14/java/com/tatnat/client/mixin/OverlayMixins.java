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
		@Inject(method = "setTitles", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$titles(CallbackInfo ci) {
			if (com.tatnat.client.modules.impl.visual.TitleTweaker.hideTitles()) ci.cancel();
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
			if (text != null) cir.setReturnValue(new net.minecraft.network.chat.TextComponent(text));
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
			com.mojang.blaze3d.platform.GlStateManager.fogStart(1.0E6f);
			com.mojang.blaze3d.platform.GlStateManager.fogEnd(2.0E6f);
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
		@Inject(method = "render", at = @At("TAIL"), require = 0)
		private void tatnat$deathCoords(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
			com.tatnat.client.modules.impl.hud.DeathInfo.onDeathScreen(new com.tatnat.client.mc.GfxImpl(new com.tatnat.client.mc.Graphics()));
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
