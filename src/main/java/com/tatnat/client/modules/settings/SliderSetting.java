package com.tatnat.client.modules.settings;

import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A number between {@code min} and {@code max}, snapped to {@code step}. Drawn as a slider. */
public class SliderSetting extends Setting<Double> {
	public final double min, max, step;
	/** Appended to the value in the menu, e.g. "x" for "1.25x" or "ms". */
	public final String suffix;

	public SliderSetting(String name, String description, double defaultValue, double min, double max, double step, String suffix) {
		super(name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	@Override
	public void set(Double v) {
		double snapped = Math.round((v - min) / step) * step + min;
		// Kill float noise like 1.2500000000002 so the label and the config stay clean.
		snapped = Math.round(snapped * 10000.0) / 10000.0;
		value = Math.max(min, Math.min(max, snapped));
	}

	public float floatValue() {
		return value.floatValue();
	}

	public int intValue() {
		return (int) Math.round(value);
	}

	/** 0..1 position of the value along the slider. */
	public double fraction() {
		return (value - min) / (max - min);
	}

	public String display() {
		int decimals = step >= 1 ? 0 : step >= 0.1 ? 1 : 2;
		return String.format(Locale.ROOT, "%." + decimals + "f", value) + suffix;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement json) {
		if (json.isJsonPrimitive()) set(json.getAsDouble());
	}
}
