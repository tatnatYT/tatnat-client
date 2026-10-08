package com.tatnat.client.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tatnat.client.modules.impl.cosmetic.CustomCapes;
import com.tatnat.client.modules.impl.cosmetic.EnchantGlint;
import com.tatnat.client.modules.impl.hud.ArmorStatus;
import com.tatnat.client.modules.impl.hud.Direction;
import com.tatnat.client.modules.impl.hud.DeathInfo;
import com.tatnat.client.modules.impl.hud.PackDisplay;
import com.tatnat.client.modules.impl.hud.Mousestrokes;
import com.tatnat.client.modules.impl.utility.DropPrevention;
import com.tatnat.client.modules.impl.utility.TierTagger;
import com.tatnat.client.modules.impl.utility.Screenshot;
import com.tatnat.client.modules.impl.utility.Reconnect;
import com.tatnat.client.modules.impl.visual.AutohideHud;
import com.tatnat.client.modules.impl.visual.TeamTracker;
import com.tatnat.client.modules.impl.visual.OverlayToggles;
import com.tatnat.client.modules.impl.visual.CameraTweaks;
import com.tatnat.client.modules.impl.visual.MobOverlay;
import com.tatnat.client.modules.impl.visual.AttackIndicator;
import com.tatnat.client.modules.impl.visual.LightLevelOverlay;
import com.tatnat.client.modules.impl.visual.TitleTweaker;
import com.tatnat.client.modules.impl.visual.UiScaling;
import com.tatnat.client.modules.impl.visual.CustomChat;
import com.tatnat.client.modules.impl.visual.InventoryTweaks;
import com.tatnat.client.modules.impl.visual.PlayerModel;
import com.tatnat.client.modules.impl.visual.CustomF3;
import com.tatnat.client.modules.impl.visual.Tablist;
import com.tatnat.client.modules.impl.visual.CustomFog;
import com.tatnat.client.modules.impl.visual.ViewModel;
import com.tatnat.client.modules.impl.visual.PostEffects;
import com.tatnat.client.modules.impl.utility.SoundFilters;
import com.tatnat.client.modules.impl.utility.DiscordStatus;
import com.tatnat.client.modules.impl.hud.UhcOverlay;
import com.tatnat.client.modules.impl.hud.Hypixel;
import com.tatnat.client.modules.impl.hud.Horses;
import com.tatnat.client.modules.impl.visual.AutoPerspective;
import com.tatnat.client.modules.impl.visual.HitIndicator;
import com.tatnat.client.modules.impl.visual.Snaplook;
import com.tatnat.client.modules.impl.hud.ItemCounter;
import com.tatnat.client.modules.impl.hud.TotemCounter;
import com.tatnat.client.modules.impl.visual.DamageIndicator;
import com.tatnat.client.modules.impl.visual.ItemDespawn;
import com.tatnat.client.modules.impl.visual.LootBeams;
import com.tatnat.client.modules.impl.visual.TntTimer;
import com.tatnat.client.modules.impl.hud.Playtime;
import com.tatnat.client.modules.impl.hud.SpeedMeter;
import com.tatnat.client.modules.impl.hud.Stopwatch;
import com.tatnat.client.modules.impl.hud.SystemResources;
import com.tatnat.client.modules.impl.utility.Backups;
import com.tatnat.client.modules.impl.utility.CullLogs;
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
		add(new Direction());
		add(new SpeedMeter());
		add(new Stopwatch());
		add(new Playtime());
		add(new SystemResources());
		add(new ItemCounter());
		add(new TotemCounter());
		add(new DeathInfo());
		add(new PackDisplay());
		add(new Mousestrokes());
		add(new UhcOverlay());
		add(new Hypixel());
		add(new Horses());
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
		add(new Backups());
		add(new CullLogs());
		add(new DropPrevention());
		add(new TierTagger());
		add(new Screenshot());
		add(new Reconnect());
		add(new SoundFilters());
		add(new DiscordStatus());
		add(new BetterTooltips());
		add(new ScrollableTooltips());
		add(new ChunkAnimator());
		add(new TntTimer());
		add(new ItemDespawn());
		add(new DamageIndicator());
		add(new LootBeams());
		add(new HitIndicator());
		add(new Snaplook());
		add(new AutoPerspective());
		add(new AutohideHud());
		add(new TeamTracker());
		add(new OverlayToggles.BossBar());
		add(new OverlayToggles.ToastControl());
		add(new OverlayToggles.Subtitles());
		add(new CameraTweaks());
		add(new MobOverlay());
		add(new AttackIndicator());
		add(new LightLevelOverlay());
		add(new TitleTweaker());
		add(new UiScaling());
		add(new CustomChat());
		add(new InventoryTweaks());
		add(new PlayerModel());
		add(new CustomF3());
		add(new Tablist());
		add(new CustomFog());
		add(new ViewModel());
		add(new PostEffects.MotionBlur());
		add(new PostEffects.ColorSaturation());
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
