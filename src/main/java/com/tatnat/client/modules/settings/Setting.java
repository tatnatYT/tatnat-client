package com.tatnat.client.modules.settings;

import java.util.function.BooleanSupplier;

import com.google.gson.JsonElement;

/**
 * One configurable value on a module. Every subclass knows how to save itself to JSON and the
 * ClickGUI picks a matching component for it, so adding a new setting type only needs a new
 * subclass plus one component.
 */
public abstract class Setting<T> {
	public final String name;
	public final String description;
	protected T value;
	protected final T defaultValue;
	private BooleanSupplier visibility = () -> true;

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public void reset() {
		set(defaultValue);
	}

	/** Hides this setting in the menu unless {@code condition} holds (e.g. "Chroma speed" only when chroma is on). */
	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
		this.visibility = condition;
		return (S) this;
	}

	public boolean isVisible() {
		return visibility.getAsBoolean();
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement json);
}
