package com.tatnat.client.modules.impl.utility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.modules.settings.SliderSetting;

/**
 * Keeps copies of your tatnat client settings (mods, HUD layout, waypoints) in
 * {@code config/tatnat-backups}, every few minutes and whenever you ask, so a bad change can be undone
 * by copying a backup back over {@code config/tatnat-client.json}.
 */
public class Backups extends Module {
	private final SliderSetting interval = add(new SliderSetting("Interval", "Minutes between backups", 30, 5, 240, 5, " min"));
	private final SliderSetting keep = add(new SliderSetting("Max Backups", "Older backups are deleted", 10, 1, 50, 1, ""));

	private long last;
	private String lastResult = "";

	public Backups() {
		super("Backups", "Automatic backups of your tatnat client settings", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.BOX;
		add(new ActionSetting("Back up now", "Make a backup right away", () -> lastResult.isEmpty() ? "Back up" : lastResult, this::backup));
		add(new ActionSetting("Open folder", "Show the backups", () -> "Open", () -> TatnatClient.game().openPath(dir())));
	}

	private static Path dir() {
		return TatnatClient.game().configDir().resolve("tatnat-backups");
	}

	@Override
	protected void onEnable() {
		last = System.currentTimeMillis();
		backup();
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (System.currentTimeMillis() - last >= interval.get() * 60_000L) {
			last = System.currentTimeMillis();
			backup();
		}
	}

	private void backup() {
		try {
			TatnatClient.CONFIG.save();
			Path src = TatnatClient.game().configDir().resolve("tatnat-client.json");
			if (!Files.exists(src)) return;
			Files.createDirectories(dir());
			String name = "tatnat-client-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".json";
			Files.copy(src, dir().resolve(name), StandardCopyOption.REPLACE_EXISTING);
			List<Path> all = new ArrayList<>();
			try (Stream<Path> s = Files.list(dir())) {
				s.filter(p -> p.getFileName().toString().startsWith("tatnat-client-")).forEach(all::add);
			}
			Collections.sort(all); // names sort by date
			for (int i = 0; i < all.size() - keep.intValue(); i++) Files.deleteIfExists(all.get(i));
			lastResult = "Saved " + new SimpleDateFormat("HH:mm").format(new Date());
		} catch (Exception ex) {
			lastResult = "Failed";
			TatnatClient.LOG.warn("[backups] {}", ex.toString());
		}
	}
}
