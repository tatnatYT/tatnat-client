package com.tatnat.client.modules.impl.utility;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.platform.Gfx;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.util.KeyCodes;
import com.tatnat.client.util.WorldProjector;

/**
 * Named markers that stay visible through walls: a beam up to the sky, a floating name with the
 * distance, and an arrow on the screen edge when the waypoint is behind you or off-screen.
 * Everything is projected onto the screen, so it looks the same on every version. Waypoints
 * belong to one world (server address or singleplayer world + dimension).
 */
public class Waypoints extends Module {
	private static final class Waypoint {
		final String name, world;
		final double x, y, z;
		final int color;

		Waypoint(String name, double x, double y, double z, int color, String world) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.color = color;
			this.world = world;
		}
	}

	private final List<Waypoint> waypoints = new ArrayList<>();

	private final TextSetting newName = add(new TextSetting("Name", "Name for the next waypoint you add", "Home", 32));
	private final ColorSetting color = add(new ColorSetting("Color", "Colour for new waypoints", 0xFFE5323E, true));
	private final KeybindSetting addKey = add(new KeybindSetting("Quick Add Key", "Press in game to drop a waypoint where you stand", KeyCodes.B));
	private final ActionSetting addHere = add(new ActionSetting("Add Waypoint Here", "Drop a waypoint at your position", () -> "Add", this::addHere));
	private final ActionSetting removeNearest = add(new ActionSetting("Remove Nearest", "Delete the closest waypoint in this world", () -> "Remove", this::removeNearest));
	private final ActionSetting clear = add(new ActionSetting("Clear This World", "Delete every waypoint in this world", () -> count() + " saved", this::clearWorld));
	private final SliderSetting textScale = add(new SliderSetting("Text Scale", "Size of the floating names", 1.0, 0.5, 3, 0.1, "x"));
	private final BooleanSetting beam = add(new BooleanSetting("Beam", "Vertical line up to the sky", true));
	private final BooleanSetting arrows = add(new BooleanSetting("Edge Arrows", "Arrow at the screen edge when a waypoint is off-screen", true));

	public Waypoints() {
		super("Waypoints", "Mark places and find your way back", Category.UTILITY, false);
		icon = Icons.Icon.PIN;
	}

	private List<Waypoint> here() {
		String key = game().worldKey();
		List<Waypoint> out = new ArrayList<>();
		for (Waypoint w : waypoints) if (w.world.equals(key)) out.add(w);
		return out;
	}

	private int count() {
		return game().inWorld() ? here().size() : 0;
	}

	private void addHere() {
		if (!game().inWorld()) return;
		String name = newName.get().trim().isEmpty() ? "Waypoint" : newName.get().trim();
		waypoints.add(new Waypoint(name, Math.floor(game().x()) + 0.5, Math.floor(game().y()), Math.floor(game().z()) + 0.5,
				color.get(), game().worldKey()));
		TatnatClient.CONFIG.markDirty();
	}

	private void removeNearest() {
		if (!game().inWorld()) return;
		Waypoint best = null;
		double bd = Double.MAX_VALUE;
		for (Waypoint w : here()) {
			double dx = w.x - game().x(), dy = w.y - game().y(), dz = w.z - game().z();
			double d = dx * dx + dy * dy + dz * dz;
			if (d < bd) {
				bd = d;
				best = w;
			}
		}
		if (best != null) waypoints.remove(best);
		TatnatClient.CONFIG.markDirty();
	}

	private void clearWorld() {
		waypoints.removeAll(here());
		TatnatClient.CONFIG.markDirty();
	}

	@Subscribe
	public void onKey(Events.Key e) {
		if (e.inGame && e.action == KeyCodes.PRESS && addKey.isBound() && e.key == addKey.get()) addHere();
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!game().inWorld() || game().hudHidden()) return;
		Gfx g = e.gfx;
		int w = game().guiWidth(), h = game().guiHeight();
		double px = game().x(), py = game().eyeY(), pz = game().z();
		for (Waypoint wp : here()) {
			double dist = Math.sqrt((wp.x - px) * (wp.x - px) + (wp.y - py) * (wp.y - py) + (wp.z - pz) * (wp.z - pz));
			if (beam.on()) drawBeam(g, wp);
			double[] p = WorldProjector.project(wp.x, wp.y + 2.2, wp.z);
			if (p[2] == 1) {
				drawLabel(g, wp, dist, p);
			} else if (arrows.on()) {
				double dx = p[0] - w / 2.0, dy = p[1] - h / 2.0;
				double ang = Math.atan2(dy, dx);
				double rx = w / 2.0 - 18, ry = h / 2.0 - 18;
				// Point on the screen-edge rectangle in that direction.
				double t = Math.min(rx / Math.max(1e-6, Math.abs(Math.cos(ang))), ry / Math.max(1e-6, Math.abs(Math.sin(ang))));
				float ax = (float) (w / 2.0 + Math.cos(ang) * t), ay = (float) (h / 2.0 + Math.sin(ang) * t);
				int scale = RenderUtils.beginPixels(g);
				Icons.arrow(g, ax * scale, ay * scale, (float) ang, 11 * scale, wp.color);
				RenderUtils.end(g);
			}
		}
	}

	/** Vertical beam: sampled up the column and drawn segment by segment where it's in view. */
	private void drawBeam(Gfx g, Waypoint wp) {
		int scale = RenderUtils.beginPixels(g);
		double y0 = game().worldMinY(), y1 = game().worldMaxY() + 64;
		double[] prev = null;
		int steps = 48;
		for (int i = 0; i <= steps; i++) {
			double y = y0 + (y1 - y0) * i / steps;
			double[] p = WorldProjector.project(wp.x, y, wp.z);
			if (p[3] > 0.05 && prev != null && prev[3] > 0.05) {
				float width = (float) Math.max(1.5, Math.min(6, 40 / Math.max(1, p[3]))) * scale / 2f;
				Icons.thickLine(g, (float) prev[0] * scale, (float) prev[1] * scale, (float) p[0] * scale, (float) p[1] * scale, width,
						Colors.withAlpha(wp.color, 0xC0));
			}
			prev = p;
		}
		RenderUtils.end(g);
	}

	private void drawLabel(Gfx g, Waypoint wp, double dist, double[] p) {
		String text = wp.name + "  " + Math.round(dist) + "m";
		float s = textScale.floatValue();
		int tw = g.mcTextWidth(text, false);
		g.push();
		g.translate((float) p[0], (float) p[1]);
		g.scale(s, s);
		g.rect(-tw / 2 - 4, -6, tw / 2 + 4, 6, 0x90000000);
		g.rect(-tw / 2 - 4, -6, -tw / 2 - 2, 6, wp.color);
		g.mcText(text, -tw / 2, -4, 0xFFFFFFFF, false, false);
		g.pop();
	}

	@Override
	public JsonObject save() {
		JsonObject o = super.save();
		JsonArray arr = new JsonArray();
		for (Waypoint w : waypoints) {
			JsonObject j = new JsonObject();
			j.addProperty("name", w.name);
			j.addProperty("x", w.x);
			j.addProperty("y", w.y);
			j.addProperty("z", w.z);
			j.addProperty("color", w.color);
			j.addProperty("world", w.world);
			arr.add(j);
		}
		o.add("waypoints", arr);
		return o;
	}

	@Override
	public void load(JsonObject o) {
		super.load(o);
		waypoints.clear();
		if (o.has("waypoints") && o.get("waypoints").isJsonArray()) {
			for (JsonElement el : o.getAsJsonArray("waypoints")) {
				try {
					JsonObject j = el.getAsJsonObject();
					waypoints.add(new Waypoint(j.get("name").getAsString(), j.get("x").getAsDouble(), j.get("y").getAsDouble(),
							j.get("z").getAsDouble(), j.get("color").getAsInt(), j.get("world").getAsString()));
				} catch (RuntimeException ignored) {
					// Skip a damaged entry rather than losing all waypoints.
				}
			}
		}
	}

	/** Dev test hook. */
	public void devAdd(String name, double x, double y, double z) {
		waypoints.add(new Waypoint(name, x, y, z, color.get(), game().worldKey()));
	}
}
