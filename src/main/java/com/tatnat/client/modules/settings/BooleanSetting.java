package com.tatnat.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** An on/off switch. Drawn as a sliding toggle. */
public class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String name, String description, boolean defaultValue) {
		super(name, description, defaultValue);
	}

	public boolean on() {
		return value;
	}

	public void toggle() {
		set(!value);
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement json) {
		if (json.isJsonPrimitive()) value = json.getAsBoolean();
	}
}
