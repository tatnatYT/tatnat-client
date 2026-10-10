package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.RenderUtils;

/**
 * Replaces the vanilla crosshair (the platform hides vanilla's while this is on) with a crisp
 * one drawn in real pixels. In dynamic mode the arms spread apart while you move or jump.
 */
public class Crosshair extends Module {
	public static Crosshair INSTANCE;

	private final ModeSetting style = add(new ModeSetting("Style", "Shape of the crosshair", "Plus", "Plus", "Plus + Dot", "Dot", "Circle", "Circle + Dot", "X"));
	private final ColorSetting color = add(new ColorSetting("Color", "Crosshair colour", 0xFFFFFFFF, true));
	private final SliderSetting size = add(new SliderSetting("Size", "Length of each arm in pixels", 7, 2, 25, 1, "px"));
	private final SliderSetting thickness = add(new SliderSetting("Thickness", "Line width in pixels", 2, 1, 6, 1, "px"));
	private final SliderSetting gap = add(new SliderSetting("Gap", "Space in the middle in pixels", 3, 0, 15, 1, "px"));
	private final BooleanSetting outline = add(new BooleanSetting("Outline", "Thin black border so it shows on bright blocks", true));
	private final BooleanSetting dynamic = add(new BooleanSetting("Dynamic", "Spread out while moving or jumping", false));

	private float spread;
	private long lastFrame = System.nanoTime();

	public Crosshair() {
		super("Crosshair", "Your own crosshair: shape, colour, size and gap", Category.VISUAL, false);
		icon = Icons.Icon.CROSSHAIR;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (game().hudHidden() || !game().firstPerson() || !game().inWorld()) return;
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		float target = 0;
		if (dynamic.on()) {
			target = (float) Math.min(8, game().horizontalSpeed() * 30) + (game().onGround() ? 0 : 4);
		}
		spread += (target - spread) * Math.min(1f, dt * 12f);

		Gfx g = e.gfx;
		RenderUtils.beginPixels(g);
		int cx = game().windowWidth() / 2, cy = game().windowHeight() / 2;
		int t = thickness.intValue(), len = size.intValue(), gp = gap.intValue() + Math.round(spread);
		String s = style.get();
		if (outline.on()) drawShape(g, s, cx, cy, t, len, gp, 0xC0000000, 1);
		drawShape(g, s, cx, cy, t, len, gp, color.color(), 0);
		RenderUtils.end(g);

		// The vanilla attack indicator lives in the crosshair we replace: draw it ourselves.
		if (game().attackIndicatorMode() == 1) {
			float charge = game().attackStrength();
			if (charge < 1f) {
				int x = game().guiWidth() / 2 - 8, y = game().guiHeight() / 2 + 9;
				g.rect(x, y, x + 16, y + 3, 0xA0000000);
				g.rect(x + 1, y + 1, x + 1 + Math.round(14 * charge), y + 2, 0xFFFFFFFF);
			}
		}
	}

	private void drawShape(Gfx g, String s, int cx, int cy, int t, int len, int gp, int c, int grow) {
		int half = t / 2;
		if (s.startsWith("Plus")) {
			RenderUtils.rect(g, cx - half - grow, cy - gp - len - grow, cx - half + t + grow, cy - gp + grow, c);
			RenderUtils.rect(g, cx - half - grow, cy + gp - grow, cx - half + t + grow, cy + gp + len + grow, c);
			RenderUtils.rect(g, cx - gp - len - grow, cy - half - grow, cx - gp + grow, cy - half + t + grow, c);
			RenderUtils.rect(g, cx + gp - grow, cy - half - grow, cx + gp + len + grow, cy - half + t + grow, c);
		}
		if (s.startsWith("Circle")) {
			int r = Math.max(len, gp + t + 2);
			RenderUtils.roundedOutline(g, cx - r - grow, cy - r - grow, (r + grow) * 2, (r + grow) * 2, r + grow, t + grow * 2, c);
		}
		if (s.equals("X")) {
			int[][] dirs = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
			for (int i = gp; i < gp + len; i++) {
				for (int[] d : dirs) {
					int px = cx + d[0] * i, py = cy + d[1] * i;
					RenderUtils.rect(g, px - half - grow, py - half - grow, px - half + t + grow, py - half + t + grow, c);
				}
			}
		}
		if (s.endsWith("Dot")) {
			int d = Math.max(2, t + 1);
			RenderUtils.circle(g, cx, cy, d / 2 + 1 + grow, c);
		}
	}
}
