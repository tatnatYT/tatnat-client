package com.tatnat.client.mc;

import static com.tatnat.client.mc.DevTest.shot;
import static com.tatnat.client.mc.DevTest.step;

import com.tatnat.client.modules.ModuleManager;
import com.tatnat.client.modules.impl.hud.ArmorStatus;
import com.tatnat.client.modules.impl.hud.Clock;
import com.tatnat.client.modules.impl.hud.Coordinates;
import com.tatnat.client.modules.impl.hud.Keystrokes;
import com.tatnat.client.modules.impl.hud.MemoryUsage;
import com.tatnat.client.modules.impl.visual.BlockOverlay;
import com.tatnat.client.ui.clickgui.ClickGuiScreen;
import com.tatnat.client.ui.hud.HudEditorScreen;

import net.minecraft.client.MinecraftClient;

/** The scripted dev test for the legacy versions (see {@link DevTest}). Commands use 1.8 syntax. */
final class DevTestSteps {
	private DevTestSteps() {
	}

	private static ClickGuiScreen gui;
	private static HudEditorScreen editor;

	private static MinecraftClient mc() {
		return MinecraftClient.getInstance();
	}

	private static void cmd(String c) {
		mc().player.sendChatMessage("/" + c);
	}

	static void build(String mode) {
		step(60, "setup world", () -> {
			cmd("time set 6000");
			cmd("weather clear");
			cmd("gamemode creative");
			// Same open scene on every version/seed: clear a small arena with a grass floor.
			cmd("fill ~-6 ~ ~-6 ~6 ~6 ~6 air");
			cmd("fill ~-6 ~-1 ~-6 ~6 ~-1 ~6 grass");
			cmd("replaceitem entity @p slot.armor.head diamond_helmet");
			cmd("replaceitem entity @p slot.armor.chest diamond_chestplate");
			cmd("replaceitem entity @p slot.armor.legs iron_leggings");
			cmd("replaceitem entity @p slot.armor.feet diamond_boots");
			cmd("replaceitem entity @p slot.hotbar.0 diamond_sword");
			cmd("tp @p ~ ~ ~ 30 55");
			ModuleManager m = ModuleManager.get();
			m.get(Coordinates.class).setEnabled(true);
			m.get(ArmorStatus.class).setEnabled(true);
			m.get(Clock.class).setEnabled(true);
			m.get(MemoryUsage.class).setEnabled(true);
			m.get(BlockOverlay.class).setEnabled(true);
			m.get(Keystrokes.class).setFavorite(true);
		});
		step(60, "shot hud", () -> shot("01-hud"));

		step(5, "open clickgui", () -> {
			gui = new ClickGuiScreen();
			GameImpl.INSTANCE.openScreen(gui);
		});
		step(20, "shot clickgui", () -> shot("02-clickgui"));
		step(2, "select keystrokes", () -> gui.openSettingsFor(ModuleManager.get().get(Keystrokes.class)));
		step(20, "shot keystrokes settings", () -> shot("03-settings-keystrokes"));
		step(2, "select block overlay + open picker", () -> {
			gui.openSettingsFor(ModuleManager.get().get(BlockOverlay.class));
			gui.devOpenFirstColorPicker();
		});
		step(20, "shot picker", () -> shot("04-color-picker"));
		step(2, "search", () -> gui.devSearch("zo"));
		step(15, "shot search", () -> shot("05-search"));
		step(2, "performance page", () -> gui.devPerformance());
		step(15, "shot performance", () -> shot("05d-performance"));
		step(2, "back to grid", () -> {
			gui.devSearch("");
			gui.devCloseSettings();
			gui.devShow(false, false);
		});

		step(2, "open hud editor", () -> {
			editor = new HudEditorScreen(gui);
			GameImpl.INSTANCE.openScreen(editor);
		});
		step(20, "shot hud editor", () -> shot("06-hud-editor"));
		step(2, "close", () -> GameImpl.INSTANCE.closeScreen());

		step(10, "zoom on", () -> com.tatnat.client.modules.impl.visual.Zoom.devForce = true);
		step(20, "shot zoom", () -> shot("08-zoom"));
		step(2, "zoom off, night", () -> {
			com.tatnat.client.modules.impl.visual.Zoom.devForce = false;
			cmd("time set 18000");
		});
		step(40, "shot night", () -> shot("09-night"));
		step(2, "fullbright on", () -> mod(com.tatnat.client.modules.impl.visual.FullBright.class).setEnabled(true));
		step(20, "shot fullbright", () -> shot("10-fullbright"));
		step(2, "fullbright off", () -> mod(com.tatnat.client.modules.impl.visual.FullBright.class).setEnabled(false));

		phase2();
		step(2, "title screen", () -> mc().setScreen(new net.minecraft.client.gui.screen.TitleScreen()));
		step(30, "shot title", () -> shot("16-title"));
		step(2, "title button", GameImpl::openModMenu);
		step(20, "shot title menu", () -> shot("17-title-menu"));
		step(2, "back", () -> GameImpl.INSTANCE.closeScreen());
		step(20, "quit", () -> mc().scheduleStop());
	}

