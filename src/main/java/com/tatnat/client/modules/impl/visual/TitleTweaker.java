package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;

/** Hides the big titles and subtitles servers put in the middle of the screen (1.14+). */
public class TitleTweaker extends Module {
	private static TitleTweaker instance;

	private final BooleanSetting titles = add(new BooleanSetting("Hide Titles", "No big titles in the middle of the screen", true));
	private final BooleanSetting subtitles = add(new BooleanSetting("Hide Subtitles", "No smaller line under the title", true));

	public TitleTweaker() {
		super("Title Tweaker", "Hides the titles servers show in the middle of the screen", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.TOOLTIP;
		instance = this;
	}

	public static boolean hideTitles() {
		return instance != null && instance.isEnabled() && instance.titles.on();
	}

	public static boolean hideSubtitles() {
		return instance != null && instance.isEnabled() && instance.subtitles.on();
	}
}
