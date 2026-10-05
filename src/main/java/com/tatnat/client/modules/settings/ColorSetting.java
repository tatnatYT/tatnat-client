package com.tatnat.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tatnat.client.ui.theme.Colors;

/**
 * An ARGB colour with an optional chroma (rainbow) mode. The value is the base colour; use
 * {@link #color(double)} when drawing so chroma is applied.
 */
public class ColorSetting extends Setting<Integer> {
	private boolean chroma;
	public final boolean allowChroma;

	public ColorSetting(String name, String description, int defaultArgb, boolean allowChroma) {
		super(name, description, defaultArgb);
		this.allowChroma = allowChroma;
	}

	public boolean chroma() {
		return chroma && allowChroma;
	}

	public void setChroma(boolean chroma) {
		this.chroma = chroma;
	}

	/** The colour to draw with. {@code offset} shifts the rainbow so neighbouring elements form a wave. */
	public int color(double offset) {
		if (!chroma()) return value;
		return Colors.withAlpha(Colors.chroma(offset), Colors.alpha(value));
	}

	public int color() {
		return color(0);
	}

	@Override
	public void reset() {
		super.reset();
		chroma = false;
	}

	@Override
	public JsonElement save() {
		JsonObject o = new JsonObject();
		o.addProperty("argb", String.format("#%08x", value));
		o.addProperty("chroma", chroma);
		return o;
	}

	@Override
	public void load(JsonElement json) {
		if (!json.isJsonObject()) return;
		JsonObject o = json.getAsJsonObject();
		try {
			if (o.has("argb")) value = (int) Long.parseLong(o.get("argb").getAsString().replace("#", ""), 16);
		} catch (NumberFormatException ignored) {
		}
		if (o.has("chroma")) chroma = o.get("chroma").getAsBoolean();
	}
}
