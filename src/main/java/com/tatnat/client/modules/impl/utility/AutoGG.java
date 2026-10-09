package com.tatnat.client.modules.impl.utility;

import java.util.Locale;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Says "gg" when a game ends. Watches chat for end-of-game lines (Hypixel-style by default, or
 * your own comma-separated list) and sends your message after the delay. At most once every 10
 * seconds so a burst of end-game lines only triggers one message.
 */
public class AutoGG extends Module {
	private final TextSetting message = add(new TextSetting("Message", "What to send", "gg", 100));
	private final SliderSetting delay = add(new SliderSetting("Delay", "Wait before sending", 1.0, 0, 5, 0.5, "s"));
	private final TextSetting triggers = add(new TextSetting("Triggers", "Comma-separated chat text that means the game ended",
			"1st Killer,Winner:,WINNER!,Victory!,Reward Summary,You won,You died! Want to play again", 300));

	private static final long COOLDOWN_MS = 10_000;
	private long lastSent;
	private int countdown = -1;

	public AutoGG() {
		super("Auto GG", "Automatically says gg when a game ends", Category.UTILITY, false);
		icon = Icons.Icon.TROPHY;
	}

	@Subscribe
	public void onChat(Events.Chat e) {
		if (countdown >= 0 || System.currentTimeMillis() - lastSent < COOLDOWN_MS) return;
		String text = e.text.toLowerCase(Locale.ROOT);
		for (String t : triggers.get().split(",")) {
			String trig = t.trim().toLowerCase(Locale.ROOT);
			if (!trig.isEmpty() && text.contains(trig)) {
				countdown = (int) Math.round(delay.get() * 20);
				return;
			}
		}
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		if (countdown < 0) return;
		if (countdown-- > 0) return;
		countdown = -1;
		String m = message.get().trim();
		if (!game().inWorld() || m.isEmpty()) return;
		if (m.startsWith("/")) game().sendCommand(m.substring(1));
		else game().sendChat(m);
		lastSent = System.currentTimeMillis();
	}
}
