package com.tatnat.client.account;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tatnat.client.TatnatClient;

/**
 * In-game account switcher. The tatnat launcher passes {@code -Dtatnat.accounts=http://127.0.0.1:port/secret}
 * (see the launcher's core/accountbridge.js); we ask it for the account list and, on a switch, for a
 * fresh session, then swap the game's session object.
 * <p>
 * The swap is done by reflection so one implementation covers every version and loader: the client
 * is the singleton the platform layer holds, its session is the field whose object carries the current
 * player name, and the new session is built with whichever constructor that version has (name, uuid,
 * token, then optional xuid / client id and the account type). On 1.19.3+ the chat-signing keys belong
 * to an account too, so the user API service and the key manager are rebuilt for the new account.
 */
public final class AccountSwitcher {
	public static final class Account {
		public final String id, name, uuid, type;
		public final boolean launcherActive;

		Account(String id, String name, String uuid, String type, boolean launcherActive) {
			this.id = id;
			this.name = name;
			this.uuid = uuid;
			this.type = type;
			this.launcherActive = launcherActive;
		}

		public boolean microsoft() {
			return "microsoft".equals(type);
		}
	}

	private static final String BRIDGE = System.getProperty("tatnat.accounts");

	private static volatile List<Account> accounts = Collections.emptyList();
	private static volatile boolean loading, switching;
	private static volatile String status = "";
	private static volatile boolean statusError;

	private AccountSwitcher() {
	}

	/** True when the game was started by the tatnat launcher (so there is someone to ask). */
	public static boolean available() {
		return BRIDGE != null && BRIDGE.startsWith("http://127.0.0.1:");
	}

	public static List<Account> accounts() {
		return accounts;
	}

	public static boolean loading() {
		return loading;
	}

	public static boolean switching() {
		return switching;
	}

	public static String status() {
		return status;
	}

	public static boolean statusError() {
		return statusError;
	}

	/** The account the game is playing as right now. */
	public static String currentName() {
		try {
			return TatnatClient.game().playerName();
		} catch (Throwable t) {
			return "";
		}
	}

	/** Fetches the launcher's account list in the background. */
	public static void refresh() {
		if (!available() || loading) return;
		loading = true;
		Thread t = new Thread(() -> {
			try {
				List<Account> list = new ArrayList<>();
				for (String line : get("/accounts").split("\n")) {
					String[] p = line.split("\t", -1);
					if (p.length >= 5) list.add(new Account(p[0], p[1], p[2], p[3], "1".equals(p[4])));
				}
				accounts = list;
			} catch (Exception e) {
				setStatus("Could not reach the tatnat launcher (is it still open?)", true);
			} finally {
				loading = false;
			}
		}, "tatnat account list");
		t.setDaemon(true);
		t.start();
	}

	/** Switches the game to {@code account}: a fresh session from the launcher, then the swap on the game thread. */
	public static void switchTo(Account account) {
		if (!available() || switching) return;
		switching = true;
		setStatus("Signing in as " + account.name + "…", false);
		Thread t = new Thread(() -> {
			try {
				String[] s = get("/session?id=" + URLEncoder.encode(account.id, "UTF-8")).split("\t", -1);
				if (s.length < 4) throw new IllegalStateException("bad answer from the launcher");
				String name = s[0], uuid = s[1], token = s[2], userType = s[3], xuid = s.length > 4 ? s[4] : "";
				TatnatClient.game().execute(() -> {
					try {
						apply(name, uuid, token, userType, xuid);
						setStatus("Now playing as " + name + ". Rejoin a server to use it there.", false);
						refresh();
					} catch (Throwable e) {
						TatnatClient.LOG.error("[accounts] switch failed", e);
						setStatus("Switching failed: " + e, true);
					} finally {
						switching = false;
					}
				});
			} catch (Exception e) {
				TatnatClient.LOG.error("[accounts] could not get a session", e);
				setStatus("Could not sign in as " + account.name + ": " + e.getMessage(), true);
				switching = false;
			}
		}, "tatnat account switch");
		t.setDaemon(true);
		t.start();
	}

	private static void setStatus(String s, boolean error) {
		status = s;
		statusError = error;
	}

