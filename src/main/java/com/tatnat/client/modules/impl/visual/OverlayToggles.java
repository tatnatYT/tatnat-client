package com.tatnat.client.modules.impl.visual;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.ui.render.Icons;

/**
 * Boss Bar, Toast Control and Subtitles: each hides one vanilla overlay while it's on. The mixins
 * (mixin/OverlayMixins, 1.14+) ask the static checks below.
 */
public final class OverlayToggles {
	private OverlayToggles() {
	}

	static BossBar boss;
	static ToastControl toasts;
	static Subtitles subtitles;

	public static boolean hideBossBar() {
		return boss != null && boss.isEnabled();
	}

	public static boolean hideToasts() {
		return toasts != null && toasts.isEnabled();
	}

	public static boolean hideSubtitles() {
		return subtitles != null && subtitles.isEnabled();
	}

	/** Hides the boss health bar (Wither, Ender Dragon, server bars) to clean up the top of the screen. */
	public static class BossBar extends Module {
		public BossBar() {
			super("Boss Bar", "Hides the boss health bar at the top of the screen", Category.VISUAL, false);
			icon = Icons.Icon.HEART;
			boss = this;
		}
	}

	/** No more advancement, recipe and tutorial pop-ups sliding in. */
	public static class ToastControl extends Module {
		public ToastControl() {
			super("Toast Control", "Hides the advancement, recipe and tutorial pop-ups", Category.VISUAL, false);
			icon = Icons.Icon.CHAT;
			toasts = this;
		}
	}

	/** Hides the subtitle captions ("Creeper hisses") even when they're on in the options. */
	public static class Subtitles extends Module {
		public Subtitles() {
			super("Subtitles", "Hides the sound subtitles in the corner", Category.VISUAL, false);
			icon = Icons.Icon.TOOLTIP;
			subtitles = this;
		}
	}
}
