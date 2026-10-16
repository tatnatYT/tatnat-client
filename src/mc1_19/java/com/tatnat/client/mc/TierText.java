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

	/** The Eclipse badge in front of a name: a crescent in the name's colour, or the purple Plus badge (its own colours). */
	public static net.minecraft.network.chat.Component badge(int kind, net.minecraft.network.chat.Component name) {
		if (kind == com.tatnat.client.plus.EclipseNet.NONE || name == null) return name;
		if (kind == com.tatnat.client.plus.EclipseNet.PLUS) return net.minecraft.network.chat.Component.literal(com.tatnat.client.plus.EclipseBadge.PLUS + " ").withStyle(net.minecraft.ChatFormatting.WHITE).append(name);
		net.minecraft.network.chat.TextColor colour = name.getStyle().getColor();
		for (net.minecraft.network.chat.Component s : name.getSiblings()) if (colour == null) colour = s.getStyle().getColor();
		final net.minecraft.network.chat.TextColor c = colour;
		return net.minecraft.network.chat.Component.literal(com.tatnat.client.plus.EclipseBadge.PLAIN + " ").withStyle(s -> c == null ? s : s.withColor(c)).append(name);
	}
}
