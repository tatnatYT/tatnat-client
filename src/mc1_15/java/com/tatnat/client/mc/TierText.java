package com.tatnat.client.mc;

import com.tatnat.client.modules.impl.utility.TierTagger;

/** Tier Tagger: the coloured "HT3 | " in front of a name. */
public final class TierText {
	private TierText() {
	}

	public static net.minecraft.network.chat.Component prefix(TierTagger.Tag t, net.minecraft.network.chat.Component name) {
		return new net.minecraft.network.chat.TextComponent(t.legacyIcon() + t.legacy()).append(name);
	}

	/** The Eclipse badge in front of a name: a crescent in the name's colour, or the purple Plus badge. */
	public static net.minecraft.network.chat.Component badge(int kind, net.minecraft.network.chat.Component name) {
		if (kind == com.tatnat.client.plus.EclipseNet.NONE || name == null) return name;
		if (kind == com.tatnat.client.plus.EclipseNet.PLUS) return new net.minecraft.network.chat.TextComponent("\u00a7f" + com.tatnat.client.plus.EclipseBadge.PLUS + " ").append(name);
		net.minecraft.network.chat.Component icon = new net.minecraft.network.chat.TextComponent(com.tatnat.client.plus.EclipseBadge.PLAIN + " ");
		net.minecraft.ChatFormatting colour = name.getStyle().getColor();
		for (net.minecraft.network.chat.Component s : name.getSiblings()) if (colour == null) colour = s.getStyle().getColor();
		if (colour != null) icon.getStyle().setColor(colour);
		return icon.append(name);
	}
}
