package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Motion Blur and Color Saturation: the mod's own post-processing shaders (1.15 - 1.21.1 and 1.21.6+).
 * Motion blur blends each frame into the blended history, like Natural Motion Blur's frame blending:
 * a still view stays sharp and turning leaves a smooth trail; menus and the HUD are never blurred.
 * One effect runs at a time; the last one switched on wins.
 */
public final class PostEffects {
	private PostEffects() {
	}

	/** The effect currently loaded ("motion_blur_50", "saturation_0"...), or null. */
	private static String loaded;

	static void sync(String wanted) {
		if (wanted == null ? loaded == null : wanted.equals(loaded)) return;
		loaded = wanted;
		com.tatnat.client.TatnatClient.game().postEffect(wanted);
	}

	abstract static class Effect extends Module {
		private String world = "";
		private boolean wasDead;

		Effect(String name, String description, Icons.Icon icon) {
			super(name, description, Category.VISUAL, false);
			this.icon = icon;
		}

		/** The shader to load for the current settings. */
		abstract String shader();

		abstract String prefix();

		@Override
		protected void onEnable() {
			loaded = null; // the last one switched on wins
			if (game().inWorld()) sync(shader());
		}

		@Subscribe
		public void onTick(Events.Tick e) {
			if (!game().inWorld()) return;
			// The game drops post effects on a new world or a respawn: load it again then.
			String w = game().worldKey();
			boolean dead = game().health() <= 0;
			if (!w.equals(world) || wasDead && !dead) loaded = null;
			world = w;
			wasDead = dead;
			if (loaded == null || loaded.startsWith(prefix())) sync(shader());
		}

		@Override
		protected void onDisable() {
			if (loaded != null && loaded.startsWith(prefix())) sync(null);
		}
	}

	/** Smooth trails when you move or turn. */
	public static class MotionBlur extends Effect {
		private final ModeSetting strength = add(new ModeSetting("Strength", "How long the trail lasts", "Medium", "Low", "Medium", "High", "Very High"));

		public MotionBlur() {
			super("Motion Blur", "Smooth blur when you move or turn (1.15 - 1.21.1, 1.21.6+)", Icons.Icon.SPEEDLINES);
		}

		@Override
		String prefix() {
			return "motion_blur_";
		}

		@Override
		String shader() {
			return prefix() + (strength.is("Low") ? 70 : strength.is("High") ? 88 : strength.is("Very High") ? 94 : 80);
		}
	}

	/** Colour saturation of the whole world, from greyscale to extra vivid. */
	public static class ColorSaturation extends Effect {
		private final ModeSetting level = add(new ModeSetting("Saturation", "0% greyscale up to 200% extra vivid", "150%", "0%", "50%", "150%", "200%"));

		public ColorSaturation() {
			super("Color Saturation", "Make colours more vivid or greyscale (1.15 - 1.21.1, 1.21.6+)", Icons.Icon.WHEEL);
		}

		@Override
		String prefix() {
			return "saturation_";
		}

		@Override
		String shader() {
			return prefix() + level.get().replace("%", "");
		}
	}
}
