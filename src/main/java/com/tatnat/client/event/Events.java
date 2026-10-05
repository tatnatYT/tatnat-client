package com.tatnat.client.event;

import net.minecraft.client.gui.GuiGraphics;

/** All event types, grouped in one file because each is only a couple of fields. */
public final class Events {
	private Events() {
	}

	/** Fired at the end of every client tick (20 per second), in and out of worlds. */
	public static final class Tick extends Event {
		public static final Tick INSTANCE = new Tick();
	}

	/** Fired after the vanilla HUD has drawn, in GUI-scaled coordinates. */
	public static final class Render2D extends Event {
		public final GuiGraphics graphics;
		public final float partialTick;

		public Render2D(GuiGraphics graphics, float partialTick) {
			this.graphics = graphics;
			this.partialTick = partialTick;
		}
	}

	/** A raw mouse button press or release. Cancelling it hides it from the game. */
	public static final class MouseButton extends Event {
		public final int button;
		/** GLFW action: 1 = press, 0 = release. */
		public final int action;
		/** True when no screen is open, i.e. the click goes to the world. */
		public final boolean inGame;

		public MouseButton(int button, int action, boolean inGame) {
			this.button = button;
			this.action = action;
			this.inGame = inGame;
		}
	}

	/** A raw key press, release or repeat. Cancelling it hides it from the game. */
	public static final class Key extends Event {
		public final int key;
		/** GLFW action: 1 = press, 0 = release, 2 = repeat. */
		public final int action;
		public final boolean inGame;

		public Key(int key, int action, boolean inGame) {
			this.key = key;
			this.action = action;
			this.inGame = inGame;
		}
	}

	/** You swung at an entity (fired before the attack packet is sent). */
	public static final class Attack extends Event {
		public final net.minecraft.world.entity.Entity target;

		public Attack(net.minecraft.world.entity.Entity target) {
			this.target = target;
		}
	}

	/**
	 * The frame's 3D debug-shape pass: anything added with {@code net.minecraft.gizmos.Gizmos}
	 * here (boxes, lines, floating text) is drawn in the world this frame.
	 */
	public static final class Gizmos extends Event {
		public final net.minecraft.client.renderer.culling.Frustum frustum;
		public final double camX, camY, camZ;
		public final float partialTick;

		public Gizmos(net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ, float partialTick) {
			this.frustum = frustum;
			this.camX = camX;
			this.camY = camY;
			this.camZ = camZ;
			this.partialTick = partialTick;
		}
	}

	/** A chat line arrived (system or player). {@link #message} may be replaced. */
	public static final class Chat extends Event {
		public net.minecraft.network.chat.Component message;

		public Chat(net.minecraft.network.chat.Component message) {
			this.message = message;
		}
	}

	/** Mouse wheel. Cancelling it stops the hotbar from scrolling. */
	public static final class Scroll extends Event {
		public final double amount;

		public Scroll(double amount) {
			this.amount = amount;
		}
	}
}
