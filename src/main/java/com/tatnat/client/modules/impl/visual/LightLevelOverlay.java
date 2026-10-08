package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.util.WorldProjector;

/**
 * Marks the ground around you: a red X where mobs can spawn in the dark (block light 0, or 7 and
 * below on 1.17 and older) and a green dot on lit, safe spots. Also serves as the Block Indicator.
 */
public class LightLevelOverlay extends Module {
	private final SliderSetting radius = add(new SliderSetting("Radius", "How far around you to check", 8, 4, 16, 1, " blocks"));
	private final BooleanSetting spawnable = add(new BooleanSetting("Show Spawnable", "Red X where mobs can spawn", true));
	private final BooleanSetting safe = add(new BooleanSetting("Show Safe", "Green dot where they can't", false));
	private final BooleanSetting numbers = add(new BooleanSetting("Show Light Level", "Write the light level on each block", false));

	/** Cached scan, refreshed twice a second: {x, y, z, light} per surface block. */
	private int[] spots = new int[0];
	private int count;
	private long scannedAt;

	public LightLevelOverlay() {
		super("Light Level Overlay", "Shows where mobs can spawn", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.SUN;
	}

	private int spawnLimit() {
		String v = game().minecraftVersion();
		String[] p = v.split("\\.");
		try {
			// 1.18 changed the rule to "block light 0".
			return p[0].equals("1") && Integer.parseInt(p[1]) <= 17 ? 7 : 0;
		} catch (RuntimeException e) {
			return 0;
		}
	}

	private void scan() {
		int r = radius.intValue();
		int px = (int) Math.floor(game().x()), py = (int) Math.floor(game().y()), pz = (int) Math.floor(game().z());
		int[] out = new int[(2 * r + 1) * (2 * r + 1) * 4 * 3];
		int n = 0;
		for (int x = px - r; x <= px + r; x++) {
			for (int z = pz - r; z <= pz + r; z++) {
				for (int y = py + 2; y >= py - 3; y--) {
					if (!game().spawnSurface(x, y, z)) continue;
					if (n + 4 > out.length) break;
					out[n++] = x;
					out[n++] = y;
					out[n++] = z;
					out[n++] = game().blockLight(x, y, z);
					break;
				}
			}
		}
		spots = out;
		count = n;
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden()) return;
		long now = System.currentTimeMillis();
		if (now - scannedAt > 500) {
			scannedAt = now;
			scan();
		}
		Gfx g = e.gfx;
		int limit = spawnLimit();
		for (int i = 0; i < count; i += 4) {
			int light = spots[i + 3];
			boolean danger = light <= limit;
			if (danger ? !spawnable.on() : !safe.on()) continue;
			double[] p = WorldProjector.project(spots[i] + 0.5, spots[i + 1] + 0.02, spots[i + 2] + 0.5);
			if (p[2] != 1) continue;
			int s = (int) Math.max(2, Math.min(6, 18 / Math.max(1, p[3])));
			int x = (int) p[0], y = (int) p[1];
			if (danger) {
				for (int k = -s; k <= s; k++) {
					g.rect(x + k, y + k, x + k + 1, y + k + 1, 0xE0FF3030);
					g.rect(x + k, y - k, x + k + 1, y - k + 1, 0xE0FF3030);
				}
			} else {
				g.rect(x - 1, y - 1, x + 2, y + 2, 0xC055FF55);
			}
			if (numbers.on()) g.mcText(String.valueOf(light), x - 2, y + s + 1, danger ? 0xFFFF5555 : 0xFF55FF55, true, false);
		}
	}
}
