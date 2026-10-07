package com.tatnat.client.mc;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

/** What the platform code needs from the mod loader; each loader build has its own copy (this one: Fabric). */
public final class LoaderInfo {
	private LoaderInfo() {
	}

	public static Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	public static boolean modLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}

	public static String modVersion() {
		return FabricLoader.getInstance().getModContainer("tatnatclient").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
	}

	public static String minecraftVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("1.8.9");
	}

	public static String loaderName() {
		return "Fabric";
	}
}
