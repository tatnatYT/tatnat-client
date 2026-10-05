package com.tatnat.client.event;

import com.tatnat.client.platform.Gfx;

/** All shared event types, grouped in one file because each is only a couple of fields. */
public final class Events {
	private Events() {
	}

	/** Fired at the end of every client tick (20 per second), in and out of worlds. */
	public static final class Tick extends Event {
		public static final Tick INSTANCE = new Tick();
	}

	/** Fired after the vanilla HUD has drawn, in GUI-scaled coordinates. */
	public static final class Render2D extends Event {
		public final Gfx gfx;
		public final float partialTick;

		public Render2D(Gfx gfx, float partialTick) {
			this.gfx = gfx;
			this.partialTick = partialTick;
		}
	}

	/** A raw mouse button press or release. Cancelling it hides it from the game. */
	public static final class MouseButton extends Event {
		public final int button;
		/** 1 = press, 0 = release. */
		public final int action;
		/** True when no screen is open, i.e. the click goes to the world. */
		public final boolean inGame;

		public MouseButton(int button, int action, boolean inGame) {
			this.button = button;
			this.action = action;
			this.inGame = inGame;
		}
	}

	/** A raw key press, release or repeat (GLFW codes). Cancelling it hides it from the game. */
	public static final class Key extends Event {
		public final int key;
		/** 1 = press, 0 = release, 2 = repeat. */
		public final int action;
		public final boolean inGame;

		public Key(int key, int action, boolean inGame) {
			this.key = key;
			this.action = action;
			this.inGame = inGame;
		}
	}

	/** Mouse wheel in game. Cancelling it stops the hotbar from scrolling. */
	public static final class Scroll extends Event {
		public final double amount;

		public Scroll(double amount) {
			this.amount = amount;
		}
	}

	/** You swung at an entity (opaque platform handle, see {@code Game}). */
	public static final class Attack extends Event {
		public final Object target;

		public Attack(Object target) {
			this.target = target;
		}
	}

	/** A chat line arrived; {@link #text} is its plain text. */
	public static final class Chat extends Event {
		public final String text;

		public Chat(String text) {
			this.text = text;
		}
	}
}
