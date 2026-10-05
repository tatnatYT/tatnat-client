package com.tatnat.client.modules.settings;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** One choice out of a fixed list, e.g. "12h" / "24h". Drawn as a cycle button. */
public class ModeSetting extends Setting<String> {
	public List<String> modes;

	public ModeSetting(String name, String description, String defaultValue, String... modes) {
		super(name, description, defaultValue);
		this.modes = List.of(modes);
	}

	/** Replaces the options (e.g. after rescanning a folder), keeping the value if it still exists. */
	public void setModes(List<String> newModes) {
		modes = List.copyOf(newModes);
		if (!modes.contains(value)) value = modes.contains(defaultValue) ? defaultValue : modes.get(0);
	}

	public boolean is(String mode) {
		return value.equals(mode);
	}

	public void cycle(int direction) {
		int i = modes.indexOf(value);
		value = modes.get(Math.floorMod(i + direction, modes.size()));
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement json) {
		if (json.isJsonPrimitive() && modes.contains(json.getAsString())) value = json.getAsString();
	}
}
