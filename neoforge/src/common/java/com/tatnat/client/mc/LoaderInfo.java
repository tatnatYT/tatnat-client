package com.tatnat.client.mc;

import java.nio.file.Path;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

/** What the platform code needs from the mod loader (this one: NeoForge). */
public final class LoaderInfo {
	private LoaderInfo() {
	}

	public static Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	/** Works from the earliest point (mixin plugins), unlike ModList. */
	public static boolean modLoaded(String id) {
		return LoadingModList.get() != null && LoadingModList.get().getModFileById(id) != null;
	}

	public static String modVersion() {
		var file = LoadingModList.get() == null ? null : LoadingModList.get().getModFileById("tatnatclient");
		return file == null ? "dev" : file.versionString();
	}

	public static String loaderName() {
		return "NeoForge";
	}
}
