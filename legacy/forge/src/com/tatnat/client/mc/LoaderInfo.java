package com.tatnat.client.mc;

import java.nio.file.Path;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/** What the platform code needs from the mod loader (this one: Forge 1.8.9). */
public final class LoaderInfo {
	private LoaderInfo() {
	}

	public static Path configDir() {
		return Loader.instance().getConfigDir().toPath();
	}

	public static boolean modLoaded(String id) {
		return Loader.isModLoaded(id);
	}

	public static String modVersion() {
		ModContainer c = Loader.instance().getIndexedModList().get("tatnatclient");
		return c == null ? "dev" : c.getVersion();
	}

	public static String minecraftVersion() {
		return Loader.MC_VERSION;
	}

	public static String loaderName() {
		return "Forge";
	}
}