	private static String get(String path) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(BRIDGE + path).openConnection();
		c.setConnectTimeout(3000);
		c.setReadTimeout(30000); // a Microsoft sign-in refresh can take a few seconds
		int code = c.getResponseCode();
		InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		for (int n; in != null && (n = in.read(buf)) > 0;) out.write(buf, 0, n);
		String body = new String(out.toByteArray(), StandardCharsets.UTF_8);
		if (code >= 400) throw new IllegalStateException(body.isEmpty() ? "HTTP " + code : body);
		return body;
	}

	// ------------------------------------------------------------------ the swap

	private static void apply(String name, String uuid, String token, String userType, String xuid) throws Exception {
		Object client = findClient();
		if (client == null) throw new IllegalStateException("game client not found");
		String current = currentName();
		Field sessionField = null;
		Object session = null;
		for (Field f : allFields(client.getClass())) {
			if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive() || f.getType().getName().startsWith("java.")) continue;
			f.setAccessible(true);
			Object v = f.get(client);
			if (v != null && holdsString(v, current)) {
				sessionField = f;
				session = v;
				break;
			}
		}
		if (sessionField == null) throw new IllegalStateException("session not found");
		Object fresh = newSession(session.getClass(), name, uuid, token, userType, xuid);
		setField(sessionField, client, fresh);
		TatnatClient.LOG.info("[accounts] now playing as {}", name);
		try {
			refreshChatKeys(client, fresh, token);
		} catch (Throwable t) {
			TatnatClient.LOG.warn("[accounts] could not refresh chat keys: {}", t.toString());
		}
	}

	/** The Minecraft client: a singleton (a class with a static field of its own type) the platform layer holds. */
	private static Object findClient() throws Exception {
		Object impl = TatnatClient.game();
		List<Object> found = new ArrayList<>();
		for (Field f : allFields(impl.getClass())) {
			if (f.getType().isPrimitive()) continue;
			f.setAccessible(true);
			Object v = Modifier.isStatic(f.getModifiers()) ? f.get(null) : f.get(impl);
			if (v != null && isSingleton(v.getClass())) found.add(v);
		}
		for (Method m : impl.getClass().getDeclaredMethods()) {
			if (m.getParameterTypes().length != 0 || m.getReturnType().isPrimitive() || !isSingleton(m.getReturnType())) continue;
			m.setAccessible(true);
			Object v = Modifier.isStatic(m.getModifiers()) ? m.invoke(null) : m.invoke(impl);
			if (v != null) found.add(v);
		}
		// The client is the one that holds the session (the current player name).
		String current = currentName();
		for (Object c : found) {
			for (Field f : allFields(c.getClass())) {
				if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive() || f.getType().getName().startsWith("java.")) continue;
				f.setAccessible(true);
				Object v = f.get(c);
				if (v != null && holdsString(v, current)) return c;
			}
		}
		return null;
	}

	private static boolean isSingleton(Class<?> c) {
		if (c.getName().startsWith("java.")) return false;
		for (Field f : c.getDeclaredFields()) if (Modifier.isStatic(f.getModifiers()) && f.getType() == c) return true;
		return false;
	}

	private static boolean holdsString(Object o, String value) throws IllegalAccessException {
		if (value == null || value.isEmpty()) return false;
		int strings = 0;
		boolean match = false;
		for (Field f : o.getClass().getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()) || f.getType() != String.class) continue;
			strings++;
			f.setAccessible(true);
			if (value.equals(f.get(o))) match = true;
		}
		// A session has the name plus at least the token as strings, and few fields overall.
		return match && strings >= 2 && o.getClass().getDeclaredFields().length <= 12;
	}

	/** Builds a session with the longest constructor, filling parameters by type and order. */
	private static Object newSession(Class<?> type, String name, String uuid, String token, String userType, String xuid) throws Exception {
		Constructor<?> best = null;
		for (Constructor<?> c : type.getDeclaredConstructors()) if (best == null || c.getParameterTypes().length > best.getParameterTypes().length) best = c;
		if (best == null) throw new IllegalStateException("no session constructor");
		Class<?>[] params = best.getParameterTypes();
		Object[] args = new Object[params.length];
		String plain = uuid.replace("-", "");
		UUID id = UUID.fromString(plain.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
		// Strings in order: name, uuid, token, then the type ("msa" / "legacy"; 1.8 - 1.12 say "mojang" / "legacy").
		String[] strings = {name, plain, token, "msa".equals(userType) && legacyTypes(type) ? "mojang" : userType};
		int si = 0, optionals = 0;
		for (int i = 0; i < params.length; i++) {
			Class<?> p = params[i];
			if (p == String.class) args[i] = si < strings.length ? strings[si++] : "";
			else if (p == UUID.class) {
				args[i] = id;
				si = Math.max(si, 2); // the uuid takes the second slot
			} else if (p == Optional.class) args[i] = optionals++ == 0 && !xuid.isEmpty() ? Optional.of(xuid) : Optional.empty();
			else if (p.isEnum()) args[i] = enumFor(p, userType);
			else args[i] = null;
		}
		best.setAccessible(true);
		return best.newInstance(args);
	}

	private static boolean legacyTypes(Class<?> session) {
		for (Class<?> c : session.getDeclaredClasses()) {
			if (!c.isEnum()) continue;
			for (Object e : c.getEnumConstants()) if ("MSA".equals(((Enum<?>) e).name())) return false;
		}
		return true;
	}

	private static Object enumFor(Class<?> e, String userType) {
		Object[] all = e.getEnumConstants();
		String want = "msa".equals(userType) ? "MSA" : "LEGACY";
		for (Object o : all) if (((Enum<?>) o).name().equalsIgnoreCase(want)) return o;
		// Obfuscated constant names: MSA is the last of LEGACY / MOJANG / MSA.
		return "msa".equals(userType) ? all[all.length - 1] : all[0];
	}

	/**
	 * 1.19.3+: chat messages are signed with keys tied to the account. Rebuild the user API service
	 * (authlib, never obfuscated) and the key manager through its static factory
	 * {@code create(UserApiService, session, Path)}; found by its parameter types.
	 */
	private static void refreshChatKeys(Object client, Object session, String token) throws Exception {
		Class<?> userApi;
		try {
			userApi = Class.forName("com.mojang.authlib.minecraft.UserApiService", false, client.getClass().getClassLoader());
		} catch (ClassNotFoundException e) {
			return; // older than 1.16: nothing to do
		}
		Object auth = null;
		Field apiField = null;
		for (Field f : allFields(client.getClass())) {
			if (Modifier.isStatic(f.getModifiers())) continue;
			f.setAccessible(true);
			if (f.getType().getName().equals("com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService")) auth = f.get(client);
			if (f.getType() == userApi) apiField = f;
		}
		Object api = null;
		if (auth != null && apiField != null) {
			try {
				api = auth.getClass().getMethod("createUserApiService", String.class).invoke(auth, token);
			} catch (Throwable t) {
				api = userApi.getField("OFFLINE").get(null);
			}
			setField(apiField, client, api);
		}
		if (api == null) return;
		for (Field f : allFields(client.getClass())) {
			if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
			Class<?> t = f.getType();
			Method create = null;
			for (Method m : t.getDeclaredMethods()) {
				Class<?>[] p = m.getParameterTypes();
				if (Modifier.isStatic(m.getModifiers()) && t.isAssignableFrom(m.getReturnType()) && p.length == 3 && p[0] == userApi
						&& p[1] == session.getClass() && p[2] == Path.class) create = m;
			}
			if (create == null) continue;
			f.setAccessible(true);
			Path dir = gameDir(f.get(client));
			create.setAccessible(true);
			setField(f, client, create.invoke(null, api, session, dir));
			TatnatClient.LOG.info("[accounts] chat keys now belong to the new account");
			return;
		}
	}

	/** The game folder: the old key manager stores {@code <game dir>/profilekeys}; otherwise the working directory. */
	private static Path gameDir(Object oldManager) {
		if (oldManager != null) {
			for (Field f : oldManager.getClass().getDeclaredFields()) {
				if (f.getType() != Path.class || Modifier.isStatic(f.getModifiers())) continue;
				try {
					f.setAccessible(true);
					Path p = (Path) f.get(oldManager);
					if (p != null && p.getParent() != null) return p.getParent();
				} catch (Throwable ignored) {
				}
			}
		}
		return Paths.get("").toAbsolutePath();
	}

	/**
	 * Dev check ({@code -Dtatnat.accountcheck=<account id>}, with a stand-in bridge): once the game has
	 * loaded, lists the accounts and switches to that one, logging the result.
	 */
	public static final class DevCheck {
		private int ticks;

		@com.tatnat.client.event.Subscribe
		public void onTick(com.tatnat.client.event.Events.Tick e) {
			ticks++;
			if (ticks == 40) refresh();
			if (ticks == 80) {
				String want = System.getProperty("tatnat.accountcheck");
				TatnatClient.LOG.info("[accountcheck] before: {} accounts={}", currentName(), accounts.size());
				for (Account a : accounts) if (a.id.equals(want)) switchTo(a);
			}
			if (ticks == 160) TatnatClient.LOG.info("[accountcheck] after: {} status={}", currentName(), status);
		}
	}

	private static List<Field> allFields(Class<?> c) {
		List<Field> out = new ArrayList<>();
		for (; c != null && c != Object.class; c = c.getSuperclass()) Collections.addAll(out, c.getDeclaredFields());
		return out;
	}

	/** Sets a (possibly final) instance field. */
	private static void setField(Field f, Object owner, Object value) throws Exception {
		f.setAccessible(true);
		f.set(owner, value);
	}
}
