package com.tatnat.client;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tatnat.client.config.ConfigManager;
import com.tatnat.client.event.EventBus;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.ModuleManager;
import com.tatnat.client.ui.clickgui.ClickGuiScreen;
import com.tatnat.client.ui.hud.HudRenderer;
import com.tatnat.client.util.CpsTracker;
import com.tatnat.client.util.DevTest;
import com.tatnat.client.util.Keys;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Entry point. Wires the event bus, loads modules and their config, and handles the global
 * hotkeys (Right Shift for the menu, per-module toggle keys).
 */
public final class TatnatClient implements ClientModInitializer {
	public static final String ID = "tatnatclient";
	public static final Logger LOG = LoggerFactory.getLogger("tatnat client");
	public static final EventBus EVENTS = new EventBus();
	public static final ConfigManager CONFIG = new ConfigManager();
	public static String VERSION = "dev";

	/** Opens the ClickGUI. */
	public static final int MENU_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

	@Override
	public void onInitializeClient() {
		VERSION = FabricLoader.getInstance().getModContainer(ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
		EVENTS.register(this);
		EVENTS.register(CpsTracker.INSTANCE);
		EVENTS.register(HudRenderer.INSTANCE);
		ModuleManager.get();
		CONFIG.load();
		Runtime.getRuntime().addShutdownHook(new Thread(CONFIG::save, "tatnat-config-save"));
		DevTest.init();
		LOG.info("tatnat client {} loaded with {} mods", VERSION, ModuleManager.get().all().size());
	}

	@Subscribe
	public void onKey(Events.Key e) {
		if (!e.inGame || e.action != GLFW.GLFW_PRESS) return;
		Minecraft mc = Minecraft.getInstance();
		if (e.key == MENU_KEY) {
			mc.setScreen(new ClickGuiScreen());
			return;
		}
		for (Module m : ModuleManager.get().all()) {
			if (m.toggleKey.isBound() && m.toggleKey.get() == e.key) m.toggle();
		}
	}

	@Subscribe
	public void onMouse(Events.MouseButton e) {
		if (!e.inGame || e.action != GLFW.GLFW_PRESS) return;
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
