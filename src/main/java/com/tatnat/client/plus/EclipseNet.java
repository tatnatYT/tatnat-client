package com.tatnat.client.plus;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tatnat.client.TatnatClient;

/**
 * Who else is playing with Eclipse, and who has Eclipse Plus with which cape.
 *
 * The launcher passes the Plus server's address and a signed session token for your account
 * ({@code -Dtatnat.plusServer}, {@code -Dtatnat.plusToken}). Hooks call {@link #cape} / {@link #badge}
 * for players as they're drawn; a background thread asks the server about new players in batches and
 * reports that you're online, so other Eclipse players see your badge. Without a server nothing is sent.
 */
public final class EclipseNet {
	/** Badge kinds. */
	public static final int NONE = 0, ECLIPSE = 1, PLUS = 2;

	private static final String SERVER = trim(System.getProperty("tatnat.plusServer"));
	private static final String TOKEN = trim(System.getProperty("tatnat.plusToken"));
	private static final long ASK_AGAIN_MS = 10 * 60_000L, HEARTBEAT_MS = 3 * 60_000L;
	private static final int BATCH = 100;

	private static final Set<String> eclipse = ConcurrentHashMap.newKeySet();
	/** Plus members and their cape ("none" = no cape shown). */
	private static final Map<String, String> plus = new ConcurrentHashMap<>();
	private static final Map<String, Long> asked = new ConcurrentHashMap<>();
	private static final Set<String> pending = ConcurrentHashMap.newKeySet();
	/** Test hook: a cape on your own player without a server. */
	private static volatile String devCape = trim(System.getProperty("tatnat.devCape"));
	private static volatile Thread worker;
	private static volatile long lastBeat;
	private static boolean warned;

	private EclipseNet() {
	}

	/** The Plus cape {@code id} wears, or null; {@code me}: it's your own player. */
	public static String cape(UUID id, boolean me) {
		if (id == null) return null;
		if (me && devCape != null) return devCape;
		// Your own cape as you picked it in the menu (before the next lookup catches up).
		if (me && myCape != null) return myCape.equals("none") ? null : myCape;
		String k = key(id);
		see(k);
		String c = plus.get(k);
		return c == null || c.equals("none") ? null : c;
	}

	/** Which badge goes next to {@code id}'s name: {@link #NONE}, {@link #ECLIPSE} or {@link #PLUS}. */
	public static int badge(UUID id, boolean me) {
		if (id == null) return NONE;
		String k = key(id);
		see(k);
		if (plus.containsKey(k) || (me && (devCape != null || Boolean.TRUE.equals(myPlus)))) return PLUS;
		// You're always playing with Eclipse yourself.
		if (me || eclipse.contains(k)) return ECLIPSE;
		return NONE;
	}

	// ---------- your own Plus status and cape (the menu's Eclipse Plus page) ----------
	private static volatile Boolean myPlus;
	private static volatile String myCape, myStatus = "";
	private static volatile boolean myOwner, myBusy, myError;

	/** True when the game was started from the Eclipse Launcher with a Plus session. */
	public static boolean available() {
		return SERVER != null && TOKEN != null;
	}

	/** Null until known; then whether you have Eclipse Plus. */
	public static Boolean hasPlus() {
		return myPlus;
	}

	public static String myCape() {
		return myCape;
	}

	public static boolean owner() {
		return myOwner;
	}

	public static boolean busy() {
		return myBusy;
	}

	/** A short message about the last action ("" for none), and whether it was an error. */
	public static String status() {
		return myStatus;
	}

	public static boolean statusError() {
		return myError;
	}

	private static final Pattern FIELD = Pattern.compile("\"(plus|owner)\"\\s*:\\s*(true|false)|\"cape\"\\s*:\\s*(null|\"([a-z]+)\")|\"message\"\\s*:\\s*\"([^\"]*)\"");

	private static void readStatus(String json) {
		Matcher m = FIELD.matcher(json);
		while (m.find()) {
			if ("plus".equals(m.group(1))) myPlus = Boolean.valueOf(m.group(2));
			else if ("owner".equals(m.group(1))) myOwner = Boolean.parseBoolean(m.group(2));
			else if (m.group(3) != null) myCape = m.group(4);
		}
	}

	/** Asks the server for your current Plus status and cape (in the background). */
	public static void refreshMe() {
		if (!available() || myBusy) return;
		background(() -> readStatus(post("/me", "{\"token\":\"" + TOKEN + "\"}")), null);
	}

