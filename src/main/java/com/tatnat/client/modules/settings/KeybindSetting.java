package com.tatnat.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.tatnat.client.util.Keys;

/** A GLFW key code (or a mouse button, see {@link Keys}); {@link Keys#NONE} means unbound. */
public class KeybindSetting extends Setting<Integer> {
	public KeybindSetting(String name, String description, int defaultKey) {
		super(name, description, defaultKey);
	}

	public boolean isBound() {
		return value != Keys.NONE;
	}

	/** Whether the key is physically held right now (works for mouse buttons too). */
	public boolean isDown() {
		return isBound() && Keys.isDown(value);
	}

	public String keyName() {
		return Keys.name(value);
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement json) {
		if (json.isJsonPrimitive()) value = json.getAsInt();
	}
}
