package com.tatnat.client.ui.clickgui;

import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.UiScreen;

/**
 * Pack Organizer: switch resource packs on and off and change their order without the vanilla
 * screen. Changes are collected and applied in one reload with Apply.
 */
public class PackScreen extends ListScreen {
	/** Packs that are on, bottom to top (the last one wins), and the rest. */
	private final List<String> on = new ArrayList<>(), off = new ArrayList<>();
	private final java.util.Map<String, String> titles = new java.util.HashMap<>();
	private boolean changed;

	public PackScreen(UiScreen parent) {
		super(parent);
		load();
	}

	private void load() {
		on.clear();
		off.clear();
		for (String[] p : TatnatClient.game().resourcePackList()) {
			(p[1].equals("1") ? on : off).add(p[0]);
			if (p.length > 2 && !p[2].isEmpty()) titles.put(p[0], p[2]);
		}
		changed = false;
	}

	@Override
	protected String title() {
		return "Pack Organizer";
	}

	@Override
	protected String hint() {
		return changed ? "Changes are waiting: press Apply on the top row to reload the packs" : "Top of the list wins. Turn packs on or off and move them up or down";
	}

	private String pretty(String id) {
		String s = titles.containsKey(id) ? titles.get(id) : id.startsWith("file/") ? id.substring(5) : id;
		return s.replaceAll("§.", "");
	}

	@Override
	protected List<Row> rows() {
		List<Row> rows = new ArrayList<>();
		if (on.isEmpty() && off.isEmpty()) {
			rows.add(new Row("Not available on this Minecraft version", "Pack Organizer needs 1.16 or newer"));
			return rows;
		}
		if (changed) rows.add(new Row("Apply changes", "Reloads the packs (takes a few seconds)").highlight(true).button("Apply", () -> {
			TatnatClient.game().applyResourcePacks(new ArrayList<>(on));
			changed = false;
		}).button("Undo", this::load));
		// Shown top (wins) to bottom.
		for (int i = on.size() - 1; i >= 0; i--) {
			final int idx = i;
			String id = on.get(i);
			Row r = new Row(pretty(id), "On  ·  position " + (on.size() - i)).button("Turn off", () -> {
				off.add(0, on.remove(idx));
				changed = true;
			});
			if (i < on.size() - 1) r.button("Up", () -> {
				on.add(idx + 1, on.remove(idx));
				changed = true;
			});
			if (i > 0) r.button("Down", () -> {
				on.add(idx - 1, on.remove(idx));
				changed = true;
			});
			rows.add(r);
		}
		for (String id : new ArrayList<>(off)) {
			rows.add(new Row(pretty(id), "Off").button("Turn on", () -> {
				off.remove(id);
				on.add(id);
				changed = true;
			}));
		}
		return rows;
	}
}
