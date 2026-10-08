package com.tatnat.client.mc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.tatnat.client.platform.Bind;
import com.tatnat.client.platform.CameraInfo;
import com.tatnat.client.platform.EffectInfo;
import com.tatnat.client.platform.Game;
import com.tatnat.client.platform.ItemInfo;
import com.tatnat.client.platform.UiScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;

/** {@link Game} for the legacy (LWJGL 2) versions. */
public final class GameImpl implements Game {
	public static final GameImpl INSTANCE = new GameImpl();

	/** Vertical FOV of the last rendered frame (set by GameRendererMixin). */
	public static volatile float lastFov = 70f;

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private GameImpl() {
	}

	private KeyBinding key(Bind b) {
		switch (b) {
			case FORWARD: return mc.options.forwardKey;
			case BACK: return mc.options.backKey;
			case LEFT: return mc.options.leftKey;
			case RIGHT: return mc.options.rightKey;
			case JUMP: return mc.options.jumpKey;
			case SNEAK: return mc.options.sneakKey;
			case SPRINT: return mc.options.sprintKey;
			case ATTACK: return mc.options.attackKey;
			case DROP: return mc.options.dropKey;
			default: return mc.options.useKey;
		}
	}

	private Window window() {
		return new Window(mc);
	}

	// ------------------------------------------------------------ window / state

	@Override
	public int windowWidth() {
		return mc.width;
	}

	@Override
	public int windowHeight() {
		return mc.height;
	}

	@Override
	public int guiScale() {
		return window().getScaleFactor();
	}

	@Override
	public int guiWidth() {
		return window().getWidth();
	}

	@Override
	public int guiHeight() {
		return window().getHeight();
	}

	@Override
	public double mouseX() {
		return Mouse.getX();
	}

	@Override
	public double mouseY() {
		// LWJGL 2 counts from the bottom of the window.
		return mc.height - Mouse.getY() - 1;
	}

	@Override
	public boolean inWorld() {
		return mc.player != null && mc.world != null;
	}

	@Override
	public boolean screenOpen() {
		return mc.currentScreen != null;
	}

	@Override
	public boolean ourScreenOpen() {
		return mc.currentScreen instanceof ScreenBridge;
	}

	@Override
	public boolean hudHidden() {
		return mc.options.hudHidden;
	}

	@Override
	public boolean firstPerson() {
		return mc.options.perspective == 0;
	}

	@Override
	public int fps() {
		return MinecraftClient.getCurrentFps();
	}

	@Override
	public boolean smoothCamera() {
		return mc.options.smoothCameraEnabled;
	}

	@Override
	public void setSmoothCamera(boolean on) {
		mc.options.smoothCameraEnabled = on;
	}

	@Override
	public int uiTextWidth(int weight, int px, String s) {
		return LegacyFont.get(weight, px).width(s);
	}

	@Override
	public int mcTextWidth(String s, boolean bold) {
		return mc.textRenderer.getStringWidth(GfxImpl.mcString(s, bold));
	}

	// ------------------------------------------------------------ player

	@Override
	public double x() {
		return mc.player.x;
	}

	@Override
	public double y() {
		return mc.player.y;
	}

	@Override
	public double z() {
		return mc.player.z;
	}

	@Override
	public double eyeY() {
		return mc.player.y + mc.player.getEyeHeight();
	}

	@Override
	public float yaw() {
		return mc.player.yaw;
	}

	@Override
	public float pitch() {
		return mc.player.pitch;
	}

	@Override
	public boolean onGround() {
		return mc.player.onGround;
	}

	@Override
	public double horizontalSpeed() {
		return Math.sqrt(mc.player.velocityX * mc.player.velocityX + mc.player.velocityZ * mc.player.velocityZ);
	}

	@Override
	public int hurtTime() {
		return mc.player.hurtTime;
	}

	@Override
	public boolean sprinting() {
		return mc.player.isSprinting();
	}

	@Override
	public String playerName() {
		return mc.getSession().getUsername();
	}

	// ------------------------------------------------------------ server / world

	@Override
	public int ping() {
		if (mc.getNetworkHandler() == null || mc.player == null) return 0;
		PlayerListEntry info = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
		return info == null ? 0 : info.getLatency();
	}

