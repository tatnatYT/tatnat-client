package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.mc.FeaturesImpl;
import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.visual.Zoom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;

/** Mouse buttons and wheel as events, plus lower sensitivity while zoomed. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void tatnat$button(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (window != mc.getWindow().handle()) return;
		Events.MouseButton e = TatnatClient.EVENTS.post(new Events.MouseButton(info.button(), action, mc.screen == null));
		if (e.isCancelled()) ci.cancel();
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void tatnat$scroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (window != mc.getWindow().handle() || mc.screen != null || yOffset == 0) return;
		Events.Scroll e = TatnatClient.EVENTS.post(new Events.Scroll(yOffset));
		if (e.isCancelled()) ci.cancel();
	}

	/** Divides mouse sensitivity by the zoom factor so aiming feels the same zoomed in. */
	@WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
	private Object tatnat$zoomSensitivity(OptionInstance<?> option, Operation<Object> original) {
		Object value = original.call(option);
		Zoom zoom = Zoom.INSTANCE;
		if (zoom != null && zoom.sensitivity.on() && option == Minecraft.getInstance().options.sensitivity()
				&& value instanceof Double d) {
			double factor = zoom.current();
			if (factor > 1.0) return d / Math.sqrt(factor) * 0.9;
		}
		return value;
	}

	/** Freecam: mouse movement turns the floating camera instead of your player. */
	@WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void tatnat$freecamTurn(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
		if (Freecam.active() && FeaturesImpl.INSTANCE.freecam() != null) FeaturesImpl.INSTANCE.freecam().turn(yaw, pitch);
		else original.call(player, yaw, pitch);
	}
}
