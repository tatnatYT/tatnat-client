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

	/** Entities within {@code range} blocks of the player (not the player itself), for world overlays. */
	/** Chat background opacity, scale and width (0-1 each), through the game's own chat options. */
	default void setChatLook(double opacity, double scale, double width) {
	}

	/** Master volume (0-1), for Sound Filters. */
	default float masterVolume() {
		return 1f;
	}

	default void setMasterVolume(float volume) {
	}

	/** True while your head is under water. */
	default boolean underwater() {
		return false;
	}

	/** Jump charge (0-1) while riding a horse-like mount, else -1 (Horses). */
	default float horseJump() {
		return -1;
	}

	/** Loads one of the game's own post-processing shaders by name ("phosphor", "desaturate"), or none for null (1.15 - 1.20.4). */
	default void postEffect(String name) {
	}

	/** Every control: {id, name, category, key name} (Keybind Search, 1.14+). */
	default java.util.List<String[]> keyMappings() {
		return java.util.Collections.emptyList();
	}

	/** Binds a control to a keyboard key (GLFW code), or unbinds it for -1, and saves the options. */
	default void rebindKey(String id, int key) {
	}

	/** Resource packs the player can choose: {id, "1" if on, title}; on ones first, bottom to top (Pack Organizer, 1.16+). */
	default java.util.List<String[]> resourcePackList() {
		return java.util.Collections.emptyList();
	}

	/** Switches exactly these packs on (bottom to top) and reloads. Built-in required packs stay on. */
	default void applyResourcePacks(java.util.List<String> ids) {
	}

	/** Sets the GUI scale option (0 = auto) and applies it right away (UI Scaling). */
	default void setGuiScale(int scale) {
	}

	/** Attack indicator: 0 = off, 1 = under the crosshair, 2 = by the hotbar (1.9+). */
	default void setAttackIndicator(int mode) {
	}

	/** Block light (0-15) at a position, for Light Level Overlay. */
	default int blockLight(int x, int y, int z) {
		return 15;
	}

	/** True when a mob could stand here: open space with a solid block underneath. */
	default boolean spawnSurface(int x, int y, int z) {
		return false;
	}

	/** True while the "Disconnected" screen is open (also remembers the server while connected). */
	default boolean disconnectedScreen() {
		return false;
	}

	/** Joins the last server again; false when there is none to rejoin. */
	default boolean reconnect() {
		return false;
	}

	/** Turns the F1 "hide HUD" state on or off (Autohide HUD). */
	default void setHudHidden(boolean hidden) {
	}

	/** The resource packs switched on, bottom to top, as the options file names them ("file/Faithful.zip"). */
	default java.util.List<String> resourcePacks() {
		return java.util.Collections.emptyList();
	}

	/** 0 = first person, 1 = third person behind, 2 = third person in front. */
	default int perspective() {
		return firstPerson() ? 0 : 1;
	}

	default void setPerspective(int perspective) {
	}

	/** The player's health (20 = full), for Death Info and Hit Indicator. */
	default float health() {
		return 20;
	}

	/** Riding something, creative-flying or gliding with an elytra (for Auto Perspective). */
	default boolean ridingOrFlying() {
		return false;
	}

	/** Item counts in your inventory, keyed "translation key|display name" (for Totem / Item Counter). */
	default java.util.Map<String, Integer> inventoryCounts() {
		return java.util.Collections.emptyMap();
	}

	default java.util.List<EntityInfo> entities(double range) {
		return java.util.Collections.emptyList();
	}

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

	/** This mod's version (the loader knows it; the default is the one in the build). */
	default String modVersion() {
		return "1.0.0";
	}
}
