package com.tatnat.client.modules.impl.utility;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ModeSetting;

/**
 * Tier Tagger, like uku's mod: shows each player's PvP tier in front of their name, on their
 * name tag and in the tab list ("HT3 | Steve"). Tiers come from PvPTiers (formerly MCTiers) or
 * SubTiers; pick a gamemode or show the player's best one. Looked up once per player in the
 * background and cached.
 */
public class TierTagger extends Module {
	private static TierTagger instance;

	private final ModeSetting list = add(new ModeSetting("Tier List", "Where the tiers come from", "PvPTiers / MCTiers", "PvPTiers / MCTiers", "SubTiers"));
	private final ModeSetting mode = add(new ModeSetting("Gamemode", "Which tier to show (Highest = the player's best)", "Highest",
			"Highest", "Sword", "Crystal", "Axe", "Pot", "Netherite Pot", "SMP", "UHC", "Mace", "Elytra",
			"Diamond SMP", "Trident", "Bed", "Minecart", "Creeper", "Bow", "Debuff", "Speed", "Manhunt", "OG Vanilla", "Diamond Crystal"));
	private final BooleanSetting fallback = add(new BooleanSetting("Fall Back to Highest", "No tier in that gamemode: show their best one instead", true));
	private final BooleanSetting tab = add(new BooleanSetting("Show in Tab List", "Also put the tier in front of names in the tab list", true));
	private final BooleanSetting showMode = add(new BooleanSetting("Show Gamemode", "Add the gamemode after the tier, e.g. HT3 Sword", false));

	/** A tier to draw: its text ("HT3", "RLT2"), its colour and the closest § colour code for old versions. */
	public static final class Tag {
		public final String text;
		public final int rgb;
		public final char code;

		Tag(String text, int rgb, char code) {
			this.text = text;
			this.rgb = rgb;
			this.code = code;
		}

		/** "§6HT3 §7| " for versions that only have § colours. */
		public String legacy() {
			return "§" + code + text + " §7| §r";
		}
	}

