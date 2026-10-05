package com.tatnat.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Free text, e.g. a custom Auto GG message. Drawn as a text field. */
public class TextSetting extends Setting<String> {
	public final int maxLength;

	public TextSetting(String name, String description, String defaultValue, int maxLength) {
		super(name, description, defaultValue);
		this.maxLength = maxLength;
	}

	@Override
	public void set(String v) {
		value = v.length() > maxLength ? v.substring(0, maxLength) : v;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement json) {
		if (json.isJsonPrimitive()) set(json.getAsString());
	}
}
