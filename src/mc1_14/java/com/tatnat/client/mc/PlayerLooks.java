package com.tatnat.client.mc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.impl.cosmetic.CapeArt;
import com.tatnat.client.modules.impl.cosmetic.CustomCapes;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Nick Hider and Custom Capes for 1.20 - 1.20.1, where a player's look is a set of separate
 * texture getters (skin, cape, elytra, model) rather than one skin record.
 */
public final class PlayerLooks {
	private PlayerLooks() {
	}

	private static final ResourceLocation CAPE = new ResourceLocation(TatnatClient.ID, "cape/current");
	private static int capeRevision = -1;
	private static ResourceLocation capeTexture;

	public static boolean isMe(UUID id) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.getUUID().equals(id) || mc.getUser().getGameProfile().getId().equals(id);
	}

	private static boolean hideSkin() {
		return NickHider.active() && NickHider.INSTANCE.hideSkin.on();
	}

	public static ResourceLocation skin(ResourceLocation original) {
		if (!hideSkin()) return original;
		boolean alex = NickHider.INSTANCE.skin.is("Alex");
		return new ResourceLocation("minecraft", alex ? "textures/entity/alex.png" : "textures/entity/steve.png");
	}

	public static String model(String original) {
		if (!hideSkin()) return original;
		return NickHider.INSTANCE.skin.is("Alex") ? "slim" : "default";
	}

	/** Your cape (also used for the elytra) when Custom Capes is on. */
	public static ResourceLocation cape(ResourceLocation original) {
		if (!CustomCapes.active()) return original;
		ResourceLocation c = capeTexture();
		return c != null ? c : original;
	}

	private static ResourceLocation capeTexture() {
		CustomCapes c = CustomCapes.INSTANCE;
		int rev = c.revision();
		if (rev == capeRevision) return capeTexture;
		capeRevision = rev;
		int[] px = c.pixels();
		if (px == null) {
			capeTexture = null;
			return null;
		}
		NativeImage img = new NativeImage(CapeArt.W, CapeArt.H, true);
		for (int y = 0; y < CapeArt.H; y++) for (int x = 0; x < CapeArt.W; x++) img.setPixelRGBA(x, y, FeaturesImpl.toAbgr(px[y * CapeArt.W + x]));
		Minecraft.getInstance().getTextureManager().register(CAPE, new DynamicTexture(img));
		capeTexture = CAPE;
		return capeTexture;
	}

	/** Name tags are plain strings on this version. */
	public static String replaceName(String s) {
		String real = Minecraft.getInstance().getUser().getName();
		if (!NickHider.active() || real == null || real.isEmpty() || !s.contains(real)) return s;
		return s.replace(real, NickHider.INSTANCE.nick.get());
	}

	/** Copy of {@code c} with every occurrence of your real name replaced (styles kept). */
	public static Component replaceName(Component c) {
		String real = Minecraft.getInstance().getUser().getName();
		if (!NickHider.active() || real == null || real.isEmpty() || !c.getString().contains(real)) return c;
		return rebuild(c, real, NickHider.INSTANCE.nick.get());
	}

	private static Component rebuild(Component c, String from, String to) {
		Component out;
		if (c instanceof TextComponent) {
			out = new TextComponent(((TextComponent) c).getText().replace(from, to));
		} else if (c instanceof TranslatableComponent) {
			TranslatableComponent tr = (TranslatableComponent) c;
			Object[] args = tr.getArgs().clone();
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Component) args[i] = rebuild((Component) args[i], from, to);
				else if (args[i] instanceof String) args[i] = ((String) args[i]).replace(from, to);
			}
			out = new TranslatableComponent(tr.getKey(), args);
		} else {
			out = c.copy();
		}
		out.setStyle(c.getStyle().copy());
		List<Component> siblings = new ArrayList<>(c.getSiblings());
		for (Component s : siblings) out.append(rebuild(s, from, to));
		return out;
	}
}
