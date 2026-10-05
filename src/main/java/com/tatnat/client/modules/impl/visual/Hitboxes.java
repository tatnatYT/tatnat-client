package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Clean wireframe hitboxes (like F3+B, but with your own colours and width) plus a line showing
 * where each entity is looking. Drawn through Minecraft's gizmo pass each frame.
 */
public class Hitboxes extends Module {
	private final ModeSetting targets = add(new ModeSetting("Show On", "Which entities get a hitbox", "Players", "Players", "Living", "All"));
	private final ColorSetting boxColor = add(new ColorSetting("Box Color", "Colour of the hitbox", 0xFFFFFFFF, true));
	private final SliderSetting width = add(new SliderSetting("Line Thickness", "Width of the lines", 1.5, 1, 5, 0.5, "px"));
	private final BooleanSetting lookLine = add(new BooleanSetting("Line of Sight", "Line showing where they're looking", true));
	private final ColorSetting lookColor = add(new ColorSetting("Line of Sight Color", "Colour of the look line", 0xFFFF3B3B, false));

	public Hitboxes() {
		super("Hitboxes", "Wireframe boxes around players and mobs", Category.VISUAL, false);
		icon = Icons.Icon.BOX;
	}

	@Subscribe
	public void onGizmos(Events.Gizmos e) {
		if (mc.level == null) return;
		float pt = e.partialTick;
		GizmoStyle style = GizmoStyle.stroke(boxColor.color(), width.floatValue());
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) continue;
			if (entity.isInvisible()) continue;
			if (targets.is("Players") && !(entity instanceof Player)) continue;
			if (targets.is("Living") && !(entity instanceof LivingEntity)) continue;
			// Interpolated so the box doesn't lag behind the model.
			Vec3 offset = entity.getPosition(pt).subtract(entity.position());
			AABB box = entity.getBoundingBox().move(offset);
			if (!e.frustum.isVisible(box.inflate(0.5))) continue;
			Gizmos.cuboid(box, style);
			if (lookLine.on() && entity instanceof LivingEntity) {
				Vec3 eye = entity.getEyePosition(pt);
				Gizmos.line(eye, eye.add(entity.getViewVector(pt).scale(2.0)), lookColor.color(), width.floatValue());
			}
		}
	}
}
