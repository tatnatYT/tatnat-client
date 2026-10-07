package com.tatnat.client.mc;

/**
 * 1.8.9 - 1.12.2 use LWJGL 2, whose key codes follow DirectInput scancodes. The shared code (and
 * saved binds) use GLFW key codes, so convert at the edges. Mouse buttons arrive from the game as
 * {@code button - 100}.
 */
public final class LwjglKeys {
	private LwjglKeys() {
	}

	private static final int[] TO_GLFW = new int[256];
	private static final int[] TO_LWJGL = new int[512];

	private static void map(int glfw, int lwjgl) {
		TO_GLFW[lwjgl] = glfw;
		TO_LWJGL[glfw] = lwjgl;
	}

	static {
		java.util.Arrays.fill(TO_GLFW, -1);
		java.util.Arrays.fill(TO_LWJGL, -1);
		map(256, 1); // escape
		for (int i = 1; i <= 9; i++) map(48 + i, 1 + i); // 1-9
		map(48, 11); // 0
		map(45, 12); // minus
		map(61, 13); // equal
		map(259, 14); // backspace
		map(258, 15); // tab
		String row1 = "QWERTYUIOP", row2 = "ASDFGHJKL", row3 = "ZXCVBNM";
		for (int i = 0; i < row1.length(); i++) map(row1.charAt(i), 16 + i);
		for (int i = 0; i < row2.length(); i++) map(row2.charAt(i), 30 + i);
		for (int i = 0; i < row3.length(); i++) map(row3.charAt(i), 44 + i);
		map(91, 26); // [
		map(93, 27); // ]
		map(257, 28); // enter
		map(341, 29); // left ctrl
		map(59, 39); // semicolon
		map(39, 40); // apostrophe
		map(96, 41); // grave
		map(340, 42); // left shift
		map(92, 43); // backslash
		map(44, 51); // comma
		map(46, 52); // period
		map(47, 53); // slash
		map(344, 54); // right shift
		map(332, 55); // keypad *
		map(342, 56); // left alt
		map(32, 57); // space
		map(280, 58); // caps lock
		for (int i = 0; i < 10; i++) map(290 + i, 59 + i); // F1-F10
		map(282, 69); // num lock
		map(281, 70); // scroll lock
		map(327, 71); // keypad 7
		map(328, 72); // keypad 8
		map(329, 73); // keypad 9
		map(333, 74); // keypad -
		map(324, 75); // keypad 4
		map(325, 76); // keypad 5
		map(326, 77); // keypad 6
		map(334, 78); // keypad +
		map(321, 79); // keypad 1
		map(322, 80); // keypad 2
		map(323, 81); // keypad 3
		map(320, 82); // keypad 0
		map(330, 83); // keypad .
		map(300, 87); // F11
		map(301, 88); // F12
		for (int i = 0; i < 6; i++) map(302 + i, 100 + i); // F13-F18
		map(335, 156); // keypad enter
		map(345, 157); // right ctrl
		map(331, 181); // keypad /
		map(283, 183); // print screen
		map(346, 184); // right alt
		map(284, 197); // pause
		map(268, 199); // home
		map(265, 200); // up
		map(266, 201); // page up
		map(263, 203); // left
		map(262, 205); // right
		map(269, 207); // end
		map(264, 208); // down
		map(267, 209); // page down
		map(260, 210); // insert
		map(261, 211); // delete
		map(343, 219); // left super
		map(347, 220); // right super
		map(348, 221); // menu
	}

	public static int toGlfw(int lwjgl) {
		return lwjgl >= 0 && lwjgl < TO_GLFW.length ? TO_GLFW[lwjgl] : -1;
	}

	public static int toLwjgl(int glfw) {
		return glfw >= 0 && glfw < TO_LWJGL.length ? TO_LWJGL[glfw] : -1;
	}
}
