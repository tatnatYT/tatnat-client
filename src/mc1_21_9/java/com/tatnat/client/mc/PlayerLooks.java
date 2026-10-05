package com.tatnat.client.mc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.impl.cosmetic.CapeArt;
import com.tatnat.client.modules.impl.cosmetic.CustomCapes;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/** Nick Hider and Custom Capes for 1.21.11: text replacement in components, skins and capes. */
public final class PlayerLooks {
	private PlayerLooks() {
	}

	private static final ResourceLocation CAPE = ResourceLocation.fromNamespaceAndPath(TatnatClient.ID, "cape/current");
	private static int capeRevision = -1;
	private static ClientAsset.Texture capeAsset;

	public static boolean isMe(UUID id) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.getUUID().equals(id) || mc.getUser().getProfileId().equals(id);
	}

	/** Your skin as you see it: default skin (Nick Hider) and/or your chosen cape. */
	public static PlayerSkin apply(PlayerSkin skin) {
		if (NickHider.active() && NickHider.INSTANCE.hideSkin.on()) skin = replacementSkin();
		if (CustomCapes.active()) {
			ClientAsset.Texture cape = cape();
			if (cape != null) skin = new PlayerSkin(skin.body(), cape, cape, skin.model(), skin.secure());
		}
		return skin;
	}

	public static PlayerSkin replacementSkin() {
		boolean alex = NickHider.INSTANCE.skin.is("Alex");
		String path = alex ? "entity/player/slim/alex" : "entity/player/wide/steve";
		ResourceLocation id = ResourceLocation.withDefaultNamespace(path);
		return PlayerSkin.insecure(new ClientAsset.ResourceTexture(id, ResourceLocation.withDefaultNamespace("textures/" + path + ".png")),
				null, null, alex ? PlayerModelType.SLIM : PlayerModelType.WIDE);
	}

	/** The cape texture, re-uploaded whenever the chosen design changes. */
	private static ClientAsset.Texture cape() {
		CustomCapes c = CustomCapes.INSTANCE;
		int rev = c.revision();
		if (rev == capeRevision) return capeAsset;
		capeRevision = rev;
		int[] px = c.pixels();
		if (px == null) {
			capeAsset = null;
			return null;
		}
		NativeImage img = new NativeImage(CapeArt.W, CapeArt.H, true);
		for (int y = 0; y < CapeArt.H; y++) for (int x = 0; x < CapeArt.W; x++) img.setPixel(x, y, px[y * CapeArt.W + x]);
		Minecraft.getInstance().getTextureManager().register(CAPE, new DynamicTexture(() -> "tatnat cape", img));
		capeAsset = new ClientAsset.ResourceTexture(CAPE, CAPE);
		return capeAsset;
	}

	/** Copy of {@code c} with every occurrence of your real name replaced (styles kept). */
	public static Component replaceName(Component c) {
		String real = Minecraft.getInstance().getUser().getName();
		if (!NickHider.active() || real == null || real.isEmpty() || !c.getString().contains(real)) return c;
		return rebuild(c, real, NickHider.INSTANCE.nick.get());
	}

	private static Component rebuild(Component c, String from, String to) {
		MutableComponent out;
		if (c.getContents() instanceof PlainTextContents.LiteralContents) {
			out = Component.literal(((PlainTextContents.LiteralContents) c.getContents()).text().replace(from, to));
		} else if (c.getContents() instanceof TranslatableContents) {
			TranslatableContents tr = (TranslatableContents) c.getContents();
			Object[] args = tr.getArgs().clone();
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Component) args[i] = rebuild((Component) args[i], from, to);
				else if (args[i] instanceof String) args[i] = ((String) args[i]).replace(from, to);
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
