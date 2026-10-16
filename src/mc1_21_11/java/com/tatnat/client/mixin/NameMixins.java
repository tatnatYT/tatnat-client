package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tatnat.client.mc.PlayerLooks;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/** Tab list names: Nick Hider, friend highlights, Tier Tagger and the Eclipse badge. */
@Mixin(PlayerTabOverlay.class)
public class NameMixins {
	/**
	 * Names in the tab list, in one hook so every change applies: Nick Hider for your own entry, friends in
	 * your colour (Tablist), Tier Tagger in front, and the Eclipse badge in front of everything.
	 */
	@com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
	private Component tatnat$tabName(Component name, PlayerInfo info) {
		if (name == null) return null;
		boolean me = PlayerLooks.isMe(info.getProfile().id());
		if (me && NickHider.active() && NickHider.INSTANCE.inTab.on()) name = PlayerLooks.replaceName(name);
		String friend = com.tatnat.client.modules.impl.visual.Tablist.highlight(name.getString());
		if (friend != null) name = net.minecraft.network.chat.Component.literal(friend);
		com.tatnat.client.modules.impl.utility.TierTagger.Tag tier = com.tatnat.client.modules.impl.utility.TierTagger.tabTag(info.getProfile().id(), info.getProfile().name());
		if (tier != null) name = com.tatnat.client.mc.TierText.prefix(tier, name);
		return com.tatnat.client.mc.TierText.badge(com.tatnat.client.plus.EclipseNet.badge(info.getProfile().id(), me), name);
	}
}
