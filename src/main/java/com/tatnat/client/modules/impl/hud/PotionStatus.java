package com.tatnat.client.modules.impl.hud;

import java.util.ArrayList;
import java.util.List;

import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.ui.render.Icons;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;

/** Your active potion effects: icon, name with level, and a ticking timer. */
public class PotionStatus extends HudModule {
	private final BooleanSetting amplifiers = add(new BooleanSetting("Show Amplifiers", "Add the level, like Speed II", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Dark box behind the list", false));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the text", true));
	private final BooleanSetting blink = add(new BooleanSetting("Blink When Ending", "Flash the timer in the last 10 seconds", true));

	private static final String[] ROMAN = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

	public PotionStatus() {
		super("Potion Status", "Shows your active effects and how long they last", false, 1.0, 0.25);
		icon = Icons.Icon.POTION;
	}

	private List<MobEffectInstance> effects(boolean preview) {
		List<MobEffectInstance> list = mc.player == null ? new ArrayList<>() : new ArrayList<>(mc.player.getActiveEffects());
		if (list.isEmpty() && preview) {
			list.add(new MobEffectInstance(MobEffects.SPEED, 20 * 90, 1));
			list.add(new MobEffectInstance(MobEffects.STRENGTH, 20 * 45, 0));
		}
		return list;
	}

	@Override
	public boolean hasContent() {
		return mc.player != null && !mc.player.getActiveEffects().isEmpty();
	}

	@Override
	protected long draw(GuiGraphics g, boolean preview) {
		List<MobEffectInstance> list = effects(preview);
		float tickRate = mc.level != null ? mc.level.tickRateManager().tickrate() : 20f;
		int w = 0;
		for (MobEffectInstance e : list) w = Math.max(w, 22 + Math.max(mc.font.width(name(e)), mc.font.width(MobEffectUtil.formatDuration(e, 1f, tickRate))));
		int h = list.size() * 22;
		if (background.on() && !list.isEmpty()) g.fill(-3, -3, w + 3, h + 1, 0x6F000000);
		int y = 0;
		for (MobEffectInstance e : list) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(e.getEffect()), 0, y, 18, 18);
			g.drawString(mc.font, name(e), 22, y, 0xFFFFFFFF, shadow.on());
			boolean ending = !e.isInfiniteDuration() && e.getDuration() < 200;
			boolean hide = blink.on() && ending && System.currentTimeMillis() / 300 % 2 == 0;
			if (!hide) g.drawString(mc.font, MobEffectUtil.formatDuration(e, 1f, tickRate), 22, y + 10, ending ? 0xFFFF5555 : 0xFFAAAAAA, shadow.on());
			y += 22;
		}
		return size(Math.max(1, w + 3), Math.max(1, h - 2));
	}

	private Component name(MobEffectInstance e) {
		Component n = e.getEffect().value().getDisplayName();
		int amp = e.getAmplifier() + 1;
		if (!amplifiers.on() || amp <= 1) return n;
		return n.copy().append(amp < ROMAN.length ? ROMAN[amp] : " " + amp);
	}
}
