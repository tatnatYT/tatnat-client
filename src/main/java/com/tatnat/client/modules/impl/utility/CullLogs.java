package com.tatnat.client.modules.impl.utility;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.TextSetting;

/**
 * Hides chat spam: common server adverts and lobby messages, plus your own words or regex. Each
 * version's chat hook asks {@link #blocked(String)} before a message is added.
 */
public class CullLogs extends Module {
	public static CullLogs INSTANCE;

	/** Typical lobby / server spam (Hypixel and similar networks). */
	private static final String[] PRESET = {
			"joined the lobby!", "is now playing", "mystery box", "you found a", "rank upgrade", "check out our store",
			"visit our store", "has found a", "[watchdog announcement]", "watchdog has banned", "staff have banned",
			"blacklisted modifications", "you are afk", "sending you to", "+50 coins", "+25 coins", "+10 coins",
	};

	private final BooleanSetting presets = add(new BooleanSetting("Filter Server Spam", "Hide common lobby and advert messages", true));
	private final TextSetting words = add(new TextSetting("Hide Words", "Hide messages containing any of these, separated by commas", "", 256));
	private final TextSetting regex = add(new TextSetting("Custom Regex", "Hide messages matching this regular expression", "", 256));

	private String compiledFrom;
	private Pattern pattern;
	private int removed;

	public CullLogs() {
		super("Cull Logs", "Hides spam messages from chat", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.CHAT;
		INSTANCE = this;
	}

	/** True when a chat message should be hidden. */
	public static boolean blocked(String text) {
		CullLogs m = INSTANCE;
		if (m == null || !m.isEnabled() || text == null) return false;
		String t = text.toLowerCase(Locale.ROOT);
		boolean hide = false;
		if (m.presets.on()) for (String p : PRESET) if (t.contains(p)) hide = true;
		if (!hide) for (String w : m.words.get().toLowerCase(Locale.ROOT).split(",")) if (!w.trim().isEmpty() && t.contains(w.trim())) hide = true;
		if (!hide && m.pattern() != null) hide = m.pattern.matcher(text).find();
		if (hide) m.removed++;
		return hide;
	}

	private Pattern pattern() {
		String r = regex.get().trim();
		if (!r.equals(compiledFrom)) {
			compiledFrom = r;
			try {
				pattern = r.isEmpty() ? null : Pattern.compile(r);
			} catch (PatternSyntaxException e) {
				pattern = null;
			}
		}
		return pattern;
	}

}
