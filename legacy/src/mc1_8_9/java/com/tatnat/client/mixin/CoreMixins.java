package com.tatnat.client.mixin;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.mc.FeaturesImpl;
import com.tatnat.client.mc.GameImpl;
import com.tatnat.client.mc.GfxImpl;
import com.tatnat.client.mc.LwjglKeys;
import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.visual.Crosshair;
import com.tatnat.client.modules.impl.visual.FovModifier;
import com.tatnat.client.modules.impl.visual.Zoom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

/** The basic hooks the shared code runs on: ticks, input, HUD drawing, FOV and attacks. */
public final class CoreMixins {
	private CoreMixins() {
	}

	/** Posts {@link Events.Tick} once per client tick. */
	@Mixin(MinecraftClient.class)
	public static class Tick {
		@Inject(method = "tick", at = @At("TAIL"))
		private void tatnat$tick(CallbackInfo ci) {
			TatnatClient.EVENTS.post(Events.Tick.INSTANCE);
		}
	}

	/**
	 * Every key and mouse-button change goes through here as LWJGL 2 codes (mouse buttons as
	 * {@code button - 100}); post them as GLFW-coded events.
	 */
	@Mixin(KeyBinding.class)
	public static class Input {
		@Inject(method = "setKeyPressed", at = @At("HEAD"), cancellable = true)
		private static void tatnat$input(int code, boolean down, CallbackInfo ci) {
			boolean inGame = MinecraftClient.getInstance().currentScreen == null;
			int action = down ? 1 : 0;
			if (code < 0) {
				Events.MouseButton e = TatnatClient.EVENTS.post(new Events.MouseButton(code + 100, action, inGame));
				if (e.isCancelled()) ci.cancel();
				return;
			}
			int key = LwjglKeys.toGlfw(code);
			if (key < 0) return;
			Events.Key e = TatnatClient.EVENTS.post(new Events.Key(key, action, inGame));
			if (e.isCancelled()) ci.cancel();
		}
	}

	/** The mouse wheel in game only reaches the hotbar; post it first so Zoom can take it. */
	@Mixin(PlayerInventory.class)
	public static class Wheel {
		@Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true)
		private void tatnat$scroll(int amount, CallbackInfo ci) {
			if (amount == 0) return;
			Events.Scroll e = TatnatClient.EVENTS.post(new Events.Scroll(Math.signum(amount)));
			if (e.isCancelled()) ci.cancel();
		}
	}

	/** Posts {@link Events.Render2D} after the vanilla HUD; hides vanilla's crosshair for ours. */
	@Mixin(InGameHud.class)
	public static class Hud {
		@Inject(method = "render", at = @At("TAIL"))
		private void tatnat$render2d(float partialTick, CallbackInfo ci) {
			com.tatnat.client.mc.HudRender.render(partialTick);
		}

		@Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
		private void tatnat$crosshair(CallbackInfoReturnable<Boolean> cir) {
			if (Crosshair.active()) cir.setReturnValue(false);
		}
	}

	/** Zoom and FOV Modifier on the world FOV, the frame's partial tick, and no hand in Freecam. */
	@Mixin(GameRenderer.class)
	public static class View {
		@Inject(method = "render", at = @At("HEAD"))
		private void tatnat$frame(float partialTick, long nanos, CallbackInfo ci) {
			FeaturesImpl.partialTick = partialTick;
		}

		@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
		private void tatnat$zoom(float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
			if (!useFovSetting) return;
			float fov = cir.getReturnValue();
			if (FovModifier.active()) fov *= FovModifier.INSTANCE.fov.floatValue() / MinecraftClient.getInstance().options.fov;
			Zoom zoom = Zoom.INSTANCE;
			if (zoom != null && zoom.isEnabled()) fov /= (float) zoom.update();
			GameImpl.lastFov = fov;
			cir.setReturnValue(fov);
		}

		/** Freecam: mouse movement turns the floating camera instead of your player. */
		@org.spongepowered.asm.mixin.injection.Redirect(method = "render", at = @At(value = "INVOKE",
				target = "Lnet/minecraft/entity/player/ClientPlayerEntity;increaseTransforms(FF)V"))
		private void tatnat$freecamTurn(net.minecraft.entity.player.ClientPlayerEntity player, float yaw, float pitch) {
			if (Freecam.active() && FeaturesImpl.INSTANCE.freecam() != null) FeaturesImpl.INSTANCE.freecam().increaseTransforms(yaw, pitch);
			else player.increaseTransforms(yaw, pitch);
		}

		@Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
		private void tatnat$hideHand(float partialTick, int pass, CallbackInfo ci) {
			if (Freecam.active()) ci.cancel();
		}
	}

	/** Posts {@link Events.Attack} when you hit an entity (Combo Counter, Reach Display). */
	@Mixin(ClientPlayerInteractionManager.class)
	public static class Attack {
		@Inject(method = "attackEntity", at = @At("HEAD"))
		private void tatnat$attack(PlayerEntity player, Entity target, CallbackInfo ci) {
			TatnatClient.EVENTS.post(new Events.Attack(target));
		}
	}
}
