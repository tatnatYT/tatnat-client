package com.tatnat.client.modules;

import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.util.KeyCodes;

/** Client-wide options shown on the Settings page. Not a mod; saved under "options" in the config. */
public final class ClientOptions extends Module {
	public static final ClientOptions INSTANCE = new ClientOptions();

	public final BooleanSetting titleButton = add(new BooleanSetting("Title Screen Button",
			"A \"Eclipse Client\" button on the main menu that opens the mod menu", true));

	public final KeybindSetting menuKey = add(new KeybindSetting("Open the menu",
			"The key that opens this menu (click to change)", KeyCodes.RIGHT_SHIFT));

	private ClientOptions() {
		super("Options", "Client options", Category.UTILITY, true);
	}

	/** The key that opens the mod menu (Right Shift unless the player changed it). */
	public static int menuKey() {
		return INSTANCE.menuKey.isBound() ? INSTANCE.menuKey.get() : KeyCodes.RIGHT_SHIFT;
	}

	/** True when the title screen should show the mod menu button. */
	public static boolean titleButton() {
		return INSTANCE.titleButton.on();
	}
}
