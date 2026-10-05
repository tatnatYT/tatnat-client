package com.tatnat.client.platform;

import java.nio.file.Path;
import java.util.List;

/**
 * Everything the shared code needs to know about (or do to) the running game. Each Minecraft
 * version's platform layer implements this; shared code never touches Minecraft classes.
 *
 * Key codes are always GLFW codes (platforms on older versions translate theirs).
 */
public interface Game {
	// ------------------------------------------------------------ window / state

	int windowWidth();

	int windowHeight();

	int guiScale();

	int guiWidth();

	int guiHeight();

	/** Mouse position in real window pixels. */
	double mouseX();

	double mouseY();

	boolean inWorld();

	/** True when any screen (menu, inventory, chat...) is open. */
	boolean screenOpen();

	/** True while one of our own screens is open. */
	boolean ourScreenOpen();

	/** F1 pressed (HUD hidden). */
	boolean hudHidden();

	boolean firstPerson();

	int fps();

	/** Vanilla "Cinematic Camera" (used by Zoom). */
	boolean smoothCamera();

	void setSmoothCamera(boolean on);

	/** Width of menu text (see {@code Gfx#uiText}); usable outside of drawing. */
	int uiTextWidth(int weight, int px, String s);

	/** Width of Minecraft-font HUD text. */
	int mcTextWidth(String s, boolean bold);

	// ------------------------------------------------------------ player

	double x();

	double y();

	double z();

	double eyeY();

	float yaw();

	float pitch();

	boolean onGround();

	double horizontalSpeed();

	int hurtTime();

	boolean sprinting();

	String playerName();

	// ------------------------------------------------------------ server / world

	/** Latency in ms from the tab list, 0 in singleplayer. */
	int ping();

	boolean connected();

	/** Server address, or null when not on a server. */
	String serverIp();

	boolean singleplayer();

	/** Identifies the current world + dimension (waypoints are stored per key). */
	String worldKey();

	int worldMinY();

	int worldMaxY();

	// ------------------------------------------------------------ keys

	boolean keyDown(Bind bind);

	/** Short name of the key bound to {@code bind}, e.g. "W". */
	String keyName(Bind bind);

	/** Takes one queued press of {@code bind} (true while any remain). */
	boolean consumeClick(Bind bind);

	void setKeyDown(Bind bind, boolean down);

	boolean rawKeyDown(int glfwKey);

	boolean rawMouseDown(int button);

	// ------------------------------------------------------------ items / effects

	/** Armour (and optionally the held item) as drawable entries. */
	List<ItemInfo> armor(boolean includeHeld, boolean preview);

	List<EffectInfo> effects(boolean preview);

	// ------------------------------------------------------------ entities (opaque handles)

	boolean isLiving(Object entity);

	boolean isAlive(Object entity);

	int hurtTime(Object entity);

	/** Distance from your eyes to where the crosshair ray hits {@code target}, or -1. */
	double reachTo(Object target);

	/** {x, y, z} just above the entity's head. */
	double[] aboveHead(Object entity);

	// ------------------------------------------------------------ camera

	/** Camera position, rotation and the vertical FOV of the last frame. */
	CameraInfo camera();

	// ------------------------------------------------------------ actions

	void sendChat(String message);

	void sendCommand(String command);

	void openChat(String prefill);

	void openScreen(UiScreen screen);

	void closeScreen();

	void openPath(Path folder);

	void openUrl(String url);

	/** Runs on the game thread (next tick if called from elsewhere). */
	void execute(Runnable r);

	// ------------------------------------------------------------ environment

	Path configDir();

	boolean modLoaded(String id);

	/** Minecraft version, e.g. "1.21.11". */
	String minecraftVersion();
}
