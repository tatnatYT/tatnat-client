package com.tatnat.client.mc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.impl.cosmetic.PlusCapes;
import com.tatnat.client.plus.CapeFrames;
import com.tatnat.client.plus.EclipseNet;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

/** Nick Hider and Eclipse Plus capes for the legacy versions. */
public final class PlayerLooks {
	private PlayerLooks() {
	}

	
	public static boolean isMe(UUID id) {
		MinecraftClient mc = MinecraftClient.getInstance();
		return mc.player != null && mc.player.getUuid().equals(id) || mc.getSession().getProfile().getId().equals(id);
	}

	private static boolean hideSkin() {
		return NickHider.active() && NickHider.INSTANCE.hideSkin.on();
	}

	public static Identifier skin(Identifier original) {
		if (!hideSkin()) return original;
		boolean alex = NickHider.INSTANCE.skin.is("Alex");
		return new Identifier("minecraft", alex ? "textures/entity/alex.png" : "textures/entity/steve.png");
	}

	public static String model(String original) {
		if (!hideSkin()) return original;
		return NickHider.INSTANCE.skin.is("Alex") ? "slim" : "default";
	}

	/** A player's Eclipse Plus cape (also used for the elytra), if they have one. */
	public static Identifier cape(Identifier original, UUID id, boolean me) {
		Identifier c = plusCape(EclipseNet.cape(id, me));
		return c != null ? c : original;
	}


	/** One live texture per Plus cape, updated in place with each new frame. */
	private static final class PlusTexture {
		final Identifier id;
		final NativeImageBackedTexture tex = new NativeImageBackedTexture(PlusCapes.W, PlusCapes.H);
		int revision = -1;
		boolean full;

		PlusTexture(String cape) {
			id = new Identifier(TatnatClient.ID, "cape/plus_" + cape);
			MinecraftClient.getInstance().getTextureManager().loadTexture(id, tex);
		}
	}

	private static final java.util.Map<String, PlusTexture> plusTextures = new java.util.HashMap<>();

	/** The texture of Plus cape {@code cape} (null for none, or before its first frame is ready). */
	private static Identifier plusCape(String cape) {
		CapeFrames.Frame frame = CapeFrames.get(cape);
		if (frame == null) return null;
		PlusTexture t = plusTextures.computeIfAbsent(cape, PlusTexture::new);
		if (t.revision != frame.revision) {
			int[] dst = t.tex.getPixels(), src = frame.pixels;
			// Only the faces change between frames; everything is copied the first time.
			if (!t.full) System.arraycopy(src, 0, dst, 0, src.length);
			else for (int y = PlusCapes.DIRTY_Y0; y < PlusCapes.DIRTY_Y1; y++) System.arraycopy(src, y * PlusCapes.W, dst, y * PlusCapes.W, PlusCapes.DIRTY_X1);
			t.tex.upload();
			t.full = true;
			t.revision = frame.revision;
		}
		return t.id;
	}

	private static String realName() {
		return MinecraftClient.getInstance().getSession().getUsername();
	}

	/** Name tags and the tab list are plain strings on these versions. */
	public static String replaceName(String s) {
		String real = realName();
		if (s == null || !NickHider.active() || real == null || real.isEmpty() || !s.contains(real)) return s;
		return s.replace(real, NickHider.INSTANCE.nick.get());
	}

	/** Copy of {@code c} with every occurrence of your real name replaced (styles kept). */
	public static Text replaceName(Text c) {
		String real = realName();
		if (!NickHider.active() || real == null || real.isEmpty() || !c.asUnformattedString().contains(real)) return c;
		return rebuild(c, real, NickHider.INSTANCE.nick.get());
	}

	private static Text rebuild(Text c, String from, String to) {
		Text out;
		if (c instanceof LiteralText) {
			out = new LiteralText(((LiteralText) c).getRawString().replace(from, to));
		} else if (c instanceof TranslatableText) {
			TranslatableText tr = (TranslatableText) c;
			Object[] args = tr.getArgs().clone();
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Text) args[i] = rebuild((Text) args[i], from, to);
				else if (args[i] instanceof String) args[i] = ((String) args[i]).replace(from, to);
			}
			out = new TranslatableText(tr.getKey(), args);
		} else {
			out = new LiteralText(c.computeValue().replace(from, to));
		}
		out.setStyle(c.getStyle().deepCopy());
		List<Text> siblings = new ArrayList<>(c.getSiblings());
		for (Text s : siblings) out.append(rebuild(s, from, to));
		return out;
	}
}
