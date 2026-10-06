package com.tatnat.client.mc;

import java.nio.file.Path;

import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;

/** What the platform code needs from the mod loader (this one: Forge). */
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
		ModFileInfo file = LoadingModList.get() == null ? null : LoadingModList.get().getModFileById("tatnatclient");
		return file == null ? "dev" : file.versionString();
	}

	public static String loaderName() {
		return "Forge";
	}
}
