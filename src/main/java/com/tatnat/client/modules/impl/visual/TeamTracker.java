package com.tatnat.client.modules.impl.visual;

import java.util.Locale;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.platform.EntityInfo;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.util.WorldProjector;

/**
 * Keeps track of your teammates: a label over each one (health and distance) that shows through
 * walls, and an arrow at the screen edge when they're behind you. Teammates are the names you list.
 */
public class TeamTracker extends Module {
	private final TextSetting names = add(new TextSetting("Teammates", "Player names, separated by commas", "", 256));
	private final BooleanSetting health = add(new BooleanSetting("Show Health", "Show their health", true));
	private final BooleanSetting distance = add(new BooleanSetting("Show Distance", "Show how far away they are", true));
	private final BooleanSetting arrows = add(new BooleanSetting("Off-Screen Arrows", "Point to teammates you can't see", true));
	private final ColorSetting color = add(new ColorSetting("Color", "Label and arrow colour", 0xFF55FF55, true));

	public TeamTracker() {
		super("Team Tracker", "Shows where your teammates are", Category.VISUAL, false);
		icon = Icons.Icon.FLAG;
	}

	private boolean teammate(String name) {
		String n = name.toLowerCase(Locale.ROOT);
		for (String p : names.get().toLowerCase(Locale.ROOT).split(",")) if (!p.trim().isEmpty() && n.equals(p.trim())) return true;
		return false;
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden() || names.get().trim().isEmpty()) return;
		int w = game().guiWidth(), h = game().guiHeight();
		double px = game().x(), py = game().y(), pz = game().z();
		for (EntityInfo p : game().entities(512)) {
			if (p.kind != EntityInfo.Kind.PLAYER || !teammate(p.name)) continue;
			double d = Math.sqrt((p.x - px) * (p.x - px) + (p.top - py) * (p.top - py) + (p.z - pz) * (p.z - pz));
			double[] s = WorldProjector.project(p.x, p.top + 0.6, p.z);
			int c = color.color(0);
			if (s[2] == 1) {
				StringBuilder t = new StringBuilder(p.name);
				if (health.on()) t.append("  ").append(String.format(Locale.ROOT, "%.0f❤", p.health / 2f));
				if (distance.on()) t.append("  ").append(Math.round(d)).append('m');
				WorldLabels.text(e.gfx, s, t.toString(), c, 1f);
			} else if (arrows.on()) {
				double ang = Math.atan2(s[1] - h / 2.0, s[0] - w / 2.0);
				double rx = w / 2.0 - 18, ry = h / 2.0 - 18;
				double k = Math.min(rx / Math.max(1e-6, Math.abs(Math.cos(ang))), ry / Math.max(1e-6, Math.abs(Math.sin(ang))));
				int scale = RenderUtils.beginPixels(e.gfx);
				Icons.arrow(e.gfx, (float) (w / 2.0 + Math.cos(ang) * k) * scale, (float) (h / 2.0 + Math.sin(ang) * k) * scale, (float) ang, 10 * scale, c);
				RenderUtils.end(e.gfx);
			}
		}
	}
}
