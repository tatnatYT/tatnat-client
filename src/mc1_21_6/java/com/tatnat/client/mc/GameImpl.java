package com.tatnat.client.mc;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.tatnat.client.platform.Bind;
import com.tatnat.client.platform.CameraInfo;
import com.tatnat.client.platform.EffectInfo;
import com.tatnat.client.platform.Game;
import com.tatnat.client.platform.ItemInfo;
import com.tatnat.client.platform.UiScreen;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.Util;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** {@link Game} for Minecraft 1.21.11. */
public final class GameImpl implements Game {
	public static final GameImpl INSTANCE = new GameImpl();
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** Vertical FOV of the last rendered frame (set by GameRendererMixin). */
	public static volatile float lastFov = 70f;

	private final Minecraft mc = Minecraft.getInstance();

	private GameImpl() {
	}

	private KeyMapping key(Bind b) {
		switch (b) {
			case FORWARD: return mc.options.keyUp;
			case BACK: return mc.options.keyDown;
			case LEFT: return mc.options.keyLeft;
			case RIGHT: return mc.options.keyRight;
			case JUMP: return mc.options.keyJump;
			case SNEAK: return mc.options.keyShift;
			case SPRINT: return mc.options.keySprint;
			case ATTACK: return mc.options.keyAttack;
			default: return mc.options.keyUse;
		}
	}

	// ------------------------------------------------------------ window / state

	@Override
	public int windowWidth() {
		return mc.getWindow().getWidth();
	}

	@Override
	public int windowHeight() {
		return mc.getWindow().getHeight();
	}

	@Override
	public int guiScale() {
		return mc.getWindow().getGuiScale();
	}

	@Override
	public int guiWidth() {
		return mc.getWindow().getGuiScaledWidth();
	}

	@Override
	public int guiHeight() {
		return mc.getWindow().getGuiScaledHeight();
	}

