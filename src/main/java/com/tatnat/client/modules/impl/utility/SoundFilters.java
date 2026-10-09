package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.SliderSetting;

/**
 * Quieter game in menus and muffled sound under water, by turning the master volume down for a
 * moment and back up after. Your own volume setting is restored when it ends or the mod is off.
 */
public class SoundFilters extends Module {
	private final BooleanSetting muffle = add(new BooleanSetting("Muffle Underwater", "Quieter while your head is under water", true));
	private final BooleanSetting menus = add(new BooleanSetting("Lower GUI Volume", "Quieter while a menu or inventory is open", false));
	private final SliderSetting amount = add(new SliderSetting("Volume", "How loud it gets while filtered", 40, 0, 100, 5, "%"));

	/** The player's real master volume while we're lowering it, else -1. */
	private float saved = -1;

	public SoundFilters() {
		super("Sound Filters", "Muffles sound under water and in menus", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.SPEAKER;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		boolean filter = game().inWorld() && (muffle.on() && game().underwater() || menus.on() && game().screenOpen() && !game().ourScreenOpen());
		if (filter && saved < 0) {
			saved = game().masterVolume();
			game().setMasterVolume(saved * amount.floatValue() / 100f);
		} else if (!filter && saved >= 0) {
			restore();
		}
	}

	private void restore() {
		game().setMasterVolume(saved);
		saved = -1;
	}

	@Override
	protected void onDisable() {
		if (saved >= 0) restore();
	}
}
