package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Hides your name and skin on your own screen (for recording / streaming). Only your client
 * changes; everyone else still sees the real you. The platform layer applies these settings to
 * chat, the tab list, name tags and skins.
 */
public class NickHider extends Module {
	public static NickHider INSTANCE;

	public final TextSetting nick = add(new TextSetting("Name", "What your name shows as", "You", 16));
	public final BooleanSetting inChat = add(new BooleanSetting("Hide in Chat", "Replace your name in chat messages", true));
	public final BooleanSetting inTab = add(new BooleanSetting("Hide in Tab List", "Replace your name in the player list", true));
	public final BooleanSetting hideSkin = add(new BooleanSetting("Hide Skin", "Show a default skin instead of yours", true));
	public final ModeSetting skin = add(new ModeSetting("Default Skin", "Which default skin to show", "Steve", "Steve", "Alex"));

	public NickHider() {
		super("Nick Hider", "Hide your name and skin on your own screen", Category.VISUAL, false);
		icon = Icons.Icon.MASK;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	/** {@code text} with your real name replaced, or unchanged when off. */
	public String replace(String text) {
		String real = game().playerName();
		if (!isEnabled() || real == null || real.isEmpty() || text == null) return text;
		return text.replace(real, nick.get());
	}
}