	/** Wears {@code cape} ("none" for no cape); everyone sees it after their next lookup. */
	public static void equip(String cape) {
		if (!available() || myBusy) return;
		background(() -> {
			readStatus(post("/equip", "{\"token\":\"" + TOKEN + "\",\"cape\":\"" + cape + "\"}"));
			myCape = cape;
		}, cape.equals("none") ? "Cape hidden." : "Cape changed. Other players see it within a few minutes.");
	}

	private interface Call {
		void run() throws Exception;
	}

	private static void background(Call call, String done) {
		myBusy = true;
		Thread t = new Thread(() -> {
			try {
				call.run();
				if (done != null) {
					myStatus = done;
					myError = false;
				}
			} catch (Exception e) {
				String msg = e.getMessage() == null ? e.toString() : e.getMessage();
				Matcher m = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"").matcher(msg);
				myStatus = m.find() ? m.group(1) : "Couldn't reach the Eclipse Plus server. Try again in a moment.";
				myError = true;
			} finally {
				myBusy = false;
			}
		}, "Eclipse Plus");
		t.setDaemon(true);
		t.start();
	}

	/** For dev tests: wear {@code cape} yourself without a server (null to stop). */
	public static void devCape(String cape) {
		devCape = cape;
	}

	private static void see(String k) {
		if (SERVER == null || TOKEN == null) return;
		Long t = asked.get(k);
		if (t == null || System.currentTimeMillis() - t > ASK_AGAIN_MS) pending.add(k);
		if (worker == null) start();
	}

	private static synchronized void start() {
		if (worker != null) return;
		Thread t = new Thread(EclipseNet::loop, "Eclipse online");
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		worker = t;
		t.start();
	}

	private static void loop() {
		while (true) {
			try {
				long now = System.currentTimeMillis();
				if (!pending.isEmpty() || now - lastBeat > HEARTBEAT_MS) {
					List<String> ids = new ArrayList<>();
					for (Iterator<String> it = pending.iterator(); it.hasNext() && ids.size() < BATCH;) {
						ids.add(it.next());
						it.remove();
					}
					lookup(ids);
					lastBeat = now;
					for (String k : ids) asked.put(k, now);
				}
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				if (!warned) TatnatClient.LOG.warn("Eclipse online check failed (retrying quietly): {}", e.toString());
				warned = true;
				try {
					Thread.sleep(30_000);
				} catch (InterruptedException ie) {
					return;
				}
			}
		}
	}

	private static final Pattern ID = Pattern.compile("\"([0-9a-f]{32})\"");
	private static final Pattern PAIR = Pattern.compile("\"([0-9a-f]{32})\"\\s*:\\s*\"([a-z]+)\"");

	/** POST /lookup {token, ids} -> {"eclipse":[ids online with Eclipse], "plus":{id: cape}}; also marks you online. */
	private static void lookup(List<String> ids) throws Exception {
		StringBuilder body = new StringBuilder("{\"token\":\"").append(TOKEN).append("\",\"ids\":[");
		for (int i = 0; i < ids.size(); i++) body.append(i == 0 ? "" : ",").append('"').append(ids.get(i)).append('"');
		String res = post("/lookup", body.append("]}").toString());
		int p = res.indexOf("\"plus\"");
		String onlinePart = p < 0 ? res : res.substring(0, p), plusPart = p < 0 ? "" : res.substring(p);
		for (String k : ids) {
			eclipse.remove(k);
			plus.remove(k);
		}
		Matcher m = ID.matcher(onlinePart);
		while (m.find()) eclipse.add(m.group(1));
		m = PAIR.matcher(plusPart);
		while (m.find()) plus.put(m.group(1), m.group(2));
	}

	private static String post(String path, String json) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(SERVER + path).openConnection();
		c.setConnectTimeout(8000);
		c.setReadTimeout(8000);
		c.setRequestMethod("POST");
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", "application/json");
		c.setRequestProperty("User-Agent", "EclipseClient");
		try (OutputStream out = c.getOutputStream()) {
			out.write(json.getBytes(StandardCharsets.UTF_8));
		}
		int code = c.getResponseCode();
		try (InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream()) {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] b = new byte[4096];
			int n;
			while (in != null && (n = in.read(b)) > 0) buf.write(b, 0, n);
			String s = new String(buf.toByteArray(), StandardCharsets.UTF_8);
			if (code >= 400) throw new IllegalStateException("HTTP " + code + " " + s);
			return s;
		}
	}

	private static String key(UUID id) {
		return id.toString().replace("-", "").toLowerCase(java.util.Locale.ROOT);
	}

	private static String trim(String s) {
		return s == null || s.trim().isEmpty() ? null : s.trim().replaceAll("/+$", "");
	}
}
