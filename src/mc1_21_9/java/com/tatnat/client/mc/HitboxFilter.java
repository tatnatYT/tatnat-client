package com.tatnat.client.mc;

import com.tatnat.client.modules.impl.visual.Hitboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Which entities get a hitbox (Hitboxes "Show On"). Lives outside the mixin package: classes
 * there can't be loaded directly, so hooks must not call helpers defined in them.
 */
public final class HitboxFilter {
	private HitboxFilter() {
	}

	public static boolean wanted(Entity e) {
		Minecraft mc = Minecraft.getInstance();
		if (e == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) return false;
		Hitboxes h = Hitboxes.INSTANCE;
		if (h.targets.is("Players")) return e instanceof Player;
		if (h.targets.is("Living")) return e instanceof LivingEntity;
		return true;
	}
}
