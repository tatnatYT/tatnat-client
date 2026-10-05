package com.tatnat.client.mc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Log;

import net.fabricmc.api.ClientModInitializer;

/** Fabric entry point for 1.21.11: hands the shared core this version's implementations. */
public final class Entry implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("tatnat client");

	private static final Log LOG = new Log() {
		@Override
		public void info(String msg, Object... args) {
			LOGGER.info(msg, args);
		}

		@Override
		public void warn(String msg, Object... args) {
			LOGGER.warn(msg, args);
		}

		@Override
		public void error(String msg, Throwable t) {
			LOGGER.error(msg, t);
		}
	};

	@Override
	public void onInitializeClient() {
		TatnatClient.init(GameImpl.INSTANCE, FeaturesImpl.INSTANCE, LOG);
		DevTest.init();
	}
}
