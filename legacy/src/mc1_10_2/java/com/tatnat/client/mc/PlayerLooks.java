package com.tatnat.client.mc;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.modules.impl.cosmetic.CapeArt;
import com.tatnat.client.modules.impl.cosmetic.CustomCapes;
import com.tatnat.client.modules.impl.visual.NickHider;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

/** Nick Hider and Custom Capes for the legacy versions. */
public final class PlayerLooks {
	private PlayerLooks() {
	}

	private static final Identifier CAPE = new Identifier(TatnatClient.ID, "cape/current");
	private static int capeRevision = -1;
	private static Identifier capeTexture;

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

	/** Your cape when Custom Capes is on. */
	public static Identifier cape(Identifier original) {
		if (!CustomCapes.active()) return original;
		Identifier c = capeTexture();
		return c != null ? c : original;
	}

	private static Identifier capeTexture() {
		CustomCapes c = CustomCapes.INSTANCE;
		int rev = c.revision();
		if (rev == capeRevision) return capeTexture;
		capeRevision = rev;
		int[] px = c.pixels();
		if (px == null) {
			capeTexture = null;
			return null;
		}
		BufferedImage img = new BufferedImage(CapeArt.W, CapeArt.H, BufferedImage.TYPE_INT_ARGB);
		img.setRGB(0, 0, CapeArt.W, CapeArt.H, px, 0, CapeArt.W);
		MinecraftClient.getInstance().getTextureManager().loadTexture(CAPE, new NativeImageBackedTexture(img));
		capeTexture = CAPE;
		return capeTexture;
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
