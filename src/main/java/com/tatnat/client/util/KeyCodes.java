package com.tatnat.client.util;

/**
 * GLFW key codes, defined here so shared code doesn't depend on LWJGL 3 (1.8.9 - 1.12.2 run on
 * LWJGL 2, whose platform layers translate their codes to these).
 */
public final class KeyCodes {
	private KeyCodes() {
	}

	public static final int PRESS = 1, RELEASE = 0, REPEAT = 2;
	public static final int MOD_SHIFT = 1;

	public static final int SPACE = 32, APOSTROPHE = 39, COMMA = 44, MINUS = 45, PERIOD = 46, SLASH = 47;
	public static final int KEY_0 = 48, KEY_9 = 57, SEMICOLON = 59, EQUAL = 61;
	public static final int A = 65, B = 66, C = 67, R = 82, V = 86, Z = 90;
	public static final int LEFT_BRACKET = 91, BACKSLASH = 92, RIGHT_BRACKET = 93, GRAVE = 96;
	public static final int ESCAPE = 256, ENTER = 257, TAB = 258, BACKSPACE = 259, INSERT = 260, DELETE = 261;
	public static final int RIGHT = 262, LEFT = 263, DOWN = 264, UP = 265, PAGE_UP = 266, PAGE_DOWN = 267, HOME = 268, END = 269;
	public static final int CAPS_LOCK = 280, F1 = 290, F4 = 293, F12 = 301;
	public static final int KP_0 = 320, KP_1 = 321, KP_9 = 329, KP_DECIMAL = 330, KP_DIVIDE = 331, KP_MULTIPLY = 332;
	public static final int KP_SUBTRACT = 333, KP_ADD = 334, KP_ENTER = 335;
	public static final int LEFT_SHIFT = 340, LEFT_CONTROL = 341, LEFT_ALT = 342, RIGHT_SHIFT = 344, RIGHT_CONTROL = 345, RIGHT_ALT = 346;

	/** Readable name for a GLFW key code (layout-independent; letters as on a US keyboard). */
	public static String name(int key) {
		if (key >= A && key <= Z) return String.valueOf((char) key);
		if (key >= KEY_0 && key <= KEY_9) return String.valueOf((char) key);
		if (key >= F1 && key <= F12 + 13) return "F" + (key - F1 + 1);
		if (key >= KP_0 && key <= KP_9) return "Num " + (key - KP_0);
		switch (key) {
			case SPACE: return "Space";
			case APOSTROPHE: return "'";
			case COMMA: return ",";
			case MINUS: return "-";
			case PERIOD: return ".";
			case SLASH: return "/";
			case SEMICOLON: return ";";
			case EQUAL: return "=";
			case LEFT_BRACKET: return "[";
			case BACKSLASH: return "\\";
			case RIGHT_BRACKET: return "]";
			case GRAVE: return "`";
			case ESCAPE: return "Esc";
			case ENTER: return "Enter";
			case TAB: return "Tab";
			case BACKSPACE: return "Backspace";
			case INSERT: return "Insert";
			case DELETE: return "Delete";
			case RIGHT: return "Right";
			case LEFT: return "Left";
			case DOWN: return "Down";
			case UP: return "Up";
			case PAGE_UP: return "Page Up";
			case PAGE_DOWN: return "Page Down";
			case HOME: return "Home";
			case END: return "End";
			case CAPS_LOCK: return "Caps Lock";
			case KP_DECIMAL: return "Num .";
			case KP_DIVIDE: return "Num /";
			case KP_MULTIPLY: return "Num *";
			case KP_SUBTRACT: return "Num -";
			case KP_ADD: return "Num +";
			case KP_ENTER: return "Num Enter";
			case LEFT_SHIFT: return "Left Shift";
			case LEFT_CONTROL: return "Left Ctrl";
			case LEFT_ALT: return "Left Alt";
			case RIGHT_SHIFT: return "Right Shift";
			case RIGHT_CONTROL: return "Right Ctrl";
			case RIGHT_ALT: return "Right Alt";
			default: return "Key " + key;
		}
	}
}
