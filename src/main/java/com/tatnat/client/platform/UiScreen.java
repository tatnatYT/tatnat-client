package com.tatnat.client.platform;

/**
 * A full-screen menu written once in shared code (the mod menu, the HUD editor). Each platform
 * wraps it in that version's Screen class and forwards drawing and input. All coordinates are
 * real window pixels.
 */
public interface UiScreen {
	/** How the world behind should look. */
	enum Backdrop {
		/** World fully visible (Feather style). */
		CLEAR,
		/** World slightly darkened. */
		DIM
	}

	Backdrop backdrop();

	boolean pausesGame();

	/** Draw a frame. {@code g} is in GUI-scaled space; switch to pixels as needed. */
	void render(Gfx g, double mouseX, double mouseY);

	/** Called right after {@link #render} for drawing that must sit on top (HUD editor overlays). */
	default void renderOverlay(Gfx g, double mouseX, double mouseY) {
	}

	boolean mouseClicked(double x, double y, int button);

	void mouseReleased(double x, double y, int button);

	void mouseDragged(double x, double y, int button);

	void mouseScrolled(double x, double y, double amount);

	/** @param key GLFW key code; @param shift whether Shift is held. Return true if handled. */
	boolean keyPressed(int key, boolean shift);

	boolean charTyped(String chars);

	/** The screen was closed (by us or the game). */
	void removed();

	/** Asked to close (Escape is routed through {@link #keyPressed} first). */
	default boolean closesOnEscape() {
		return false;
	}
}
