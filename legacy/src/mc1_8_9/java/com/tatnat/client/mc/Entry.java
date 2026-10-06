package com.tatnat.client.mc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Log;

import net.fabricmc.api.ClientModInitializer;

/** Fabric (Legacy Fabric) entry point: hands the shared core this version's implementations. */
public final class Entry implements ClientModInitializer {
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

	@Override
	public void onInitializeClient() {
		TatnatClient.init(GameImpl.INSTANCE, FeaturesImpl.INSTANCE, LOG);
		DevTest.init();
		if ("audit".equals(System.getProperty("tatnat.devtest"))) {
			// Dev check: apply every hook now so all broken ones are reported in one run, then quit.
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
					net.minecraft.client.MinecraftClient.getInstance().scheduleStop();
				}
			});
		}
	}
}
