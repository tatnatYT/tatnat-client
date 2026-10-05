package com.tatnat.client.platform;

/** Logging, routed to the game's log by the platform (old versions use log4j, new ones slf4j). */
public interface Log {
	void info(String msg, Object... args);

	void warn(String msg, Object... args);

	void error(String msg, Throwable t);

	/** Fallback until the platform installs its logger. */
	Log STDOUT = new Log() {
		@Override
		public void info(String msg, Object... args) {
			System.out.println("[tatnat client] " + format(msg, args));
		}

		@Override
		public void warn(String msg, Object... args) {
			System.out.println("[tatnat client] WARN " + format(msg, args));
		}

		@Override
		public void error(String msg, Throwable t) {
			System.out.println("[tatnat client] ERROR " + msg);
			if (t != null) t.printStackTrace();
		}
	};

	/** Replaces each "{}" with the next argument, like slf4j. */
	static String format(String msg, Object... args) {
		StringBuilder sb = new StringBuilder();
		int a = 0, i = 0;
		while (i < msg.length()) {
			if (i + 1 < msg.length() && msg.charAt(i) == '{' && msg.charAt(i + 1) == '}' && a < args.length) {
				sb.append(args[a++]);
				i += 2;
			} else {
				sb.append(msg.charAt(i++));
			}
		}
		return sb.toString();
	}
}
