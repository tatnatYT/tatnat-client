package com.tatnat.client.mc;

/**
 * 26.3 moved input from GLFW to SDL: key events carry SDL scancodes and mouse buttons use SDL
 * numbering. The shared code (and saved binds) use GLFW codes, so convert at the edges.
 */
public final class SdlKeys {
	private SdlKeys() {
	}

	private static final int[] SDL_TO_GLFW = new int[512];
	private static final int[] GLFW_TO_SDL = new int[512];

	private static void map(int glfw, int sdl) {
		SDL_TO_GLFW[sdl] = glfw;
		GLFW_TO_SDL[glfw] = sdl;
	}

	static {
		java.util.Arrays.fill(SDL_TO_GLFW, -1);
		java.util.Arrays.fill(GLFW_TO_SDL, -1);
		for (int i = 0; i < 26; i++) map(65 + i, 4 + i); // A-Z
		for (int i = 1; i <= 9; i++) map(48 + i, 29 + i); // 1-9
		map(48, 39); // 0
		map(32, 44); // space
		map(39, 52); // apostrophe
		map(44, 54); // comma
		map(45, 45); // minus
		map(46, 55); // period
		map(47, 56); // slash
		map(59, 51); // semicolon
		map(61, 46); // equal
		map(91, 47); // [
		map(92, 49); // backslash
		map(93, 48); // ]
		map(96, 53); // grave
		map(256, 41); // escape
		map(257, 40); // enter
		map(258, 43); // tab
		map(259, 42); // backspace
		map(260, 73); // insert
		map(261, 76); // delete
		map(262, 79); // right
		map(263, 80); // left
		map(264, 81); // down
		map(265, 82); // up
		map(266, 75); // page up
		map(267, 78); // page down
		map(268, 74); // home
		map(269, 77); // end
		map(280, 57); // caps lock
		map(281, 71); // scroll lock
		map(282, 83); // num lock
		map(283, 70); // print screen
		map(284, 72); // pause
		for (int i = 0; i < 12; i++) map(290 + i, 58 + i); // F1-F12
		for (int i = 0; i < 12; i++) map(302 + i, 104 + i); // F13-F24
		for (int i = 1; i <= 9; i++) map(320 + i, 88 + i); // keypad 1-9
		map(320, 98); // keypad 0
		map(330, 99); // keypad .
		map(331, 84); // keypad /
		map(332, 85); // keypad *
		map(333, 86); // keypad -
		map(334, 87); // keypad +
		map(335, 88); // keypad enter
		map(336, 103); // keypad =
		map(340, 225); // left shift
		map(341, 224); // left ctrl
		map(342, 226); // left alt
		map(343, 227); // left super
		map(344, 229); // right shift
		map(345, 228); // right ctrl
		map(346, 230); // right alt
		map(347, 231); // right super
		map(348, 101); // menu
	}

	public static int toGlfw(int sdl) {
		return sdl >= 0 && sdl < SDL_TO_GLFW.length && SDL_TO_GLFW[sdl] >= 0 ? SDL_TO_GLFW[sdl] : -1;
	}

	public static int toSdl(int glfw) {
		return glfw >= 0 && glfw < GLFW_TO_SDL.length && GLFW_TO_SDL[glfw] >= 0 ? GLFW_TO_SDL[glfw] : -1;
	}

	/** SDL buttons are 1 left, 2 middle, 3 right, 4/5 side; GLFW is 0 left, 1 right, 2 middle, 3/4 side. */
	public static int buttonToGlfw(int sdl) {
		switch (sdl) {
			case 1: return 0;
			case 2: return 2;
			case 3: return 1;
			default: return sdl - 1;
		}
	}

	/** SDL reports a held key's repeats as -1; GLFW used 2. */
	public static int actionToGlfw(int action) {
		return action == -1 ? 2 : action;
	}

	/** SDL's shift modifier bits (either shift key). */
	public static final int MOD_SHIFT = 3;
}
