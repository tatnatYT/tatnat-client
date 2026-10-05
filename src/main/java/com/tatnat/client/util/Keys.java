package com.tatnat.client.util;

import com.tatnat.client.TatnatClient;

/**
 * Key codes used by keybind settings. Keyboard keys are GLFW key codes; mouse buttons are
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
		if (isMouse(code)) return TatnatClient.game().rawMouseDown(MOUSE_BASE - code);
		return TatnatClient.game().rawKeyDown(code);
	}

	public static String name(int code) {
		if (code == NONE) return "None";
		if (isMouse(code)) {
			int b = MOUSE_BASE - code;
			switch (b) {
				case 0: return "LMB";
				case 1: return "RMB";
				case 2: return "MMB";
				default: return "Mouse " + (b + 1);
			}
		}
		return KeyCodes.name(code);
	}
}