	@Override
	public double mouseX() {
		return mc.mouseHandler.xpos() * mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getScreenWidth());
	}

	@Override
	public double mouseY() {
		return mc.mouseHandler.ypos() * mc.getWindow().getHeight() / Math.max(1, mc.getWindow().getScreenHeight());
	}

	@Override
	public boolean inWorld() {
		return mc.player != null && mc.level != null;
	}

	@Override
	public boolean screenOpen() {
		return mc.screen != null;
	}

	@Override
	public boolean ourScreenOpen() {
		return mc.screen instanceof ScreenBridge;
	}

	@Override
	public boolean hudHidden() {
		return mc.options.hideGui;
	}

	@Override
	public boolean firstPerson() {
		return mc.options.getCameraType().isFirstPerson();
	}

	@Override
	public int fps() {
		return mc.getFps();
	}

	@Override
	public boolean smoothCamera() {
		return mc.options.smoothCamera;
	}

	@Override
	public void setSmoothCamera(boolean on) {
		mc.options.smoothCamera = on;
	}

	@Override
	public int uiTextWidth(int weight, int px, String s) {
		return mc.font.width(GfxImpl.uiComponent(weight, px, s));
	}

	@Override
	public int mcTextWidth(String s, boolean bold) {
		return bold ? mc.font.width(GfxImpl.mcComponent(s, true)) : mc.font.width(s);
	}

	// ------------------------------------------------------------ player

	@Override
	public double x() {
		return mc.player.getX();
	}

	@Override
	public double y() {
		return mc.player.getY();
	}

	@Override
	public double z() {
		return mc.player.getZ();
	}

	@Override
	public double eyeY() {
		return mc.player.getEyeY();
	}

	@Override
	public float yaw() {
		return mc.player.getYRot();
	}

	@Override
	public float pitch() {
		return mc.player.getXRot();
	}

	@Override
	public boolean onGround() {
		return mc.player.onGround();
	}

	@Override
	public double horizontalSpeed() {
		return mc.player.getDeltaMovement().horizontalDistance();
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
		return mc.getUser().getName();
	}

	// ------------------------------------------------------------ server / world

	@Override
	public int ping() {
		if (mc.getConnection() == null || mc.player == null) return 0;
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return info == null ? 0 : info.getLatency();
	}

	@Override
	public boolean connected() {
		return mc.getConnection() != null;
	}

	@Override
	public String serverIp() {
		ServerData server = mc.getCurrentServer();
		return server == null ? null : server.ip;
	}

	@Override
	public boolean singleplayer() {
		return mc.hasSingleplayerServer();
	}

	@Override
	public String worldKey() {
		if (mc.level == null) return "";
		String dim = mc.level.dimension().location().toString();
		if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip + "|" + dim;
		if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName() + "|" + dim;
		return "?|" + dim;
	}

	@Override
	public int worldMinY() {
		return mc.level == null ? -64 : mc.level.getMinY();
	}

	@Override
	public int worldMaxY() {
		return mc.level == null ? 320 : mc.level.getMaxY();
	}

	// ------------------------------------------------------------ keys

	@Override
	public boolean keyDown(Bind bind) {
		return key(bind).isDown();
	}

	@Override
	public String keyName(Bind bind) {
		return key(bind).getTranslatedKeyMessage().getString();
	}

	@Override
	public boolean consumeClick(Bind bind) {
		return key(bind).consumeClick();
	}

	@Override
	public void setKeyDown(Bind bind, boolean down) {
		key(bind).setDown(down);
	}

	@Override
	public boolean rawKeyDown(int glfwKey) {
		return InputConstants.isKeyDown(mc.getWindow().getWindow(), glfwKey);
	}

	@Override
	public boolean rawMouseDown(int button) {
		return GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), button) == GLFW.GLFW_PRESS;
	}

	// ------------------------------------------------------------ items / effects

	private static ItemInfo info(ItemStack s) {
		return new ItemInfo(s, s.getCount(), s.isDamageableItem(), s.getDamageValue(), s.getMaxDamage());
	}

	@Override
	public List<ItemInfo> armor(boolean includeHeld, boolean preview) {
		List<ItemInfo> list = new ArrayList<>();
		if (mc.player != null) {
			if (includeHeld && !mc.player.getMainHandItem().isEmpty()) list.add(info(mc.player.getMainHandItem()));
			for (EquipmentSlot slot : ARMOR) {
				ItemStack s = mc.player.getItemBySlot(slot);
				if (!s.isEmpty()) list.add(info(s));
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
		List<MobEffectInstance> raw = mc.player == null ? new ArrayList<>() : new ArrayList<>(mc.player.getActiveEffects());
		if (raw.isEmpty() && preview) {
			raw.add(new MobEffectInstance(MobEffects.SPEED, 20 * 90, 1));
			raw.add(new MobEffectInstance(MobEffects.STRENGTH, 20 * 45, 0));
		}
		float tickRate = mc.level != null ? mc.level.tickRateManager().tickrate() : 20f;
		List<EffectInfo> out = new ArrayList<>();
		for (MobEffectInstance e : raw) {
			boolean ending = !e.isInfiniteDuration() && e.getDuration() < 200;
			out.add(new EffectInfo(e.getEffect(), e.getEffect().value().getDisplayName().getString(), e.getAmplifier() + 1,
					MobEffectUtil.formatDuration(e, 1f, tickRate).getString(), ending));
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
		HitResult hit = mc.hitResult;
		if (mc.player == null || !(hit instanceof EntityHitResult) || ((EntityHitResult) hit).getEntity() != target) return -1;
		return mc.player.getEyePosition().distanceTo(hit.getLocation());
	}

	@Override
	public double[] aboveHead(Object entity) {
		Entity e = (Entity) entity;
		return new double[] {e.getX(), e.getBoundingBox().maxY + 0.35, e.getZ()};
	}

	// ------------------------------------------------------------ camera

	@Override
	public CameraInfo camera() {
		Camera cam = mc.gameRenderer.getMainCamera();
		return new CameraInfo(cam.position().x, cam.position().y, cam.position().z, cam.getYRot(), cam.getXRot(), lastFov);
	}

	// ------------------------------------------------------------ actions

	@Override
	public void sendChat(String message) {
		if (mc.player != null) mc.player.connection.sendChat(message);
	}

	@Override
	public void sendCommand(String command) {
		if (mc.player != null) mc.player.connection.sendCommand(command);
	}

	@Override
	public void openChat(String prefill) {
		mc.setScreen(new ChatScreen(prefill));
	}

	@Override
	public void openScreen(UiScreen screen) {
		mc.setScreen(new ScreenBridge(screen));
	}

	@Override
	public void closeScreen() {
		mc.setScreen(null);
	}

	@Override
	public void openPath(Path folder) {
		Util.getPlatform().openPath(folder);
	}

	@Override
	public void openUrl(String url) {
		Util.getPlatform().openUri(URI.create(url));
	}

	@Override
	public void execute(Runnable r) {
		mc.execute(r);
	}

	// ------------------------------------------------------------ environment

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public boolean modLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}

	@Override
	public String minecraftVersion() {
		return SharedConstants.getCurrentVersion().name();
	}
}
