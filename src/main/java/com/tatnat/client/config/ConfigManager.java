package com.tatnat.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.ModuleManager;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Saves every module's state to {@code config/tatnat-client.json}.
 *
 * Changes only mark the config dirty; the file is written at most once a second from the tick
 * loop (and on shutdown), so dragging a slider doesn't hammer the disk. Writes go to a temp file
 * first and are then moved into place, so a crash mid-write can never leave a half-written config.
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final Path file = FabricLoader.getInstance().getConfigDir().resolve("tatnat-client.json");
	private boolean dirty;
	private boolean loading;
	private long lastSave;

	public void markDirty() {
		if (!loading) dirty = true;
	}

	public void load() {
		loading = true;
		try {
			JsonObject root = new JsonObject();
			if (Files.exists(file)) {
				try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					root = JsonParser.parseReader(r).getAsJsonObject();
				} catch (Exception e) {
					TatnatClient.LOG.error("Config was unreadable, starting from defaults (old file kept as .broken)", e);
					try {
						Files.copy(file, file.resolveSibling("tatnat-client.json.broken"), StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException ignored) {
					}
				}
			}
			JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
			int version = root.has("version") ? root.get("version").getAsInt() : 0;
			if (version < 2) {
				// v2: brackets became opt-in, so drop the old saved "on" values.
				for (String key : mods.keySet()) {
					if (mods.get(key).isJsonObject() && mods.getAsJsonObject(key).has("settings")) mods.getAsJsonObject(key).getAsJsonObject("settings").remove("Brackets");
				}
			}
			for (Module m : ModuleManager.get().all()) {
				JsonObject o = mods.has(m.id()) && mods.get(m.id()).isJsonObject() ? mods.getAsJsonObject(m.id()) : new JsonObject();
				m.load(o);
			}
		} finally {
			loading = false;
		}
		if (!Files.exists(file)) save();
	}

	public void save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 2);
		JsonObject mods = new JsonObject();
		for (Module m : ModuleManager.get().all()) mods.add(m.id(), m.save());
		root.add("modules", mods);
		try {
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling("tatnat-client.json.tmp");
			try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, w);
			}
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			dirty = false;
		} catch (IOException e) {
			TatnatClient.LOG.error("Could not save config", e);
		}
		lastSave = System.currentTimeMillis();
	}

	/** Called every tick: writes pending changes, at most once per second. */
	public void tick() {
		if (dirty && System.currentTimeMillis() - lastSave > 1000) save();
	}
}
