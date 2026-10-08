package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.SliderSetting;

/**
 * The chat's look: background opacity, text size and width, kept in step with the game's own chat
 * options (background opacity needs 1.14+).
 */
public class CustomChat extends Module {
	private final SliderSetting opacity = add(new SliderSetting("Background Opacity", "How dark the chat background is", 30, 0, 100, 5, "%"));
	private final SliderSetting scale = add(new SliderSetting("Chat Scale", "Size of the chat text", 100, 30, 100, 5, "%"));
	private final SliderSetting width = add(new SliderSetting("Chat Width", "How wide the chat is", 100, 20, 100, 5, "%"));

	private String applied = "";

	public CustomChat() {
		super("Custom Chat", "Chat background, size and width", Category.VISUAL, false);
		icon = com.tatnat.client.ui.render.Icons.Icon.CHAT;
	}

	@Override
	protected void onEnable() {
		applied = "";
	}

	@Subscribe
	public void onTick(Events.Tick e) {
		String want = opacity.get() + "/" + scale.get() + "/" + width.get();
		if (want.equals(applied)) return;
		applied = want;
		game().setChatLook(opacity.get() / 100.0, scale.get() / 100.0, width.get() / 100.0);
	}
}
