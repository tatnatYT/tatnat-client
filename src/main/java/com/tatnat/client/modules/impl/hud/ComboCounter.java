package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.HudModule;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;
import com.tatnat.client.ui.theme.Colors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;

/**
 * Counts hits you land in a row. A hit counts when the target wasn't still flashing red from
 * your previous hit (so spam-clicking doesn't inflate it). Taking damage resets it, and it fades
 * out once you stop hitting.
 */
public class ComboCounter extends HudModule {
	private final ModeSetting font = add(new ModeSetting("Font", "Bold or regular text", "Bold", "Bold", "Regular"));
	private final SliderSetting fadeOut = add(new SliderSetting("Fade Out", "How long the combo stays after your last hit", 2500, 500, 6000, 100, "ms"));
	private final ColorSetting color = add(new ColorSetting("Color", "Text colour", 0xFFFFFFFF, true));
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the text", true));

	private int combo;
	private long lastHit;
	private int lastHurtTime;

	public ComboCounter() {
		super("Combo Counter", "Shows how many hits in a row you've landed", false, 0.5, 0.62);
		icon = Icons.Icon.COMBO;
	}

	@Subscribe
	public void onAttack(Events.Attack e) {
		if (!(e.target instanceof LivingEntity target) || !target.isAlive() || target.hurtTime > 0) return;
		if (System.currentTimeMillis() - lastHit > fadeOut.get()) combo = 0;
		combo++;
		lastHit = System.currentTimeMillis();
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (mc.player == null) return;
		int hurt = mc.player.hurtTime;
		// hurtTime jumps up to its maximum on the tick you take damage.
		if (hurt > lastHurtTime) combo = 0;
		lastHurtTime = hurt;
	}

	@Override
	public boolean hasContent() {
		return combo > 0 && System.currentTimeMillis() - lastHit < fadeOut.get();
	}

	@Override
	protected long draw(GuiGraphics g, boolean preview) {
		int n = combo;
		float alpha = 1f;
		long since = System.currentTimeMillis() - lastHit;
		if (!hasContent()) {
			if (!preview) return size(1, 1);
			n = 3;
		} else if (since > fadeOut.get() - 400) {
			alpha = Math.max(0f, (fadeOut.floatValue() - since) / 400f);
		}
		Component text = Component.literal(n + " Combo").withStyle(Style.EMPTY.withBold(font.is("Bold")));
		int w = mc.font.width(text);
		g.pose().pushMatrix();
		g.pose().scale(2f, 2f);
		g.drawString(mc.font, text, 0, 0, Colors.fade(color.color(), alpha), shadow.on());
		g.pose().popMatrix();
		return size(w * 2, 18);
	}
}
