package com.tatnat.client.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tatnat.client.modules.impl.cosmetic.CustomCapes;
import com.tatnat.client.modules.impl.cosmetic.EnchantGlint;
import com.tatnat.client.modules.impl.hud.ArmorStatus;
import com.tatnat.client.modules.impl.hud.Clock;
import com.tatnat.client.modules.impl.hud.ComboCounter;
import com.tatnat.client.modules.impl.hud.Coordinates;
import com.tatnat.client.modules.impl.hud.CpsCounter;
import com.tatnat.client.modules.impl.hud.FpsCounter;
import com.tatnat.client.modules.impl.hud.Keystrokes;
import com.tatnat.client.modules.impl.hud.MemoryUsage;
import com.tatnat.client.modules.impl.hud.PingDisplay;
import com.tatnat.client.modules.impl.hud.PotionStatus;
import com.tatnat.client.modules.impl.hud.ReachDisplay;
import com.tatnat.client.modules.impl.hud.ServerAddress;
import com.tatnat.client.modules.impl.utility.AutoGG;
import com.tatnat.client.modules.impl.utility.BetterTooltips;
import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.utility.Macros;
import com.tatnat.client.modules.impl.utility.ScrollableTooltips;
import com.tatnat.client.modules.impl.utility.ToggleSprint;
import com.tatnat.client.modules.impl.utility.Waypoints;
import com.tatnat.client.modules.impl.visual.BlockOverlay;
import com.tatnat.client.modules.impl.visual.ChunkAnimator;
import com.tatnat.client.modules.impl.visual.ClearWater;
import com.tatnat.client.modules.impl.visual.Crosshair;
import com.tatnat.client.modules.impl.visual.FovModifier;
import com.tatnat.client.modules.impl.visual.FullBright;
import com.tatnat.client.modules.impl.visual.HitColor;
import com.tatnat.client.modules.impl.visual.Hitboxes;
import com.tatnat.client.modules.impl.visual.NickHider;
import com.tatnat.client.modules.impl.visual.TimeChanger;
import com.tatnat.client.modules.impl.visual.Zoom;

/** Owns every module. Registration order is the order shown in the menu. */
public final class ModuleManager {
	private static ModuleManager instance;
	private final List<Module> modules = new ArrayList<>();

	public static ModuleManager get() {
		if (instance == null) instance = new ModuleManager();
		return instance;
	}

	private ModuleManager() {
		// HUD
		add(new Keystrokes());
		add(new FpsCounter());
		add(new CpsCounter());
		add(new Coordinates());
		add(new ArmorStatus());
		add(new PotionStatus());
		add(new ComboCounter());
		add(new ReachDisplay());
		add(new Clock());
		add(new MemoryUsage());
		add(new PingDisplay());
		add(new ServerAddress());
		// Visual
		add(new Zoom());
		add(new Crosshair());
		add(new BlockOverlay());
		add(new FullBright());
		add(new FovModifier());
		add(new Hitboxes());
		add(new HitColor());
		add(new ClearWater());
		add(new TimeChanger());
		add(new NickHider());
		// Utility
		add(new ToggleSprint());
		add(new Waypoints());
		add(new Freecam());
		add(new AutoGG());
		add(new Macros());
		add(new BetterTooltips());
		add(new ScrollableTooltips());
		add(new ChunkAnimator());
		// Cosmetic
		add(new CustomCapes());
		add(new EnchantGlint());
	}

	private void add(Module m) {
		modules.add(m);
	}

	public List<Module> all() {
		return Collections.unmodifiableList(modules);
	}

	public List<HudModule> hud() {
		List<HudModule> list = new ArrayList<>();
		for (Module m : modules) if (m instanceof HudModule) list.add((HudModule) m);
		return list;
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> type) {
		for (Module m : modules) if (type.isInstance(m)) return (T) m;
		throw new IllegalArgumentException("No module " + type.getSimpleName());
	}
}
