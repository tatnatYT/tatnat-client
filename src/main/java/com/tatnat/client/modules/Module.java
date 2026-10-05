package com.tatnat.client.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.modules.settings.Setting;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.util.Keys;

import net.minecraft.client.Minecraft;

/**
 * Base class for every mod in the client.
 *
 * A module is only subscribed to the event bus while it is enabled, so disabled modules cost
 * nothing per frame. Subclasses declare their settings with {@link #add} in the constructor; the
 * menu and the config file pick them up automatically.
 */
public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	public final String name;
	public final String description;
	public final Category category;
	/** Toggles the module from anywhere in game. Unbound by default. */
	public KeybindSetting toggleKey = new KeybindSetting("Toggle Key", "Press this key in game to turn the mod on or off", Keys.NONE);

	/** Gives this mod a default toggle key (call from the constructor). */
	protected final void defaultToggleKey(int key) {
		toggleKey = new KeybindSetting("Toggle Key", "Press this key in game to turn the mod on or off", key);
	}

	/** Line-art icon on the mod's card in the menu. */
	protected Icons.Icon icon = Icons.Icon.GRID;
	/** Starred in the menu (shown first and under the heart filter). */
	private boolean favorite;

	private final List<Setting<?>> settings = new ArrayList<>();
	private final boolean enabledByDefault;
	private boolean enabled;

	public Icons.Icon icon() {
		return icon;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
		TatnatClient.CONFIG.markDirty();
	}

	protected Module(String name, String description, Category category, boolean enabledByDefault) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.enabledByDefault = enabledByDefault;
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	public List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		if (enabled) {
			TatnatClient.EVENTS.register(this);
			onEnable();
		} else {
			TatnatClient.EVENTS.unregister(this);
			onDisable();
		}
		TatnatClient.CONFIG.markDirty();
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Restores every setting (and the on/off state) to its default. */
	public void resetToDefaults() {
		for (Setting<?> s : settings) s.reset();
		toggleKey.reset();
		setEnabled(enabledByDefault);
		TatnatClient.CONFIG.markDirty();
	}

	public boolean enabledByDefault() {
		return enabledByDefault;
	}

	/** Stable key used in the config file. */
	public String id() {
		return name.toLowerCase().replaceAll("[^a-z0-9]+", "_");
	}

	public JsonObject save() {
		JsonObject o = new JsonObject();
		o.addProperty("enabled", enabled);
		o.addProperty("favorite", favorite);
		o.add("toggleKey", toggleKey.save());
		JsonObject s = new JsonObject();
		for (Setting<?> setting : settings) s.add(setting.name, setting.save());
		o.add("settings", s);
		return o;
	}

	public void load(JsonObject o) {
		if (o.has("toggleKey")) toggleKey.load(o.get("toggleKey"));
		favorite = o.has("favorite") && o.get("favorite").getAsBoolean();
		if (o.has("settings") && o.get("settings").isJsonObject()) {
			JsonObject s = o.getAsJsonObject("settings");
			for (Setting<?> setting : settings) {
				JsonElement e = s.get(setting.name);
				if (e != null) {
					try {
						setting.load(e);
					} catch (RuntimeException ex) {
						TatnatClient.LOG.warn("Ignoring bad value for {} / {}", name, setting.name);
					}
				}
			}
		}
		setEnabled(o.has("enabled") ? o.get("enabled").getAsBoolean() : enabledByDefault);
	}
}
