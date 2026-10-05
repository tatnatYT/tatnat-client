package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.theme.Theme;

/**
 * Replaces the thin black block outline with a thick coloured wireframe and tints the face you
 * are looking at. The drawing itself happens in {@code LevelRendererMixin}, which asks this
 * module for colours each frame.
 */
public class BlockOverlay extends Module {
	public static BlockOverlay INSTANCE;

	public final ColorSetting outlineColor = add(new ColorSetting("Outline Color", "Colour of the wireframe", Theme.ACCENT, true));
	public final SliderSetting thickness = add(new SliderSetting("Thickness", "Outline width in pixels", 2.5, 1.0, 5.0, 0.5, "px"));
	public final BooleanSetting fillFace = add(new BooleanSetting("Fill Face", "Tint the side of the block you're looking at", true));
	public final ColorSetting fillColor = add(new ColorSetting("Fill Color", "Colour of the tinted face", Theme.ACCENT, true));
	public final SliderSetting fillOpacity = add(new SliderSetting("Fill Opacity", "How see-through the tinted face is", 25, 0, 100, 1, "%"));

	public BlockOverlay() {
		super("Block Overlay", "Thick coloured outline around the block you're looking at", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.CUBE;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	/** Opaque outline colour (chroma cycles over time). */
	public int outline() {
		return 0xFF000000 | (outlineColor.color() & 0xFFFFFF);
	}

	public int fill() {
		int a = Math.round(fillOpacity.floatValue() / 100f * 255f);
		return (a << 24) | (fillColor.color(0.2) & 0xFFFFFF);
	}
}