	private static <T extends com.tatnat.client.modules.Module> T mod(Class<T> c) {
		return ModuleManager.get().get(c);
	}

	/** Combat HUD, world visuals, freecam, cosmetics, tooltips. */
	private static void phase2() {
		step(2, "combat setup", () -> {
			cmd("time set 6000");
			cmd("kill @e[type=Pig]");
			cmd("effect @p speed 120 1");
			cmd("effect @p strength 45 0");
			cmd("replaceitem entity @p slot.hotbar.0 diamond_sword 1 0 {ench:[{id:16,lvl:5}]}");
			// A pig 3 blocks in front, frozen, so the attacks land.
			double yaw = Math.toRadians(mc().player.yaw);
			double px = mc().player.x - Math.sin(yaw) * 2.8, pz = mc().player.z + Math.cos(yaw) * 2.8;
			cmd(String.format(java.util.Locale.ROOT, "summon Pig %.2f %.2f %.2f {NoAI:1,Attributes:[{Name:generic.maxHealth,Base:400}],Health:400}",
					px, mc().player.y, pz));
			cmd("tp @p ~ ~ ~ ~ 20");
			mod(com.tatnat.client.modules.impl.hud.ComboCounter.class).setEnabled(true);
			mod(com.tatnat.client.modules.impl.hud.ReachDisplay.class).setEnabled(true);
			mod(com.tatnat.client.modules.impl.hud.PotionStatus.class).setEnabled(true);
			mod(com.tatnat.client.modules.impl.visual.Crosshair.class).setEnabled(true);
			com.tatnat.client.modules.impl.visual.Hitboxes hb = mod(com.tatnat.client.modules.impl.visual.Hitboxes.class);
			hb.setEnabled(true);
			for (com.tatnat.client.modules.settings.Setting<?> st : hb.settings()) {
				if (st instanceof com.tatnat.client.modules.settings.ModeSetting && st.name.equals("Show On")) ((com.tatnat.client.modules.settings.ModeSetting) st).set("All");
			}
			mod(com.tatnat.client.modules.impl.visual.HitColor.class).setEnabled(true);
			mod(com.tatnat.client.modules.impl.cosmetic.EnchantGlint.class).setEnabled(true);
			mod(BlockOverlay.class).setEnabled(false);
		});
		for (int i = 0; i < 3; i++) {
			step(12, "attack " + i, () -> {
				net.minecraft.entity.Entity pig = null;
				for (net.minecraft.entity.Entity e : mc().world.loadedEntities) {
					if (e instanceof net.minecraft.entity.passive.PigEntity) pig = e;
				}
				if (pig == null) {
					com.tatnat.client.TatnatClient.LOG.warn("[devtest] no pig found");
					return;
				}
				// Look straight at it so the crosshair ray (and reach) is real.
				net.minecraft.util.math.Box bb = pig.getBoundingBox();
				double tx = (bb.minX + bb.maxX) / 2 - mc().player.x, tz = (bb.minZ + bb.maxZ) / 2 - mc().player.z;
				double ty = (bb.minY + bb.maxY) / 2 - (mc().player.y + mc().player.getEyeHeight());
				mc().player.yaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
				mc().player.pitch = (float) -Math.toDegrees(Math.atan2(ty, Math.sqrt(tx * tx + tz * tz)));
				mc().gameRenderer.updateTargetedEntity(1f);
				mc().interactionManager.attackEntity(mc().player, pig);
				mc().player.swingHand();
				com.tatnat.client.TatnatClient.LOG.info("[devtest] pig health {} hurtTime {}", ((net.minecraft.entity.LivingEntity) pig).getHealth(), ((net.minecraft.entity.LivingEntity) pig).hurtTime);
			});
		}
		step(3, "shot combat", () -> shot("11-combat"));

		step(2, "waypoint", () -> {
			mod(com.tatnat.client.modules.impl.visual.Hitboxes.class).setEnabled(false);
			com.tatnat.client.modules.impl.utility.Waypoints wp = mod(com.tatnat.client.modules.impl.utility.Waypoints.class);
			wp.setEnabled(true);
			double yaw = Math.toRadians(mc().player.yaw);
			wp.devAdd("Base", mc().player.x - Math.sin(yaw) * 30, mc().player.y, mc().player.z + Math.cos(yaw) * 30);
			wp.devAdd("Behind", mc().player.x + Math.sin(yaw) * 40, mc().player.y, mc().player.z - Math.cos(yaw) * 40);
			mc().player.pitch = 0;
			mod(com.tatnat.client.modules.impl.visual.TimeChanger.class).setEnabled(true);
		});
		step(15, "shot waypoint", () -> shot("12-waypoints-timechanger"));

		step(2, "cape + nick, third person back", () -> {
			mod(com.tatnat.client.modules.impl.utility.Waypoints.class).setEnabled(false);
			mod(com.tatnat.client.modules.impl.cosmetic.CustomCapes.class).setEnabled(true);
			mod(com.tatnat.client.modules.impl.visual.NickHider.class).setEnabled(true);
			mc().options.perspective = 1;
			cmd("say hello from tatnat");
		});
		step(20, "shot cape", () -> shot("13-cape-nick"));

		step(2, "freecam", () -> {
			mc().options.perspective = 0;
			mod(com.tatnat.client.modules.impl.visual.NickHider.class).setEnabled(false);
			mod(com.tatnat.client.modules.impl.utility.Freecam.class).setEnabled(true);
			FeaturesImpl.INSTANCE.freecam().moveBy(0, 4, 0);
			FeaturesImpl.INSTANCE.freecam().increaseTransforms(0, 300);
		});
		step(15, "shot freecam", () -> shot("14-freecam"));
		step(2, "freecam off, survival", () -> {
			mod(com.tatnat.client.modules.impl.utility.Freecam.class).setEnabled(false);
			mod(com.tatnat.client.modules.impl.utility.BetterTooltips.class).setEnabled(true);
			cmd("gamemode survival");
		});
		step(20, "open inventory", () -> mc().setScreen(new net.minecraft.client.gui.screen.ingame.SurvivalInventoryScreen(mc().player)));
		step(5, "hover sword", () -> {
			net.minecraft.client.util.Window w = new net.minecraft.client.util.Window(mc());
			int scale = w.getScaleFactor();
			int left = (w.getWidth() - 176) / 2, top = (w.getHeight() - 166) / 2;
			int sx = (left + 8 + 8 + 18 * mc().player.inventory.selectedSlot) * scale, sy = (top + 142 + 8) * scale;
			// LWJGL 2 counts from the bottom of the window.
			org.lwjgl.input.Mouse.setCursorPosition(sx, mc().height - sy);
		});
		step(10, "shot tooltip", () -> shot("15-tooltip"));
		step(2, "close inv", () -> mc().setScreen(null));
	}
}
