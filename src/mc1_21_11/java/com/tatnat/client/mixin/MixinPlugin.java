package com.tatnat.client.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Skips hooks that clash with other mods instead of crashing: Sodium rewrites the vanilla chunk
 * renderer, so Chunk Animator's hook is only applied when Sodium isn't installed.
 */
public class MixinPlugin implements IMixinConfigPlugin {
	private boolean sodium;

	@Override
	public void onLoad(String mixinPackage) {
		sodium = FabricLoader.getInstance().isModLoaded("sodium");
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.endsWith("WorldLookMixins$Chunks")) return !sodium;
		return true;
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
