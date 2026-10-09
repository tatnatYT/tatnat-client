package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.platform.UiScreen;
import com.tatnat.client.ui.clickgui.KeybindScreen;
import com.tatnat.client.ui.clickgui.PackScreen;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.util.KeyCodes;
import com.tatnat.client.util.Keys;

/** Keybind Search and Pack Organizer: tools that open their own screen (from their settings or a hotkey). */
public final class ScreenTools {
	private ScreenTools() {
	}

	abstract static class Tool extends Module {
		private final KeybindSetting key = add(new KeybindSetting("Open Key", "Opens it in game", Keys.NONE));

		Tool(String name, String description, Icons.Icon icon) {
			super(name, description, Category.UTILITY, true);
			this.icon = icon;
			add(new ActionSetting("Open", "Open it now", () -> "Open", () -> TatnatClient.game().openScreen(create(new com.tatnat.client.ui.clickgui.ClickGuiScreen()))));
		}

		abstract UiScreen create(UiScreen parent);

		@Subscribe
		public void onKey(Events.Key e) {
			if (e.inGame && e.action == KeyCodes.PRESS && key.isBound() && e.key == key.get()) {
				TatnatClient.game().openScreen(create(null));
				e.cancel();
			}
		}
	}

	/** Every control in one searchable list, rebindable in place. */
	public static class KeybindSearch extends Tool {
		public KeybindSearch() {
			super("Keybind Search", "Search and change every control (1.14+)", Icons.Icon.KEYSEARCH);
		}

		@Override
		UiScreen create(UiScreen parent) {
			return new KeybindScreen(parent);
		}
	}

	/** Turn resource packs on and off and change their order. */
	public static class PackOrganizer extends Tool {
		public PackOrganizer() {
			super("Pack Organizer", "Sort, enable and disable resource packs (1.16+)", Icons.Icon.FOLDER);
		}

		@Override
		UiScreen create(UiScreen parent) {
			return new PackScreen(parent);
		}
	}
}
