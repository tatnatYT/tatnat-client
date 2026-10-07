package com.tatnat.client.mc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Log;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/** Forge (1.8.9 - 1.12.2) entry point: hands the shared core this version's implementations (client only). */
@Mod(modid = TatnatClient.ID, name = "tatnat client", version = "1.0.0", clientSideOnly = true)
public final class ForgeEntry {
	private static final Logger LOGGER = LogManager.getLogger("tatnat client");

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

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		TatnatClient.init(GameImpl.INSTANCE, FeaturesImpl.INSTANCE, LOG);
		DevTest.init();
	}
}
