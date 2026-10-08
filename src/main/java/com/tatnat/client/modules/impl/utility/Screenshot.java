package com.tatnat.client.modules.impl.utility;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ActionSetting;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.platform.Gfx;

/**
 * Better screenshots: a notice in the corner when one is saved, and (on Windows) the picture is
 * copied to the clipboard so you can paste it straight into Discord.
 */
public class Screenshot extends Module {
	private final BooleanSetting copy = add(new BooleanSetting("Auto Copy to Clipboard", "Copy each new screenshot (Windows)", true));
	private final BooleanSetting notice = add(new BooleanSetting("Show Notice", "Show a notice when a screenshot is saved", true));

	private long lastCheck, newest = -1, noticeAt;
	private String noticeText = "";

	public Screenshot() {
		super("Screenshot", "Copies new screenshots to the clipboard", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.CAMERA;
		add(new ActionSetting("Open folder", "Show your screenshots", () -> "Open", () -> TatnatClient.game().openPath(dir())));
	}

	private static Path dir() {
		// The game folder is the config folder's parent.
		return TatnatClient.game().configDir().toAbsolutePath().getParent().resolve("screenshots");
	}

	@Override
	protected void onEnable() {
		newest = latestTime();
	}

	private static long latestTime() {
		File[] files = dir().toFile().listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png"));
		long t = 0;
		if (files != null) for (File f : files) t = Math.max(t, f.lastModified());
		return t;
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		long now = System.currentTimeMillis();
		if (now - lastCheck < 1000) return;
		lastCheck = now;
		if (!Files.isDirectory(dir())) return;
		try (Stream<Path> s = Files.list(dir())) {
			Path latest = s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
					.max((a, b) -> Long.compare(a.toFile().lastModified(), b.toFile().lastModified())).orElse(null);
			if (latest == null || latest.toFile().lastModified() <= newest) return;
			newest = latest.toFile().lastModified();
			boolean copied = copy.on() && copyToClipboard(latest);
			noticeText = "Screenshot saved" + (copied ? " and copied" : "") + ": " + latest.getFileName();
			noticeAt = now;
		} catch (Exception ex) {
			TatnatClient.LOG.warn("[screenshot] {}", ex.toString());
		}
	}

	/** Windows only: PowerShell puts the image on the clipboard (the game itself runs headless AWT). */
	private static boolean copyToClipboard(Path png) {
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return false;
		String path = png.toAbsolutePath().toString().replace("'", "''");
		String script = "Add-Type -AssemblyName System.Windows.Forms,System.Drawing; "
				+ "[System.Windows.Forms.Clipboard]::SetImage([System.Drawing.Image]::FromFile('" + path + "'))";
		try {
			new ProcessBuilder("powershell", "-NoProfile", "-STA", "-WindowStyle", "Hidden", "-Command", script).start();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Subscribe
	public void onRender(Events.Render2D e) {
		if (!notice.on() || System.currentTimeMillis() - noticeAt > 3000 || noticeText.isEmpty()) return;
		Gfx g = e.gfx;
		int w = g.mcTextWidth(noticeText, false);
		int x = game().guiWidth() - w - 10, y = game().guiHeight() - 30;
		g.rect(x - 5, y - 4, x + w + 5, y + 12, 0xB0000000);
		g.rect(x - 5, y - 4, x - 3, y + 12, 0xFF4EB1FF);
		g.mcText(noticeText, x, y, 0xFFFFFFFF, false, false);
	}
}
