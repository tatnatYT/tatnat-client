package com.tatnat.client.mc;

import com.tatnat.client.modules.impl.utility.TierTagger;

/** Tier Tagger: the coloured "HT3 | " in front of a name. */
public final class TierText {
	private TierText() {
	}

	public static net.minecraft.network.chat.Component prefix(TierTagger.Tag t, net.minecraft.network.chat.Component name) {
		// The icon stays white: the font tints glyphs with the text colour.
		return net.minecraft.network.chat.Component.literal(t.icon.isEmpty() ? "" : t.icon + " ").withStyle(net.minecraft.ChatFormatting.WHITE)
				.append(net.minecraft.network.chat.Component.literal(t.text).withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(t.rgb))))
				.append(net.minecraft.network.chat.Component.literal(" | ").withStyle(net.minecraft.ChatFormatting.GRAY)).append(name);
	}
}
