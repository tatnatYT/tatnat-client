package com.tatnat.client.mc;

import com.tatnat.client.modules.impl.visual.Hitboxes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Which entities get a hitbox (Hitboxes "Show On"). Lives outside the mixin package: classes
 * there can't be loaded directly, so hooks must not call helpers defined in them.
 */
public final class HitboxFilter {
	private HitboxFilter() {
	}

	public static boolean wanted(Entity e) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (e == mc.getCameraEntity() && mc.options.perspective == 0) return false;
		Hitboxes h = Hitboxes.INSTANCE;
		if (h.targets.is("Players")) return e instanceof PlayerEntity;
		if (h.targets.is("Living")) return e instanceof LivingEntity;
		return true;
	}
}
