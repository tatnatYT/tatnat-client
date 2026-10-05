package com.tatnat.client.modules;

import com.google.gson.JsonObject;
import com.tatnat.client.modules.settings.SliderSetting;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A module that draws an element on the HUD which can be dragged around in the HUD editor.
 *
 * Positions are stored as fractions of the free space (0 = left/top edge, 1 = right/bottom
 * edge, 0.5 = centred), so a layout made at one resolution lands in the same place at any
 * other resolution or GUI scale.
 */
public abstract class HudModule extends Module {
	public final SliderSetting scale;

	private double relX, relY;
	private final double defaultX, defaultY;
	/** Unscaled size from the last draw, in GUI units. */
	private int width = 40, height = 12;

	protected HudModule(String name, String description, boolean enabledByDefault, double defaultX, double defaultY) {
		this(name, description, Category.HUD, enabledByDefault, defaultX, defaultY);
	}

	/** For modules that live in another category but still draw something (e.g. Toggle Sprint's indicator). */
	protected HudModule(String name, String description, Category category, boolean enabledByDefault, double defaultX, double defaultY) {
		super(name, description, category, enabledByDefault);
		this.relX = this.defaultX = defaultX;
		this.relY = this.defaultY = defaultY;
		this.scale = add(new SliderSetting("Scale", "Size of this element", 1.0, 0.5, 3.0, 0.05, "x"));
	}

	/**
	 * Draws the element with its top-left at (0, 0) in unscaled GUI units and returns its size
	 * packed as {@code width << 16 | height}. {@code preview} is true in the HUD editor, where
	 * elements should show sample content even when there is nothing real to show.
	 */
	protected abstract long draw(GuiGraphics g, boolean preview);

	/** Whether there is anything to show right now (e.g. Potion Status with no effects). */
	public boolean hasContent() {
		return true;
	}

	/** Draws at the element's on-screen position with its scale applied. */
	public final void render(GuiGraphics g, int screenW, int screenH, boolean preview) {
		float s = scale.floatValue();
		g.pose().pushMatrix();
		g.pose().translate(screenX(screenW), screenY(screenH));
		g.pose().scale(s, s);
		long size = draw(g, preview);
		g.pose().popMatrix();
		width = Math.max(1, (int) (size >>> 16));
		height = Math.max(1, (int) (size & 0xFFFF));
	}

	protected static long size(int w, int h) {
		return ((long) w << 16) | (h & 0xFFFF);
	}

	public float scaledWidth() {
		return width * scale.floatValue();
	}

	public float scaledHeight() {
		return height * scale.floatValue();
	}

	public float screenX(int screenW) {
		return (float) (relX * Math.max(0, screenW - scaledWidth()));
	}

	public float screenY(int screenH) {
		return (float) (relY * Math.max(0, screenH - scaledHeight()));
	}

	/** Moves the element so its top-left is at (x, y), clamped to the screen. */
	public void setScreenPos(float x, float y, int screenW, int screenH) {
		float freeW = Math.max(1, screenW - scaledWidth()), freeH = Math.max(1, screenH - scaledHeight());
		relX = Math.max(0, Math.min(1, x / freeW));
		relY = Math.max(0, Math.min(1, y / freeH));
	}

	public void resetPosition() {
		relX = defaultX;
		relY = defaultY;
	}

	@Override
	public void resetToDefaults() {
		super.resetToDefaults();
		resetPosition();
	}

	@Override
	public JsonObject save() {
		JsonObject o = super.save();
		o.addProperty("x", relX);
		o.addProperty("y", relY);
		return o;
	}

	@Override
	public void load(JsonObject o) {
		super.load(o);
		if (o.has("x")) relX = Math.max(0, Math.min(1, o.get("x").getAsDouble()));
		if (o.has("y")) relY = Math.max(0, Math.min(1, o.get("y").getAsDouble()));
	}
}