	@Override
	public boolean connected() {
		return mc.getNetworkHandler() != null;
	}

	@Override
	public String serverIp() {
		ServerInfo server = mc.getCurrentServerEntry();
		return server == null ? null : server.address;
	}

	@Override
	public boolean singleplayer() {
		return mc.isInSingleplayer();
	}

	@Override
	public String worldKey() {
		if (mc.player == null) return "";
		String dim = "dim" + mc.player.dimension;
		if (mc.getCurrentServerEntry() != null) return mc.getCurrentServerEntry().address + "|" + dim;
		if (mc.getServer() != null) return "sp:" + mc.getServer().getLevelName() + "|" + dim;
		return "?|" + dim;
	}

	@Override
	public int worldMinY() {
		return 0;
	}

	@Override
	public int worldMaxY() {
		return 256;
	}

	// ------------------------------------------------------------ keys

	@Override
	public boolean keyDown(Bind bind) {
		return key(bind).isPressed();
	}

	@Override
	public String keyName(Bind bind) {
		return GameOptions.getFormattedNameForKeyCode(key(bind).getCode());
	}

	@Override
	public boolean consumeClick(Bind bind) {
		return key(bind).wasPressed();
	}

	@Override
	public void setKeyDown(Bind bind, boolean down) {
		KeyBinding.setKeyPressed(key(bind).getCode(), down);
	}

	@Override
	public boolean rawKeyDown(int glfwKey) {
		int k = LwjglKeys.toLwjgl(glfwKey);
		return k > 0 && Keyboard.isKeyDown(k);
	}

	@Override
	public boolean rawMouseDown(int button) {
		return Mouse.isButtonDown(button);
	}

	// ------------------------------------------------------------ items / effects

	private static ItemInfo info(ItemStack s) {
		return new ItemInfo(s, s.getCount(), s.isDamageable(), s.getDamage(), s.getMaxDamage());
	}

	@Override
	public List<ItemInfo> armor(boolean includeHeld, boolean preview) {
		List<ItemInfo> list = new ArrayList<>();
		if (mc.player != null) {
			ItemStack held = mc.player.getMainHandStack();
			if (includeHeld && held != null && !held.isEmpty()) list.add(info(held));
			// Armour slots run boots (0) to helmet (3).
			for (int i = 3; i >= 0; i--) {
				ItemStack s = mc.player.inventory.getArmor(i);
				if (s != null && !s.isEmpty()) list.add(info(s));
			}
		}
		if (list.isEmpty() && preview) {
			// Sample set so the element can be positioned before you own any armour.
			if (includeHeld) list.add(info(new ItemStack(Items.DIAMOND_SWORD)));
			list.add(info(new ItemStack(Items.DIAMOND_HELMET)));
			list.add(info(new ItemStack(Items.DIAMOND_CHESTPLATE)));
			list.add(info(new ItemStack(Items.DIAMOND_LEGGINGS)));
			list.add(info(new ItemStack(Items.DIAMOND_BOOTS)));
		}
		return list;
	}

