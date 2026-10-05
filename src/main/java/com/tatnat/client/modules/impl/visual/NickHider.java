package com.tatnat.client.modules.impl.visual;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.TextSetting;
import com.tatnat.client.ui.render.Icons;

import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Hides your name and skin on your own screen (for recording / streaming). Only your client
 * changes; everyone else still sees the real you. Hooks: chat ({@code ChatComponentMixin}), tab
 * list, name tags and skins ({@code PlayerSkinMixins}).
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

	public boolean isMe(UUID id) {
		return mc.player != null && mc.player.getUUID().equals(id) || mc.getUser().getProfileId().equals(id);
	}

	public String realName() {
		return mc.getUser().getName();
	}

	public PlayerSkin replacementSkin() {
		boolean alex = skin.is("Alex");
		String path = alex ? "entity/player/slim/alex" : "entity/player/wide/steve";
		Identifier id = Identifier.withDefaultNamespace(path);
		return PlayerSkin.insecure(new ClientAsset.ResourceTexture(id, Identifier.withDefaultNamespace("textures/" + path + ".png")),
				null, null, alex ? PlayerModelType.SLIM : PlayerModelType.WIDE);
	}

	/** Copy of {@code c} with every occurrence of your real name replaced (styles kept). */
	public Component replace(Component c) {
		String real = realName();
		if (real == null || real.isEmpty() || !c.getString().contains(real)) return c;
		return rebuild(c, real, nick.get());
	}

	private static Component rebuild(Component c, String from, String to) {
		MutableComponent out;
		if (c.getContents() instanceof PlainTextContents.LiteralContents lit) {
			out = Component.literal(lit.text().replace(from, to));
		} else if (c.getContents() instanceof TranslatableContents tr) {
			Object[] args = tr.getArgs().clone();
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Component ac) args[i] = rebuild(ac, from, to);
				else if (args[i] instanceof String s) args[i] = s.replace(from, to);
			}
			out = Component.translatableWithFallback(tr.getKey(), tr.getFallback(), args);
		} else {
			out = c.plainCopy();
		}
		out.setStyle(c.getStyle());
		List<Component> siblings = new ArrayList<>(c.getSiblings());
		for (Component s : siblings) out.append(rebuild(s, from, to));
		return out;
	}
}
