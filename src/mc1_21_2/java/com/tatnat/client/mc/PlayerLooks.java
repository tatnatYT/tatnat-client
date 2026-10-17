package com.tatnat.client.mc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;
import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.impl.cosmetic.PlusCapes;
import com.tatnat.client.plus.CapeFrames;
import com.tatnat.client.plus.EclipseNet;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.PlayerSkin;

/** Nick Hider and Eclipse Plus capes for 1.21.11: text replacement in components, skins and capes. */
public final class PlayerLooks {
	private PlayerLooks() {
	}

	
	/** Minecraft's own elytra, so Plus capes don't show up on it. */
	private static final ResourceLocation DEFAULT_ELYTRA = ResourceLocation.withDefaultNamespace("textures/entity/equipment/wings/elytra.png");

	public static boolean isMe(UUID id) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.getUUID().equals(id) || mc.getUser().getProfileId().equals(id);
	}

	/** A player's skin as you see it: the default skin for yourself (Nick Hider), and their Eclipse Plus cape (not on the elytra). */
	public static PlayerSkin apply(PlayerSkin skin, UUID id, boolean me) {
		if (me && NickHider.active() && NickHider.INSTANCE.hideSkin.on()) skin = replacementSkin();
		ResourceLocation cape = plusCape(EclipseNet.cape(id, me));
		if (cape != null) skin = new PlayerSkin(skin.texture(), skin.textureUrl(), cape, skin.elytraTexture() != null ? skin.elytraTexture() : DEFAULT_ELYTRA, skin.model(), skin.secure());
		return skin;
	}

	public static PlayerSkin replacementSkin() {
		boolean alex = NickHider.INSTANCE.skin.is("Alex");
		String path = alex ? "entity/player/slim/alex" : "entity/player/wide/steve";
		return new PlayerSkin(ResourceLocation.withDefaultNamespace("textures/" + path + ".png"), null, null, null,
				alex ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, false);
	}


	/** One live texture per Plus cape, updated in place with each new frame. */
	private static final class PlusTexture {
		final ResourceLocation id;
		final NativeImage img = new NativeImage(PlusCapes.W, PlusCapes.H, true);
		final DynamicTexture tex;
		final ResourceLocation handle;
		int revision = -1;
		boolean full;

		PlusTexture(String cape) {
			id = ResourceLocation.fromNamespaceAndPath(TatnatClient.ID, "cape/plus_" + cape);
			NativeImage img = this.img;
			tex = new DynamicTexture(img);
			Minecraft.getInstance().getTextureManager().register(id, tex);
			handle = id;
		}
	}

	private static final java.util.Map<String, PlusTexture> plusTextures = new java.util.HashMap<>();

	/** The texture of Plus cape {@code cape} (null for none, or before its first frame is ready). */
	private static ResourceLocation plusCape(String cape) {
		CapeFrames.Frame frame = CapeFrames.get(cape);
		if (frame == null) return null;
		PlusTexture t = plusTextures.computeIfAbsent(cape, PlusTexture::new);
		if (t.revision != frame.revision) {
			NativeImage img = t.img;
			int[] px = frame.pixels;
			// Only the faces change between frames; everything is copied the first time.
			// Only the faces change between frames; everything is copied the first time.
			boolean wings = frame.revision % 3 == 0;
			int y0 = !t.full ? 0 : wings ? 0 : PlusCapes.DIRTY_Y0;
			int y1 = !t.full ? PlusCapes.H : wings ? PlusCapes.WINGS_Y1 : PlusCapes.DIRTY_Y1;
			int x1 = !t.full ? PlusCapes.W : wings ? PlusCapes.WINGS_X1 : PlusCapes.DIRTY_X1;
			for (int y = y0; y < y1; y++) {
				for (int x = 0; x < x1; x++) {
					int c = px[y * PlusCapes.W + x];
					img.setPixel(x, y, c);
				}
			}
			t.tex.upload();
			t.full = true;
			t.revision = frame.revision;
		}
		return t.handle;
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