	@Override
	public List<EffectInfo> effects(boolean preview) {
		List<StatusEffectInstance> raw = new ArrayList<>();
		if (mc.player != null) {
			Collection<StatusEffectInstance> active = mc.player.getStatusEffectInstances();
			raw.addAll(active);
		}
		if (raw.isEmpty() && preview) {
			raw.add(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED, 20 * 90, 1));
			raw.add(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.STRENGTH, 20 * 45, 0));
		}
		List<EffectInfo> out = new ArrayList<>();
		for (StatusEffectInstance e : raw) {
			StatusEffect type = e.getStatusEffect();
			if (type == null) continue;
			out.add(new EffectInfo(type, I18n.translate(type.getTranslationKey()), e.getAmplifier() + 1,
					StatusEffect.method_2436(e, 1f), e.getDuration() < 200));
		}
		return out;
	}

	// ------------------------------------------------------------ entities

	@Override
	public boolean isLiving(Object entity) {
		return entity instanceof LivingEntity;
	}

	@Override
	public boolean isAlive(Object entity) {
		return entity instanceof Entity && ((Entity) entity).isAlive();
	}

	@Override
	public int hurtTime(Object entity) {
		return entity instanceof LivingEntity ? ((LivingEntity) entity).hurtTime : 0;
	}

	@Override
	public double reachTo(Object target) {
		BlockHitResult hit = mc.result;
		if (mc.player == null || hit == null || hit.entity != target || hit.pos == null) return -1;
		return mc.player.getCameraPosVec(1f).distanceTo(hit.pos);
	}

	@Override
	public double[] aboveHead(Object entity) {
		Entity e = (Entity) entity;
		return new double[] {e.x, e.getBoundingBox().maxY + 0.35, e.z};
	}

	@Override
	public java.util.List<com.tatnat.client.platform.EntityInfo> entities(double range) {
		java.util.List<com.tatnat.client.platform.EntityInfo> out = new java.util.ArrayList<>();
		if (mc.world == null || mc.player == null) return out;
		double r2 = range * range;
		for (Entity e : new java.util.ArrayList<>(mc.world.entities)) {
			if (e == mc.player || e.squaredDistanceTo(mc.player) > r2) continue;
			double top = e.getBoundingBox().maxY;
			if (e instanceof net.minecraft.entity.TntEntity) {
				out.add(com.tatnat.client.platform.EntityInfo.tnt(e.x, top, e.z, ((net.minecraft.entity.TntEntity) e).getFuse()));
			} else if (e instanceof net.minecraft.entity.ItemEntity) {
				net.minecraft.entity.ItemEntity it = (net.minecraft.entity.ItemEntity) e;
				if (it.getItemStack() == null) continue;
				out.add(com.tatnat.client.platform.EntityInfo.item(e.x, top, e.z, it.getAge(), it.getItemStack().getCustomName(), it.getItemStack().getCount()));
			} else if (e instanceof net.minecraft.entity.LivingEntity && !(e instanceof net.minecraft.entity.decoration.ArmorStandEntity)) {
				net.minecraft.entity.LivingEntity l = (net.minecraft.entity.LivingEntity) e;
				out.add(com.tatnat.client.platform.EntityInfo.living(e.x, top, e.z, e instanceof net.minecraft.entity.player.PlayerEntity, l.getHealth(), l.getMaxHealth(), e.getName().asUnformattedString()));
			}
		}
		return out;
	}

	@Override
	public java.util.Map<String, Integer> inventoryCounts() {
		java.util.Map<String, Integer> out = new java.util.HashMap<>();
		if (mc.player == null) return out;
		for (int i = 0; i < mc.player.inventory.getInvSize(); i++) {
			ItemStack s = mc.player.inventory.getInvStack(i);
			if (s == null || s.isEmpty()) continue;
			out.merge(s.getItem().getTranslationKey(s) + "|" + s.getCustomName(), s.getCount(), Integer::sum);
		}
		return out;
	}

	@Override
	public int perspective() {
		return mc.options.perspective;
	}

	@Override
	public void setPerspective(int perspective) {
		mc.options.perspective = Math.max(0, Math.min(2, perspective));
	}

	@Override
	public float health() {
		return mc.player == null ? 20 : mc.player.getHealth();
	}

	@Override
	public boolean ridingOrFlying() {
		return mc.player != null && (mc.player.hasMount() || mc.player.abilities.flying);
	}

	@Override
	public void setHudHidden(boolean hidden) {
		mc.options.hudHidden = hidden;
	}

	@Override
	public java.util.List<String> resourcePacks() {
		return new java.util.ArrayList<>(mc.options.resourcePacks);
	}

	private static net.minecraft.client.network.ServerInfo lastServer;

	@Override
	public boolean disconnectedScreen() {
		if (mc.getCurrentServerEntry() != null) lastServer = mc.getCurrentServerEntry();
		return mc.currentScreen instanceof net.minecraft.client.gui.screen.DisconnectedScreen;
	}

	@Override
	public boolean reconnect() {
		net.minecraft.client.network.ServerInfo d = lastServer;
		if (d == null) return false;
		String[] hp = d.address.split(":");
		mc.setScreen(new net.minecraft.client.gui.screen.ConnectScreen(new net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen(
				new net.minecraft.client.gui.screen.TitleScreen()), mc, hp[0], hp.length > 1 ? Integer.parseInt(hp[1]) : 25565));
		return true;
	}

	@Override
	public void setAttackIndicator(int mode) {
		mc.options.field_13290 = Math.max(0, Math.min(2, mode));
	}

	@Override
	public int blockLight(int x, int y, int z) {
		return mc.world == null ? 15 : mc.world.getLightAtPos(net.minecraft.world.LightType.BLOCK, new net.minecraft.util.math.BlockPos(x, y, z));
	}

	@Override
	public boolean spawnSurface(int x, int y, int z) {
		if (mc.world == null) return false;
		net.minecraft.util.math.BlockPos p = new net.minecraft.util.math.BlockPos(x, y, z), below = p.down();
		return mc.world.isAir(p) && mc.world.getBlockState(below).isFullBlock();
	}

	@Override
	public void setGuiScale(int scale) {
		mc.options.guiScale = Math.max(0, scale);
	}

	@Override
	public float horseJump() {
		return mc.player != null && mc.player.isRidingHorse() ? mc.player.getMountJumpStrength() : -1;
	}

	@Override
	public void setChatLook(double opacity, double scale, double width) {
		mc.options.chatScale = (float) scale;
		mc.options.chatWidth = (float) width;
	}

	@Override
	public float masterVolume() {
		return mc.options.getSoundVolume(net.minecraft.client.sound.SoundCategory.MASTER);
	}

	@Override
	public void setMasterVolume(float volume) {
		mc.options.setSoundVolume(net.minecraft.client.sound.SoundCategory.MASTER, Math.max(0, Math.min(1, volume)));
	}

	@Override
	public boolean underwater() {
		return mc.player != null && mc.player.isSubmergedIn(net.minecraft.block.material.Material.WATER);
	}

	// ------------------------------------------------------------ camera

	@Override
	public CameraInfo camera() {
		Entity cam = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
		float pt = FeaturesImpl.partialTick;
		double x = cam.prevTickX + (cam.x - cam.prevTickX) * pt;
		double y = cam.prevTickY + (cam.y - cam.prevTickY) * pt + cam.getEyeHeight();
		double z = cam.prevTickZ + (cam.z - cam.prevTickZ) * pt;
		float yaw = cam.prevYaw + (cam.yaw - cam.prevYaw) * pt, pitch = cam.prevPitch + (cam.pitch - cam.prevPitch) * pt;
		if (mc.options.perspective == 2) {
			yaw += 180f;
			pitch = -pitch;
		}
		return new CameraInfo(x, y, z, yaw, pitch, lastFov);
	}

	// ------------------------------------------------------------ actions

	@Override
	public void sendChat(String message) {
		if (mc.player != null) mc.player.sendChatMessage(message);
	}

	@Override
	public void sendCommand(String command) {
		if (mc.player != null) mc.player.sendChatMessage("/" + command);
	}

	@Override
	public void openChat(String prefill) {
		mc.setScreen(new ChatScreen(prefill));
	}

	@Override
	public void openScreen(UiScreen screen) {
		mc.setScreen(new ScreenBridge(screen));
	}

	/** What the title-screen button does. */
	public static void openModMenu() {
		INSTANCE.openScreen(new com.tatnat.client.ui.clickgui.ClickGuiScreen());
	}

	@Override
	public void closeScreen() {
		mc.setScreen(null);
	}

	@Override
	public void openPath(Path folder) {
		Sys.openURL(folder.toUri().toString());
	}

	@Override
	public void openUrl(String url) {
		Sys.openURL(url);
	}

	@Override
	public void execute(Runnable r) {
		mc.submit(r);
	}

	// ------------------------------------------------------------ environment

	@Override
	public Path configDir() {
		return com.tatnat.client.mc.LoaderInfo.configDir();
	}

	@Override
	public boolean modLoaded(String id) {
		return com.tatnat.client.mc.LoaderInfo.modLoaded(id);
	}

	@Override
	public String modVersion() {
		return com.tatnat.client.mc.LoaderInfo.modVersion();
	}

	@Override
	public String minecraftVersion() {
		return com.tatnat.client.mc.LoaderInfo.minecraftVersion();
	}
}
