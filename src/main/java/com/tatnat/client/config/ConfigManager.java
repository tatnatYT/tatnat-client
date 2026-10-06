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


/**
 * Saves every module's state to {@code config/tatnat-client.json}.
 *
 * Changes only mark the config dirty; the file is written at most once a second from the tick
 * loop (and on shutdown), so dragging a slider doesn't hammer the disk. Writes go to a temp file
 * first and are then moved into place, so a crash mid-write can never leave a half-written config.
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private Path file;

	/** Resolved on first use: the loader's config folder comes from the platform. */
	private Path file() {
		if (file == null) file = TatnatClient.game().configDir().resolve("tatnat-client.json");
		return file;
	}
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
			if (Files.exists(file())) {
				try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
					// Instance parse(): the static parseReader() is missing from the older Gson in 1.8.9-1.12.2.
					root = new JsonParser().parse(r).getAsJsonObject();
				} catch (Exception e) {
					TatnatClient.LOG.error("Config was unreadable, starting from defaults (old file kept as .broken)", e);
					try {
						Files.copy(file(), file().resolveSibling("tatnat-client.json.broken"), StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException ignored) {
					}
				}
			}
			JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
			int version = root.has("version") ? root.get("version").getAsInt() : 0;
			if (version < 2) {
				// v2: brackets became opt-in, so drop the old saved "on" values.
				for (java.util.Map.Entry<String, com.google.gson.JsonElement> en : mods.entrySet()) {
					if (en.getValue().isJsonObject() && en.getValue().getAsJsonObject().has("settings")) en.getValue().getAsJsonObject().getAsJsonObject("settings").remove("Brackets");
				}
			}
			if (root.has("options") && root.get("options").isJsonObject()) com.tatnat.client.modules.ClientOptions.INSTANCE.load(root.getAsJsonObject("options"));
			if (root.has("performance") && root.get("performance").isJsonObject()) com.tatnat.client.modules.Performance.INSTANCE.load(root.getAsJsonObject("performance"));
			for (Module m : ModuleManager.get().all()) {
				JsonObject o = mods.has(m.id()) && mods.get(m.id()).isJsonObject() ? mods.getAsJsonObject(m.id()) : new JsonObject();
				m.load(o);
			}
		} finally {
			loading = false;
		}
		if (!Files.exists(file())) save();
	}

	public void save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 2);
		JsonObject mods = new JsonObject();
		for (Module m : ModuleManager.get().all()) mods.add(m.id(), m.save());
		root.add("modules", mods);
		root.add("performance", com.tatnat.client.modules.Performance.INSTANCE.save());
		root.add("options", com.tatnat.client.modules.ClientOptions.INSTANCE.save());
		try {
			Files.createDirectories(file().getParent());
			Path tmp = file().resolveSibling("tatnat-client.json.tmp");
			try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, w);
			}
			Files.move(tmp, file(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
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
