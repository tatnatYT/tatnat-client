package com.tatnat.client.util;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;

/**
 * Key codes used by keybind settings. Keyboard keys are plain GLFW key codes; mouse buttons are
 * stored as {@code MOUSE_BASE - button} so one int can hold either.
 */
public final class Keys {
	public static final int NONE = -1;
	private static final int MOUSE_BASE = -100;

	private Keys() {
	}

	public static int fromMouseButton(int button) {
		return MOUSE_BASE - button;
	}

	public static boolean isMouse(int code) {
		return code <= MOUSE_BASE;
	}

	public static boolean isDown(int code) {
		if (code == NONE) return false;
		long window = Minecraft.getInstance().getWindow().handle();
		if (isMouse(code)) return GLFW.glfwGetMouseButton(window, MOUSE_BASE - code) == GLFW.GLFW_PRESS;
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), code);
	}

	public static String name(int code) {
		if (code == NONE) return "None";
		if (isMouse(code)) {
			int b = MOUSE_BASE - code;
			return switch (b) {
				case 0 -> "LMB";
				case 1 -> "RMB";
				case 2 -> "MMB";
				default -> "Mouse " + (b + 1);
			};
		}
		return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
	}
}
