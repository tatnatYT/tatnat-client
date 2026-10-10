package com.tatnat.client.mc;

import com.tatnat.client.modules.impl.utility.TierTagger;

/** Tier Tagger: the coloured "HT3 | " in front of a name. */
public final class TierText {
	private TierText() {
	}

	public static net.minecraft.network.chat.Component prefix(TierTagger.Tag t, net.minecraft.network.chat.Component name) {
		return new net.minecraft.network.chat.TextComponent(t.legacyIcon() + t.legacy()).append(name);
	}
}
