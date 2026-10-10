package com.tatnat.client.mc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Log;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** NeoForge 1.20.x entry point (no client-only @Mod there yet): hands the shared core this version's implementations (client only). */
@Mod(TatnatClient.ID)
public final class NeoEntry20 {
	private static final Logger LOGGER = LoggerFactory.getLogger("Eclipse Client");

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

	public NeoEntry20() {
		if (net.neoforged.fml.loading.FMLEnvironment.dist != Dist.CLIENT) return;
		TatnatClient.init(GameImpl.INSTANCE, FeaturesImpl.INSTANCE, LOG);
		DevTest.init();
		if ("audit".equals(System.getProperty("tatnat.devtest"))) {
			// Release check: apply every hook now so broken ones (e.g. a bad refmap) show up at the menu.
			TatnatClient.EVENTS.register(new Object() {
				private boolean done;

				@com.tatnat.client.event.Subscribe
				public void onTick(com.tatnat.client.event.Events.Tick e) {
					if (done) return;
					done = true;
					try {
						org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
						LOG.info("[audit] mixin audit finished");
					} catch (Throwable t) {
						LOG.error("[audit] mixin audit failed", t);
					}
				}
			});
		}
	}
}
