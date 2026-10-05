package com.tatnat.client.modules.impl.visual;

import java.util.Map;
import java.util.WeakHashMap;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ModeSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Easing;
import com.tatnat.client.ui.render.Icons;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Newly loaded chunks slide into place instead of popping in. Works on Minecraft's own chunk
 * renderer ({@code ChunkAnimatorMixin}); Sodium replaces that renderer, so with the FPS Boost
 * pack the animation is skipped (the mod says so in its description).
 */
public class ChunkAnimator extends Module {
	public static ChunkAnimator INSTANCE;
	public static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");

	public final SliderSetting duration = add(new SliderSetting("Duration", "How long a chunk takes to slide in", 700, 100, 3000, 50, "ms"));
	public final ModeSetting mode = add(new ModeSetting("Mode", "Where new chunks come from", "From Below", "From Below", "From Above", "From Y 0"));

	/** When each chunk section was first drawn. Weak keys: sections are dropped with their chunks. */
	private final Map<Object, Long> firstSeen = new WeakHashMap<>();

	public ChunkAnimator() {
		super("Chunk Animator", SODIUM ? "Chunks slide in as they load (not with Sodium / FPS Boost)" : "Chunks slide in as they load",
				Category.UTILITY, false);
		icon = Icons.Icon.LAYERS;
		INSTANCE = this;
	}

	@Override
	public boolean available() {
		return com.tatnat.client.TatnatClient.features().supports("chunk_animator");
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	/** Vertical offset in blocks for a section at {@code originY}, 0 once it has finished. */
	/** Dev test counter: proves the renderer hook is live. */
	public static int calls;

	public float offset(Object section, int originY) {
		calls++;
		long now = System.currentTimeMillis();
		Long t0 = firstSeen.get(section);
		if (t0 == null) {
			firstSeen.put(section, now);
			t0 = now;
		}
		float t = (now - t0) / duration.floatValue();
		if (t >= 1f) return 0f;
		float remaining = 1f - Easing.outCubic(t);
		if (mode.is("From Above")) return 24f * remaining;
		if (mode.is("From Y 0")) return -originY * remaining;
		return -24f * remaining;
	}

	@Override
	protected void onEnable() {
		firstSeen.clear();
	}
}
