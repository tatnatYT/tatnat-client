package com.tatnat.client.modules.settings;

import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/**
 * A button in a mod's settings (e.g. "Add waypoint here"). Holds no value; clicking runs the
 * action. The button label can change (e.g. to show a count).
 */
public class ActionSetting extends Setting<Runnable> {
	private final Supplier<String> buttonText;

	public ActionSetting(String name, String description, Supplier<String> buttonText, Runnable action) {
		super(name, description, action);
		this.buttonText = buttonText;
	}

	public String buttonText() {
		return buttonText.get();
	}

	public void run() {
		value.run();
	}

	@Override
	public void reset() {
	}

	@Override
	public JsonElement save() {
		return JsonNull.INSTANCE;
	}

	@Override
	public void load(JsonElement json) {
	}
}
