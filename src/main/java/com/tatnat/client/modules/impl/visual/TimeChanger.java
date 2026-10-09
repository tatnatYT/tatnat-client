package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Sets the time of day and the weather on your screen only ({@code ClientLevelMixins}). The server
 * and other players are unaffected; mobs still spawn by the real time.
 */
public class TimeChanger extends Module {
	public static TimeChanger INSTANCE;

	public final SliderSetting time = add(new SliderSetting("Time", "0 = sunrise, 6000 = noon, 13000 = night, 18000 = midnight", 6000, 0, 24000, 250, ""));
	public final ModeSetting weather = add(new ModeSetting("Weather", "Weather you see", "Clear", "Server", "Clear", "Rain", "Thunder"));

	public TimeChanger() {
		super("Time Changer", "Pick your own time of day and weather", Category.VISUAL, false);
		icon = Icons.Icon.DAYNIGHT;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}
}