	/** Per list + player: their rankings (mode id -> {tier, pos, retired, peakTier, peakPos}), or an empty map. */
	private static final Map<String, Map<String, int[]>> CACHE = new ConcurrentHashMap<>();
	private static final Map<String, Long> FETCHED = new ConcurrentHashMap<>();
	private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "tatnat-tiers");
		t.setDaemon(true);
		return t;
	});
	private static final long REFRESH_MS = 15 * 60 * 1000L;

	private static final String[] MODE_IDS = {null, "sword", "crystal", "axe", "pot", "neth_pot", "smp", "uhc", "mace", "elytra",
			"dia_smp", "trident", "bed", "minecart", "creeper", "bow", "debuff", "speed", "manhunt", "og_vanilla", "dia_crystal"};

	public TierTagger() {
		super("Tier Tagger", "Shows players' PvP tiers (PvPTiers, MCTiers, SubTiers) next to their name", Category.UTILITY, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.STAR;
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Item tooltips no longer get a tier line (Tier Tagger is about players now). */
	public static String line(int ordinal, String name) {
		return null;
	}

	/** The tag for a player's name tag, or null (unknown player, not ranked, still loading or mod off). */
	public static Tag nameTag(UUID uuid, String name) {
		return active() ? lookup(uuid, name) : null;
	}

	/** The tag for a player's tab list entry, or null. */
	public static Tag tabTag(UUID uuid, String name) {
		return active() && instance.tab.on() ? lookup(uuid, name) : null;
	}

	private static Tag lookup(UUID uuid, String name) {
		if (uuid == null) return null;
		// Real (Mojang) accounts are looked up by their UUID. Offline-mode / cracked servers give
		// everyone a made-up (version 3) UUID, so there the player's name is looked up instead.
		boolean online = uuid.version() == 4;
		if (!online && (name == null || !name.matches("[A-Za-z0-9_]{1,16}"))) return null;
		TierTagger m = instance;
		boolean sub = m.list.is("SubTiers");
		String key = (sub ? "s:" : "p:") + (online ? uuid.toString() : "name:" + name.toLowerCase(Locale.ROOT));
		Map<String, int[]> rankings = CACHE.get(key);
		Long at = FETCHED.get(key);
		if (at == null || System.currentTimeMillis() - at > REFRESH_MS) {
			FETCHED.put(key, System.currentTimeMillis());
			POOL.execute(() -> {
				UUID real = online ? uuid : realUuid(name);
				if (real != null) fetch(key, real, sub);
				else CACHE.put(key, new ConcurrentHashMap<>());
			});
		}
		if (rankings == null || rankings.isEmpty()) return null;

		String wanted = MODE_IDS[Math.max(0, m.mode.modes.indexOf(m.mode.get()))];
		String chosen = wanted != null && rankings.containsKey(wanted) ? wanted : null;
		if (chosen == null && (wanted == null || m.fallback.on())) {
			int best = Integer.MAX_VALUE;
			for (Map.Entry<String, int[]> e : rankings.entrySet()) {
				int[] r = e.getValue();
				int score = (r[2] == 1 ? 1000 : 0) + r[0] * 2 + r[1];
				if (score < best) {
					best = score;
					chosen = e.getKey();
				}
			}
		}
		if (chosen == null) return null;
		int[] r = rankings.get(chosen);
		boolean retired = r[2] == 1;
		// Retired players show their peak, prefixed with R (like Tier Tagger).
		int tier = retired && r[3] > 0 ? r[3] : r[0], pos = retired && r[3] > 0 ? r[4] : r[1];
		String text = (retired ? "R" : "") + (pos == 0 ? "H" : "L") + "T" + tier;
		if (m.showMode.on()) text += " " + modeTitle(chosen);
		return retired ? new Tag(text, 0xA2D6FF, 'b') : new Tag(text, color(tier, pos), code(tier, pos));
	}

	private static String modeTitle(String id) {
		int i = java.util.Arrays.asList(MODE_IDS).indexOf(id);
		return i > 0 ? instance.mode.modes.get(i) : id;
	}

	/** Tier Tagger's colours: gold, silver, bronze, then purples. */
	private static int color(int tier, int pos) {
		boolean high = pos == 0;
		switch (tier) {
			case 1: return high ? 0xE8BA3A : 0xD5B355;
			case 2: return high ? 0xC4D3E7 : 0xA0A7B2;
			case 3: return high ? 0xF89F5A : 0xC67B42;
			case 4: return high ? 0x81749A : 0x655B79;
			default: return high ? 0x8F82A8 : 0x655B79;
		}
	}

	private static char code(int tier, int pos) {
		switch (tier) {
			case 1: return pos == 0 ? '6' : 'e';
			case 2: return pos == 0 ? 'f' : '7';
			case 3: return '6';
			default: return '5';
		}
	}

	/** A name's real account UUID from Mojang (for offline-mode servers), or null when there is none. */
	private static UUID realUuid(String name) {
		try {
			HttpURLConnection c = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openConnection();
			c.setConnectTimeout(5000);
			c.setReadTimeout(8000);
			c.setRequestProperty("User-Agent", "tatnat-client");
			if (c.getResponseCode() != 200) return null;
			try (InputStream in = c.getInputStream()) {
				String id = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().get("id").getAsString();
				return new UUID(Long.parseUnsignedLong(id.substring(0, 16), 16), Long.parseUnsignedLong(id.substring(16), 16));
			}
		} catch (Exception e) {
			TatnatClient.LOG.warn("[tier tagger] name lookup failed for {}: {}", name, e.toString());
			return null;
		}
	}

	private static void fetch(String key, UUID uuid, boolean sub) {
		String id = uuid.toString().replace("-", "");
		String url = sub ? "https://subtiers.net/api/v2/profile/" + uuid + "/rankings" : "https://pvptiers.com/api/profile/" + id;
		Map<String, int[]> out = new ConcurrentHashMap<>();
		try {
			HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
			c.setConnectTimeout(5000);
			c.setReadTimeout(8000);
			c.setRequestProperty("User-Agent", "tatnat-client");
			c.setRequestProperty("Accept", "application/json");
			if (c.getResponseCode() == 200) {
				try (InputStream in = c.getInputStream()) {
					JsonElement root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
					JsonObject obj = root.getAsJsonObject();
					// PvPTiers wraps the rankings in a profile; SubTiers' /rankings is the map itself.
					if (obj.has("rankings") && obj.get("rankings").isJsonObject()) obj = obj.getAsJsonObject("rankings");
					for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
						if (!e.getValue().isJsonObject()) continue;
						JsonObject r = e.getValue().getAsJsonObject();
						if (!r.has("tier") || !r.has("pos")) continue;
						out.put(e.getKey().toLowerCase(Locale.ROOT), new int[] {r.get("tier").getAsInt(), r.get("pos").getAsInt(),
								r.has("retired") && !r.get("retired").isJsonNull() && r.get("retired").getAsBoolean() ? 1 : 0,
								num(r, "peak_tier"), num(r, "peak_pos")});
					}
				}
			}
		} catch (Exception e) {
			TatnatClient.LOG.warn("[tier tagger] lookup failed for {}: {}", uuid, e.toString());
			FETCHED.put(key, System.currentTimeMillis() - REFRESH_MS + 60_000L); // try again in a minute
		}
		CACHE.put(key, out);
	}

	private static int num(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0;
	}
}
