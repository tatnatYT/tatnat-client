package com.tatnat.client.modules.impl.utility;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

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
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.render.RenderUtils;
import com.tatnat.client.ui.theme.Colors;
import com.tatnat.client.util.WorldProjector;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

/**
 * Named markers that stay visible through walls: a beam up to the sky, a floating name with the
 * distance, and an arrow on the screen edge when the waypoint is behind you or off-screen.
 * Waypoints belong to one world (server address or singleplayer world + dimension).
 */
public class Waypoints extends Module {
	private record Waypoint(String name, double x, double y, double z, int color, String world) {
	}

	private final List<Waypoint> waypoints = new ArrayList<>();

	private final TextSetting newName = add(new TextSetting("Name", "Name for the next waypoint you add", "Home", 32));
	private final ColorSetting color = add(new ColorSetting("Color", "Colour for new waypoints", 0xFFE5323E, true));
	private final KeybindSetting addKey = add(new KeybindSetting("Quick Add Key", "Press in game to drop a waypoint where you stand", GLFW.GLFW_KEY_B));
	private final ActionSetting add = add(new ActionSetting("Add Waypoint Here", "Drop a waypoint at your position", () -> "Add", this::addHere));
	private final ActionSetting removeNearest = add(new ActionSetting("Remove Nearest", "Delete the closest waypoint in this world", () -> "Remove", this::removeNearest));
	private final ActionSetting clear = add(new ActionSetting("Clear This World", "Delete every waypoint in this world", () -> count() + " saved", this::clearWorld));
	private final SliderSetting textScale = add(new SliderSetting("Text Scale", "Size of the floating names", 1.0, 0.5, 3, 0.1, "x"));
	private final BooleanSetting beam = add(new BooleanSetting("Beam", "Vertical line up to the sky", true));
	private final BooleanSetting arrows = add(new BooleanSetting("Edge Arrows", "Arrow at the screen edge when a waypoint is off-screen", true));

	public Waypoints() {
		super("Waypoints", "Mark places and find your way back", Category.UTILITY, false);
		icon = Icons.Icon.PIN;
	}

	private String worldKey() {
		if (mc.level == null) return "";
		String dim = mc.level.dimension().identifier().toString();
		if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip + "|" + dim;
		if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName() + "|" + dim;
		return "?|" + dim;
	}

	private List<Waypoint> here() {
		String key = worldKey();
		List<Waypoint> out = new ArrayList<>();
		for (Waypoint w : waypoints) if (w.world.equals(key)) out.add(w);
		return out;
	}

	private int count() {
		return mc.level == null ? 0 : here().size();
	}

	private void addHere() {
		if (mc.player == null) return;
		String name = newName.get().isBlank() ? "Waypoint" : newName.get().trim();
		waypoints.add(new Waypoint(name, Math.floor(mc.player.getX()) + 0.5, Math.floor(mc.player.getY()), Math.floor(mc.player.getZ()) + 0.5,
				color.get(), worldKey()));
		TatnatClient.CONFIG.markDirty();
	}

	private void removeNearest() {
		if (mc.player == null) return;
		Waypoint best = null;
		double bd = Double.MAX_VALUE;
		for (Waypoint w : here()) {
			double d = mc.player.distanceToSqr(w.x, w.y, w.z);
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
		if (e.inGame && e.action == GLFW.GLFW_PRESS && addKey.isBound() && e.key == addKey.get()) addHere();
	}

	@Subscribe
	public void onGizmos(Events.Gizmos e) {
		if (mc.level == null) return;
		Vec3 cam = new Vec3(e.camX, e.camY, e.camZ);
		for (Waypoint w : here()) {
			Vec3 pos = new Vec3(w.x, w.y, w.z);
			double dist = cam.distanceTo(pos);
			if (beam.on()) {
				Gizmos.line(new Vec3(w.x, mc.level.getMinY(), w.z), new Vec3(w.x, mc.level.getMaxY() + 64, w.z), Colors.withAlpha(w.color, 0xFF), 4f)
						.setAlwaysOnTop();
			}
			// Far waypoints are drawn closer along the same line of sight (and scaled up to match),
			// so they never fall outside the view distance.
			Vec3 label = pos.add(0, 2.2, 0);
			double shown = Math.min(dist, 48);
			Vec3 dir = label.subtract(cam).normalize();
			Vec3 at = cam.add(dir.scale(shown));
			float scale = (float) (textScale.get() * Math.max(0.6, shown / 10.0));
			String text = w.name + "  " + Math.round(dist) + "m";
			Gizmos.billboardText(text, at, TextGizmo.Style.forColorAndCentered(0xFFFFFFFF).withScale(scale)).setAlwaysOnTop();
		}
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!arrows.on() || mc.level == null || mc.options.hideGui) return;
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		GuiGraphics g = e.graphics;
		for (Waypoint wp : here()) {
			double[] p = WorldProjector.project(new Vec3(wp.x, wp.y + 1, wp.z));
			if (p[2] == 1) continue;
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
		waypoints.add(new Waypoint(name, x, y, z, color.get(), worldKey()));
	}
}
