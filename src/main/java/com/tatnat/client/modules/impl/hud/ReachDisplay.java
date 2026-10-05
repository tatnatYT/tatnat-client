package com.tatnat.client.modules.impl.hud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.theme.Colors;

import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * How far away you hit from, measured from your eyes to the exact point the hit ray touched the
 * target's hitbox. Shows the last value on the HUD and, optionally, as text that floats up from
 * the target and fades out.
 */
public class ReachDisplay extends TextHudModule {
	private final BooleanSetting floating = add(new BooleanSetting("Floating Text", "Show the distance above the entity you hit", true));
	private final ColorSetting floatColor = add(new ColorSetting("Floating Color", "Colour of the floating text", 0xFFFFD54F, true));
	private final SliderSetting fade = add(new SliderSetting("Fade Time", "How long the floating text stays", 1000, 300, 3000, 100, "ms"));

	private record Pop(Vec3 pos, String text, long time) {
	}

	private final List<Pop> pops = new ArrayList<>();
	private double last = -1;
	private long lastTime;

	public ReachDisplay() {
		super("Reach Display", "Shows how far away you hit from", false, 0.0, 0.45);
		icon = Icons.Icon.RULER;
	}

	@Subscribe
	public void onAttack(Events.Attack e) {
		HitResult hit = mc.hitResult;
		if (mc.player == null || !(hit instanceof EntityHitResult ehr) || ehr.getEntity() != e.target) return;
		last = mc.player.getEyePosition().distanceTo(hit.getLocation());
		lastTime = System.currentTimeMillis();
		if (floating.on()) {
			Vec3 top = new Vec3(e.target.getX(), e.target.getBoundingBox().maxY + 0.35, e.target.getZ());
			pops.add(new Pop(top, String.format(Locale.ROOT, "%.2f", last), lastTime));
			if (pops.size() > 12) pops.remove(0);
		}
	}

	@Subscribe
	public void onGizmos(Events.Gizmos e) {
		long now = System.currentTimeMillis();
		for (Iterator<Pop> it = pops.iterator(); it.hasNext();) {
			Pop p = it.next();
			float t = (now - p.time) / fade.floatValue();
			if (t >= 1f) {
				it.remove();
				continue;
			}
			int color = Colors.fade(floatColor.color(), t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f);
			Gizmos.billboardText(p.text, p.pos.add(0, t * 0.6, 0), TextGizmo.Style.forColorAndCentered(color).withScale(0.6f));
		}
	}

	@Override
	protected String label() {
		return "Reach";
	}

	@Override
	protected String value(boolean preview) {
		if (last < 0 || System.currentTimeMillis() - lastTime > 4000) return preview ? "3.00" : "-";
		return String.format(Locale.ROOT, "%.2f", last);
	}
}
