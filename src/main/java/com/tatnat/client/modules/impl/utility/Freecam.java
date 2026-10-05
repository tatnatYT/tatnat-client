package com.tatnat.client.modules.impl.utility;

import org.lwjgl.glfw.GLFW;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

import net.minecraft.util.Mth;

/**
 * Detaches the camera from your body so you can fly around and look. Your player stays exactly
 * where it is and receives no movement input ({@code KeyboardInputMixin}); mouse movement turns
 * the camera ({@code MouseHandlerMixin}); clicks are blocked so you can't hit or place blocks
 * from the camera position. Toggle with F4 by default.
 *
 * Note: many PvP servers forbid freecam. It only moves your view (the server sees you standing
 * still), but use it where it's allowed.
 */
public class Freecam extends Module {
	public static Freecam INSTANCE;

	private final SliderSetting speed = add(new SliderSetting("Speed", "Flying speed", 1.0, 0.1, 5, 0.1, "x"));
	private final BooleanSetting hideBody = add(new BooleanSetting("Hide Body", "Make your own player invisible while flying", true));

	private FreeCamera camera;

	public Freecam() {
		super("Freecam", "Fly the camera around without moving (F4)", Category.UTILITY, false);
		icon = Icons.Icon.CAMERA;
		defaultToggleKey(GLFW.GLFW_KEY_F4);
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.camera != null;
	}

	public static FreeCamera camera() {
		return INSTANCE == null ? null : INSTANCE.camera;
	}

	public boolean hideBody() {
		return hideBody.on();
	}

	@Override
	protected void onEnable() {
		if (mc.player == null || mc.level == null) {
			// Can't fly outside a world: switch straight back off.
			mc.execute(() -> setEnabled(false));
			return;
		}
		camera = new FreeCamera(mc.level);
		camera.setPos(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
		camera.xo = camera.getX();
		camera.yo = camera.getY();
		camera.zo = camera.getZ();
		camera.setYRot(mc.player.getYRot());
		camera.setXRot(mc.player.getXRot());
		camera.yRotO = camera.getYRot();
		camera.xRotO = camera.getXRot();
		mc.setCameraEntity(camera);
	}

	@Override
	protected void onDisable() {
		camera = null;
		if (mc.player != null) mc.setCameraEntity(mc.player);
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (camera == null) return;
		if (mc.player == null || mc.level == null || camera.level() != mc.level) {
			setEnabled(false);
			return;
		}
		if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
		if (mc.screen != null) {
			camera.moveBy(0, 0, 0);
			return;
		}
		double fwd = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
		double side = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
		double up = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
		double sp = speed.get() * (mc.options.keySprint.isDown() ? 2.5 : 1.0);
		float yaw = camera.getYRot() * Mth.DEG_TO_RAD;
		double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
		double dx = (-sin * fwd + cos * side), dz = (cos * fwd + sin * side);
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len > 1) {
			dx /= len;
			dz /= len;
		}
		camera.moveBy(dx * sp, up * sp, dz * sp);
	}

	/** Blocks attacking / using from the floating camera. */
	@Subscribe
	public void onMouse(Events.MouseButton e) {
		if (camera != null && e.inGame && (e.button == 0 || e.button == 1)) e.cancel();
	}
}
