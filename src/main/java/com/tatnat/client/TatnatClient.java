package com.tatnat.client;

import com.tatnat.client.config.ConfigManager;
import com.tatnat.client.event.EventBus;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.ModuleManager;
import com.tatnat.client.platform.Features;
import com.tatnat.client.platform.Game;
import com.tatnat.client.platform.Log;
import com.tatnat.client.ui.clickgui.ClickGuiScreen;
import com.tatnat.client.ui.hud.HudRenderer;
import com.tatnat.client.util.CpsTracker;
import com.tatnat.client.util.KeyCodes;
import com.tatnat.client.util.Keys;

import net.fabricmc.loader.api.FabricLoader;

/**
 * The shared core. Each Minecraft version's entry point calls {@link #init} with its own
 * {@link Game}, {@link Features} and {@link Log}; after that, everything here (menu, HUD, mods,
 * config) is identical on every version.
 */
public final class TatnatClient {
	public static final String ID = "tatnatclient";
	public static Log LOG = Log.STDOUT;
	public static final EventBus EVENTS = new EventBus();
	public static final ConfigManager CONFIG = new ConfigManager();
	public static String VERSION = "dev";

	/** Opens the mod menu. */
	public static final int MENU_KEY = KeyCodes.RIGHT_SHIFT;

	private static Game game;
	private static Features features = new Features() {
	};

	private TatnatClient() {
	}

	public static Game game() {
		return game;
	}

	public static Features features() {
		return features;
	}

	public static void init(Game g, Features f, Log log) {
		game = g;
		if (f != null) features = f;
		if (log != null) LOG = log;
		VERSION = FabricLoader.getInstance().getModContainer(ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
		EVENTS.register(new Hotkeys());
		EVENTS.register(CpsTracker.INSTANCE);
		EVENTS.register(HudRenderer.INSTANCE);
		ModuleManager.get();
		CONFIG.load();
		Runtime.getRuntime().addShutdownHook(new Thread(CONFIG::save, "tatnat-config-save"));
		LOG.info("tatnat client {} loaded with {} mods on Minecraft {}", VERSION, ModuleManager.get().all().size(), g.minecraftVersion());
	}

	/** Right Shift for the menu, plus each mod's own toggle key. */
	public static final class Hotkeys {
		@Subscribe
		public void onKey(Events.Key e) {
			if (!e.inGame || e.action != KeyCodes.PRESS) return;
			if (e.key == MENU_KEY) {
				game.openScreen(new ClickGuiScreen());
				// Consume it, or the game hands this same press to the menu that just opened (which closes it).
				e.cancel();
				return;
			}
			for (Module m : ModuleManager.get().all()) {
				if (m.toggleKey.isBound() && m.toggleKey.get() == e.key) m.toggle();
			}
		}

		@Subscribe
		public void onMouse(Events.MouseButton e) {
			if (!e.inGame || e.action != KeyCodes.PRESS) return;
			int code = Keys.fromMouseButton(e.button);
			for (Module m : ModuleManager.get().all()) {
				if (m.toggleKey.get() == code) m.toggle();
			}
		}

		@Subscribe
		public void onTick(Events.Tick e) {
			CONFIG.tick();
		}
	}
}
