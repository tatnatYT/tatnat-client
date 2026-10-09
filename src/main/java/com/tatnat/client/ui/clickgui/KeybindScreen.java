package com.tatnat.client.ui.clickgui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.UiScreen;
import com.tatnat.client.util.KeyCodes;

/**
 * Keybind Search: every Minecraft control (and those of other mods) in one searchable list.
 * Click Change and press a key to rebind; Esc while choosing unbinds. Clashing keys are marked.
 */
public class KeybindScreen extends ListScreen {
	/** The control waiting for a new key, or null. */
	private String waiting;

	public KeybindScreen(UiScreen parent) {
		super(parent);
	}

	@Override
	protected String title() {
		return "Keybind Search";
	}

	@Override
	protected String hint() {
		return waiting != null ? "Press a key for \"" + nameOf(waiting) + "\" (Esc = unbind)" : "Search any control by name, category or key";
	}

	private String nameOf(String id) {
		for (String[] k : TatnatClient.game().keyMappings()) if (k[0].equals(id)) return k[1];
		return id;
	}

	@Override
	protected List<Row> rows() {
		List<String[]> keys = TatnatClient.game().keyMappings();
		Map<String, Integer> uses = new HashMap<>();
		for (String[] k : keys) uses.merge(k[3], 1, Integer::sum);
		List<Row> out = new ArrayList<>();
		if (keys.isEmpty()) {
			out.add(new Row("Not available on this Minecraft version", "Keybind Search needs 1.14 or newer"));
			return out;
		}
		for (String[] k : keys) {
			String id = k[0];
			boolean clash = !k[3].isEmpty() && !"Not Bound".equalsIgnoreCase(k[3]) && uses.getOrDefault(k[3], 0) > 1;
			String sub = k[2] + "  ·  " + k[3] + (clash ? "  ·  also used by another control" : "");
			out.add(new Row(k[1], sub).highlight(id.equals(waiting) || clash)
					.button(id.equals(waiting) ? "Press a key…" : "Change", () -> waiting = id));
		}
		return out;
	}

	@Override
	protected boolean onKey(int key) {
		if (waiting == null) return false;
		TatnatClient.game().rebindKey(waiting, key == KeyCodes.ESCAPE ? -1 : key);
		waiting = null;
		return true;
	}

	@Override
	public boolean charTyped(String chars) {
		// While choosing a key, typing shouldn't also search.
		return waiting != null || super.charTyped(chars);
	}
}
