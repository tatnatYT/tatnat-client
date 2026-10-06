package com.tatnat.client.mc;

import com.tatnat.client.modules.impl.visual.Hitboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Hitboxes for 1.21.9+: drawn through Minecraft's gizmo pass (the same system F3+B uses). */
public final class HitboxGizmos {
	private HitboxGizmos() {
	}

	public static void emit(Frustum frustum, float pt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Hitboxes h = Hitboxes.INSTANCE;
		GizmoStyle style = GizmoStyle.stroke(h.boxColor.color(), h.width.floatValue());
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) continue;
			if (entity.isInvisible()) continue;
			if (h.targets.is("Players") && !(entity instanceof Player)) continue;
			if (h.targets.is("Living") && !(entity instanceof LivingEntity)) continue;
			// Interpolated so the box doesn't lag behind the model.
			Vec3 offset = entity.getPosition(pt).subtract(entity.position());
			AABB box = entity.getBoundingBox().move(offset);
			if (!frustum.isVisible(box.inflate(0.5))) continue;
			Gizmos.cuboid(box, style);
			if (h.lookLine.on() && entity instanceof LivingEntity) {
				Vec3 eye = entity.getEyePosition(pt);
				Gizmos.line(eye, eye.add(entity.getViewVector(pt).scale(2.0)), h.lookColor.color(), h.width.floatValue());
			}
		}
	}
}
