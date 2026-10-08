package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;

/**
 * Motion Blur and Color Saturation, built on the game's own post-processing shaders (1.15 - 1.20.4):
 * "phosphor" leaves a fading trail behind movement, "desaturate" drains the colour out of the world.
 * One shader runs at a time, so turning one on turns the other off.
 */
public final class PostEffects {
	private PostEffects() {
	}

	private static String loaded;

	/** Keeps the right shader loaded: the most recently enabled effect wins. */
	static void sync(String wanted) {
		if (wanted == null ? loaded == null : wanted.equals(loaded)) return;
		loaded = wanted;
		com.tatnat.client.TatnatClient.game().postEffect(wanted);
	}

	abstract static class Effect extends Module {
		private final String shader;

		Effect(String name, String description, String shader, com.tatnat.client.ui.render.Icons.Icon icon) {
			super(name, description, Category.VISUAL, false);
			this.shader = shader;
			this.icon = icon;
		}

		@Override
		protected void onEnable() {
			if (game().inWorld()) sync(shader); // the last one switched on wins
		}

		@Subscribe
		public void onTick(Events.Tick e) {
			// Enabled before joining a world: load it once you're in.
			if (loaded == null && game().inWorld()) sync(shader);
		}

		@Override
		protected void onDisable() {
			if (shader.equals(loaded)) sync(null);
		}
	}

	/** Fast camera movements leave a fading trail (the vanilla "phosphor" shader). */
	public static class MotionBlur extends Effect {
		public MotionBlur() {
			super("Motion Blur", "Fast movement leaves a fading trail (1.15 - 1.20.4)", "phosphor", com.tatnat.client.ui.render.Icons.Icon.RUN);
		}
	}

	/** Greyscale world (the vanilla "desaturate" shader). */
	public static class ColorSaturation extends Effect {
		public ColorSaturation() {
			super("Color Saturation", "Drains the colour from the world (1.15 - 1.20.4)", "desaturate", com.tatnat.client.ui.render.Icons.Icon.DROP);
		}
	}
}
