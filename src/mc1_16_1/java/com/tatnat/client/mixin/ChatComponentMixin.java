package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.event.Events;
import com.tatnat.client.mc.PlayerLooks;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

/** Every chat line passes through here: Auto GG reads it, Nick Hider rewrites your name in it. */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;I)V", at = @At("HEAD"), argsOnly = true)
	private Component tatnat$chat(Component message, Component m, int id) {
		TatnatClient.EVENTS.post(new Events.Chat(message.getString()));
		if (NickHider.active() && NickHider.INSTANCE.inChat.on()) return PlayerLooks.replaceName(message);
		return message;
	}
}
