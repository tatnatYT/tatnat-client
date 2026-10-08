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

	@Mixin(net.minecraft.client.gui.components.toasts.ToastManager.class)
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
	/** Custom F3: drop the system-information column on the right. */
	@Mixin(net.minecraft.client.gui.components.DebugScreenOverlay.class)
	public static class DebugSpam {
		@Inject(method = "renderLines", at = @At("HEAD"), cancellable = true, require = 0)
		private void tatnat$spam(net.minecraft.client.gui.GuiGraphics g, java.util.List<String> lines, boolean left, CallbackInfo ci) {
			if (!left && com.tatnat.client.modules.impl.visual.CustomF3.hideSpam()) ci.cancel();
		}
	}
}
