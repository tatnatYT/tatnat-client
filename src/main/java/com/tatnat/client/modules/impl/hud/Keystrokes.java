package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.ui.theme.Theme;
import com.tatnat.client.util.CpsTracker;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;

/**
 * WASD + mouse buttons (+ optional space bar) that light up while held.
 *
 * Each key fades between its idle and pressed colours over ~80ms instead of snapping, and in
 * chroma mode the rainbow is offset by the key's position so it sweeps across the keyboard as a
 * wave. Reads the actual key bindings, so it follows the player's own controls (ESDF etc.).
 */
public class Keystrokes extends HudModule {
	private final ModeSetting layout = add(new ModeSetting("Layout", "Which keys to show", "WASD + Mouse", "WASD + Mouse", "WASD + Mouse + Space", "WASD"));
	private final BooleanSetting showCps = add(new BooleanSetting("Show CPS", "Tiny clicks-per-second counter on the mouse buttons", true));
	private final ColorSetting pressedColor = add(new ColorSetting("Pressed Color", "Colour of a held key (turn on Chroma for rainbow)", Theme.ACCENT, true));
	private final ColorSetting keyColor = add(new ColorSetting("Key Color", "Background of a key that isn't held", 0x6F000000, false));
	private final ColorSetting textColor = add(new ColorSetting("Text Color", "Colour of the letters", 0xFFFFFFFF, false));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the letters", false));

	private static final int KEY = 22, GAP = 2;
	/** Press animation per key, 0 = idle, 1 = fully lit. Index order: W A S D LMB RMB SPACE. */
	private final float[] glow = new float[7];
	private long lastFrame = System.nanoTime();

	public Keystrokes() {
		super("Keystrokes", "Shows WASD and mouse buttons lighting up as you press them", true, 0.0, 0.55);
		icon = com.tatnat.client.ui.render.Icons.Icon.KEYBOARD;
	}

	@Override
	protected long draw(GuiGraphics g, boolean preview) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;

		int full = KEY * 3 + GAP * 2;
		int y = 0;
		drawKey(g, 0, mc.options.keyUp, KEY + GAP, y, KEY, KEY, dt, null);
		y += KEY + GAP;
		drawKey(g, 1, mc.options.keyLeft, 0, y, KEY, KEY, dt, null);
		drawKey(g, 2, mc.options.keyDown, KEY + GAP, y, KEY, KEY, dt, null);
		drawKey(g, 3, mc.options.keyRight, (KEY + GAP) * 2, y, KEY, KEY, dt, null);
		y += KEY + GAP;
		if (!layout.is("WASD")) {
			int half = (full - GAP) / 2;
			drawKey(g, 4, mc.options.keyAttack, 0, y, half, KEY, dt, "LMB");
			drawKey(g, 5, mc.options.keyUse, half + GAP, y, full - half - GAP, KEY, dt, "RMB");
			y += KEY + GAP;
		}
		if (layout.is("WASD + Mouse + Space")) {
			drawKey(g, 6, mc.options.keyJump, 0, y, full, 12, dt, "space");
			y += 12 + GAP;
		}
		return size(full, y - GAP);
	}

	private void drawKey(GuiGraphics g, int index, KeyMapping key, int x, int y, int w, int h, float dt, String label) {
		boolean down = key.isDown();
		// ~80ms ease towards the target state.
		float target = down ? 1f : 0f;
		glow[index] += (target - glow[index]) * Math.min(1f, dt * 14f);

		int lit = pressedColor.color((x + y) / 160.0);
		int bg = Colors.lerp(keyColor.get(), Colors.withAlpha(lit, 0xC0), glow[index]);
		g.fill(x, y, x + w, y + h, bg);

		String text;
		if ("space".equals(label)) {
			// A short bar instead of the word, like a real space bar.
			int bw = w / 3, by = y + h / 2;
			g.fill(x + (w - bw) / 2, by - 1, x + (w + bw) / 2, by, Colors.lerp(textColor.get(), 0xFF000000, glow[index] * 0.7f));
			return;
		}
		text = label != null ? label : keyName(key);
		int tc = Colors.lerp(textColor.get(), 0xFF101014, glow[index] * 0.85f);
		boolean mouse = index == 4 || index == 5;
		if (mouse && showCps.on()) {
			g.drawString(mc.font, text, x + (w - mc.font.width(text)) / 2, y + 4, tc, shadow.on());
			String cps = CpsTracker.INSTANCE.cps(index - 4) + " CPS";
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2f, y + 14);
			g.pose().scale(0.5f, 0.5f);
			g.drawString(mc.font, cps, -mc.font.width(cps) / 2, 0, tc, false);
			g.pose().popMatrix();
		} else {
			g.drawString(mc.font, text, x + (w - mc.font.width(text)) / 2, y + (h - 8) / 2 + 1, tc, shadow.on());
		}
	}

	/** "W", "A"... from the actual binding, shortened for keys like "Left Shift". */
	private static String keyName(KeyMapping key) {
		String s = key.getTranslatedKeyMessage().getString();
		return s.length() <= 3 ? s.toUpperCase() : s.substring(0, 1).toUpperCase();
	}
}
