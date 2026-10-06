package com.tatnat.client.modules;

import com.tatnat.client.modules.settings.BooleanSetting;

/** Client-wide options shown on the Settings page. Not a mod; saved under "options" in the config. */
public final class ClientOptions extends Module {
	public static final ClientOptions INSTANCE = new ClientOptions();

	public final BooleanSetting titleButton = add(new BooleanSetting("Title Screen Button",
			"A \"tatnat client\" button on the main menu that opens the mod menu", true));

	private ClientOptions() {
		super("Options", "Client options", Category.UTILITY, true);
	}

	/** True when the title screen should show the mod menu button. */
	public static boolean titleButton() {
		return INSTANCE.titleButton.on();
	}
}
