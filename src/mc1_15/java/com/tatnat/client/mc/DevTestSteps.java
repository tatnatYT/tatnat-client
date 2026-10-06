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
import com.tatnat.client.modules.impl.visual.FullBright;
import com.tatnat.client.modules.impl.visual.Zoom;
import com.tatnat.client.ui.clickgui.ClickGuiScreen;
import com.tatnat.client.ui.hud.HudEditorScreen;

import net.minecraft.client.Minecraft;

/** The scripted dev test (see {@link DevTest}). Development only. */
final class DevTestSteps {
	private DevTestSteps() {
	}

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	private static void cmd(String c) {
		mc().player.chat("/" + c);
	}

	private static ClickGuiScreen gui;
	private static HudEditorScreen editor;

	static void build(String mode) {
		if (mode.contains("p2")) {
			step(60, "setup world (p2)", () -> {
				cmd("time set 6000");
				cmd("weather clear");
				cmd("gamemode creative");
				cmd("tp @s ~ ~ ~ 30 20");
			});
			phase2();
			step(20, "quit", () -> mc().stop());
			return;
		}
		step(60, "setup world", () -> {
			cmd("time set 6000");
			cmd("weather clear");
			cmd("gamemode creative");
			// Same open scene on every version/seed: clear a small arena with a grass floor.
			cmd("fill ~-6 ~ ~-6 ~6 ~6 ~6 air");
			cmd("fill ~-6 ~-1 ~-6 ~6 ~-1 ~6 grass_block");
			cmd("replaceitem entity @s armor.head diamond_helmet");
			cmd("replaceitem entity @s armor.chest diamond_chestplate");
			cmd("replaceitem entity @s armor.legs iron_leggings");
			cmd("replaceitem entity @s armor.feet diamond_boots");
			cmd("replaceitem entity @s weapon.mainhand diamond_sword");
			cmd("tp @s ~ ~ ~ 30 55");
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
		step(2, "list view", () -> {
			gui.devSearch("");
			gui.openSettingsFor(ModuleManager.get().get(Keystrokes.class));
			gui.devCloseSettings();
			gui.devShow(false, true);
		});
		step(15, "shot list", () -> shot("05b-list"));
		step(2, "settings page", () -> gui.devShow(true, false));
		step(15, "shot settings page", () -> shot("05c-client-settings"));
		step(2, "performance page", () -> gui.devPerformance());
		step(15, "shot performance", () -> shot("05d-performance"));
		step(2, "back to grid", () -> gui.devShow(false, false));

		step(2, "open hud editor", () -> {
			gui.devSearch("");
			editor = new HudEditorScreen(gui);
			GameImpl.INSTANCE.openScreen(editor);
		});
		step(20, "shot hud editor", () -> shot("06-hud-editor"));
		step(2, "drag clock near centre", () -> {
			Clock clock = ModuleManager.get().get(Clock.class);
			int w = editor.width, h = editor.height;
			double sx = clock.screenX(w) + 3, sy = clock.screenY(h) + 3;
			int sc = (int) mc().getWindow().getGuiScale();
			editor.mouseClicked(sx * sc, sy * sc, 0);
			// Aim 2 GUI px off the exact centre: the snap should pull it in and show guides.
			double tx = w / 2.0 - clock.scaledWidth() / 2 + 2 + 3, ty = h / 2.0 - clock.scaledHeight() / 2 - 1 + 3;
			editor.mouseDragged(tx * sc, ty * sc, 0);
		});
		step(10, "shot snapping", () -> shot("07-snapping"));
		step(2, "release + close", () -> {
			editor.mouseReleased(0, 0, 0);
			ModuleManager.get().get(Clock.class).resetPosition();
			mc().setScreen(null);
		});

		step(10, "zoom on", () -> Zoom.devForce = true);
		step(20, "shot zoom", () -> shot("08-zoom"));
		step(2, "zoom off, night", () -> {
			Zoom.devForce = false;
			cmd("time set 18000");
		});
		step(40, "shot night", () -> shot("09-night"));
		step(2, "fullbright on", () -> ModuleManager.get().get(FullBright.class).setEnabled(true));
		step(20, "shot fullbright", () -> shot("10-fullbright"));
		step(2, "fullbright off", () -> ModuleManager.get().get(FullBright.class).setEnabled(false));

		phase2();

		step(2, "title screen", () -> mc().setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
		step(30, "shot title", () -> shot("16-title"));
		step(2, "title button", GameImpl::openModMenu);
		step(20, "shot title menu", () -> shot("17-title-menu"));
		step(2, "back", () -> GameImpl.INSTANCE.closeScreen());
		if (!mode.contains("stay")) step(20, "quit", () -> mc().stop());
	}

	private static <T extends com.tatnat.client.modules.Module> T mod(Class<T> c) {
		return ModuleManager.get().get(c);
	}

	/** Phase 2 mods: combat HUD, world visuals, freecam, cosmetics, tooltips. */
	private static void phase2() {
		step(2, "combat setup", () -> {
			cmd("time set 6000");
			cmd("kill @e[type=pig]");
			cmd("effect give @s speed 120 1");
			cmd("effect give @s strength 45 0");
			cmd("replaceitem entity @s weapon.mainhand diamond_sword{Enchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}");
			// A pig 3 blocks in front, frozen, so the attacks land.
			double yaw = Math.toRadians(mc().player.yRot);
			double px = mc().player.getX() - Math.sin(yaw) * 2.8, pz = mc().player.getZ() + Math.cos(yaw) * 2.8;
			cmd(String.format(java.util.Locale.ROOT, "summon pig %.2f %.2f %.2f {NoAI:1b,Attributes:[{Name:\"generic.maxHealth\",Base:400d}],Health:400f}",
					px, mc().player.getY(), pz));
			cmd("tp @s ~ ~ ~ ~ 20");
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
				net.minecraft.world.entity.Entity pig = null;
				for (net.minecraft.world.entity.Entity e : mc().level.entitiesForRendering()) {
					if (e instanceof net.minecraft.world.entity.animal.Pig) pig = e;
				}
				if (pig == null) {
					com.tatnat.client.TatnatClient.LOG.warn("[devtest] no pig found");
					return;
				}
				// Look straight at it so the crosshair ray (and reach) is real.
				net.minecraft.world.phys.Vec3 eye = mc().player.getEyePosition(1f);
				net.minecraft.world.phys.Vec3 to = pig.getBoundingBox().getCenter().subtract(eye);
				mc().player.yRot = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
				mc().player.xRot = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
				mc().gameRenderer.pick(1f);
				mc().gameMode.attack(mc().player, pig);
				mc().player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
				com.tatnat.client.TatnatClient.LOG.info("[devtest] pig health {} hurtTime {}", ((net.minecraft.world.entity.LivingEntity) pig).getHealth(), ((net.minecraft.world.entity.LivingEntity) pig).hurtTime);
			});
		}
		step(3, "shot combat", () -> shot("11-combat"));

		step(2, "waypoint", () -> {
			mod(com.tatnat.client.modules.impl.visual.Hitboxes.class).setEnabled(false);
			com.tatnat.client.modules.impl.utility.Waypoints wp = mod(com.tatnat.client.modules.impl.utility.Waypoints.class);
			wp.setEnabled(true);
			double yaw = Math.toRadians(mc().player.yRot);
			wp.devAdd("Base", mc().player.getX() - Math.sin(yaw) * 30, mc().player.getY(), mc().player.getZ() + Math.cos(yaw) * 30);
			wp.devAdd("Behind", mc().player.getX() + Math.sin(yaw) * 40, mc().player.getY(), mc().player.getZ() - Math.cos(yaw) * 40);
			mc().player.xRot = 0;
			mod(com.tatnat.client.modules.impl.visual.TimeChanger.class).setEnabled(true);
		});
		step(15, "shot waypoint", () -> shot("12-waypoints-timechanger"));

		step(2, "cape + nick, third person back", () -> {
			mod(com.tatnat.client.modules.impl.utility.Waypoints.class).setEnabled(false);
			mod(com.tatnat.client.modules.impl.cosmetic.CustomCapes.class).setEnabled(true);
			mod(com.tatnat.client.modules.impl.visual.NickHider.class).setEnabled(true);
			mc().options.thirdPersonView = 1;
			cmd("say hello from tatnat");
		});
		step(20, "shot cape", () -> shot("13-cape-nick"));

		step(2, "freecam", () -> {
			mc().options.thirdPersonView = 0;
			mod(com.tatnat.client.modules.impl.visual.NickHider.class).setEnabled(false);
			com.tatnat.client.modules.impl.utility.Freecam fc = mod(com.tatnat.client.modules.impl.utility.Freecam.class);
			fc.setEnabled(true);
			// Fly up and back so the (hidden) body would be in view.
			FeaturesImpl.INSTANCE.freecam().moveBy(0, 4, 0);
			FeaturesImpl.INSTANCE.freecam().turn(0, 300);
		});
		step(15, "shot freecam", () -> shot("14-freecam"));
		step(2, "freecam off", () -> mod(com.tatnat.client.modules.impl.utility.Freecam.class).setEnabled(false));

		step(2, "tooltip", () -> {
			mod(com.tatnat.client.modules.impl.utility.BetterTooltips.class).setEnabled(true);
			mc().gameMode.setLocalMode(net.minecraft.world.level.GameType.SURVIVAL);
			net.minecraft.client.gui.screens.inventory.InventoryScreen inv = new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc().player);
			mc().setScreen(inv);
		});
		step(5, "hover sword", () -> {
			int scale = (int) mc().getWindow().getGuiScale();
			int gw = mc().getWindow().getGuiScaledWidth(), gh = mc().getWindow().getGuiScaledHeight();
			int left = (gw - 176) / 2, top = (gh - 166) / 2;
			double sx = (left + 8 + 8 + 18 * mc().player.inventory.selected) * scale, sy = (top + 142 + 8) * scale;
			org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc().getWindow().getWindow(),
					sx * mc().getWindow().getScreenWidth() / mc().getWindow().getWidth(), sy * mc().getWindow().getScreenHeight() / mc().getWindow().getHeight());
		});
		step(10, "shot tooltip", () -> shot("15-tooltip"));
		step(2, "close inv", () -> mc().setScreen(null));

		step(2, "chunk animator on + move", () -> {
			mod(com.tatnat.client.modules.impl.visual.ChunkAnimator.class).setEnabled(true);
			cmd("tp @s ~400 ~ ~");
		});
		step(60, "chunk animator check", () -> com.tatnat.client.TatnatClient.LOG.info("[devtest] chunk animator offset calls: {}",
				com.tatnat.client.modules.impl.visual.ChunkAnimator.calls));
	}
}
