package com.tatnat.client.mc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Log;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge entry point: hands the shared core this version's implementations (client only). */
@Mod(TatnatClient.ID)
public final class ForgeEntry {
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

	public ForgeEntry() {
		if (FMLEnvironment.dist != Dist.CLIENT) return;
		TatnatClient.init(GameImpl.INSTANCE, FeaturesImpl.INSTANCE, LOG);
		DevTest.init();
	}
}
