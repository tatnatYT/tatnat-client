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

	/** Looked up on use: NeoForge constructs mods before the client exists. */
	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	private GameImpl() {
	}

	private KeyMapping key(Bind b) {
		switch (b) {
			case FORWARD: return mc().options.keyUp;
			case BACK: return mc().options.keyDown;
			case LEFT: return mc().options.keyLeft;
			case RIGHT: return mc().options.keyRight;
			case JUMP: return mc().options.keyJump;
			case SNEAK: return mc().options.keyShift;
			case SPRINT: return mc().options.keySprint;
			case ATTACK: return mc().options.keyAttack;
			case DROP: return mc().options.keyDrop;
			default: return mc().options.keyUse;
		}
	}

	// ------------------------------------------------------------ window / state

	@Override
	public int windowWidth() {
		return mc().getWindow().getWidth();
	}

	@Override
	public int windowHeight() {
		return mc().getWindow().getHeight();
	}

	@Override
	public int guiScale() {
		return (int) mc().getWindow().getGuiScale();
	}

	@Override
	public int guiWidth() {
		return mc().getWindow().getGuiScaledWidth();
	}

	@Override
	public int guiHeight() {
		return mc().getWindow().getGuiScaledHeight();
	}

	@Override
	public double mouseX() {
		return mc().mouseHandler.xpos() * mc().getWindow().getWidth() / Math.max(1, mc().getWindow().getScreenWidth());
	}

	@Override
	public double mouseY() {
		return mc().mouseHandler.ypos() * mc().getWindow().getHeight() / Math.max(1, mc().getWindow().getScreenHeight());
	}

	@Override
	public boolean inWorld() {
		return mc().player != null && mc().level != null;
	}

	@Override
	public boolean screenOpen() {
		return mc().screen != null;
	}

	@Override
	public boolean ourScreenOpen() {
		return mc().screen instanceof ScreenBridge;
	}

	@Override
	public boolean hudHidden() {
		return mc().options.hideGui;
	}

	@Override
	public boolean firstPerson() {
		return mc().options.getCameraType().isFirstPerson();
	}

	@Override
	public int fps() {
		return mc().getFps();
	}

	@Override
	public boolean smoothCamera() {
		return mc().options.smoothCamera;
	}

	@Override
	public void setSmoothCamera(boolean on) {
		mc().options.smoothCamera = on;
	}

	@Override
	public int uiTextWidth(int weight, int px, String s) {
		return mc().font.width(GfxImpl.uiComponent(weight, px, s));
	}

	@Override
	public int mcTextWidth(String s, boolean bold) {
		return bold ? mc().font.width(GfxImpl.mcComponent(s, true)) : mc().font.width(s);
	}

	// ------------------------------------------------------------ player

	@Override
	public double x() {
		return mc().player.getX();
	}

	@Override
	public double y() {
		return mc().player.getY();
	}

	@Override
	public double z() {
		return mc().player.getZ();
	}

	@Override
	public double eyeY() {
		return mc().player.getEyeY();
	}

	@Override
	public float yaw() {
		return mc().player.getYRot();
	}

	@Override
	public float pitch() {
		return mc().player.getXRot();
	}

	@Override
	public boolean onGround() {
		return mc().player.isOnGround();
	}

	@Override
	public double horizontalSpeed() {
		return mc().player.getDeltaMovement().horizontalDistance();
	}

	@Override
	public int hurtTime() {
		return mc().player.hurtTime;
	}

	@Override
	public boolean sprinting() {
		return mc().player.isSprinting();
	}

	@Override
	public String playerName() {
		return mc().getUser().getName();
	}

	// ------------------------------------------------------------ server / world

	@Override
	public int ping() {
		if (mc().getConnection() == null || mc().player == null) return 0;
		PlayerInfo info = mc().getConnection().getPlayerInfo(mc().player.getUUID());
		return info == null ? 0 : info.getLatency();
	}

	@Override
	public boolean connected() {
		return mc().getConnection() != null;
	}

	@Override
	public String serverIp() {
		ServerData server = mc().getCurrentServer();
		return server == null ? null : server.ip;
	}

	@Override
	public boolean singleplayer() {
		return mc().hasSingleplayerServer();
	}

	@Override
	public String worldKey() {
		if (mc().level == null) return "";
		String dim = mc().level.dimension().location().toString();
		if (mc().getCurrentServer() != null) return mc().getCurrentServer().ip + "|" + dim;
		if (mc().getSingleplayerServer() != null) return "sp:" + mc().getSingleplayerServer().getWorldData().getLevelName() + "|" + dim;
		return "?|" + dim;
	}

	@Override
	public int worldMinY() {
		return mc().level == null ? -64 : mc().level.getMinBuildHeight();
	}

	@Override
	public int worldMaxY() {
		return mc().level == null ? 320 : mc().level.getMaxBuildHeight();
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
		return InputConstants.isKeyDown(mc().getWindow().getWindow(), glfwKey);
	}

	@Override
	public boolean rawMouseDown(int button) {
		return GLFW.glfwGetMouseButton(mc().getWindow().getWindow(), button) == GLFW.GLFW_PRESS;
	}

	// ------------------------------------------------------------ items / effects

	private static ItemInfo info(ItemStack s) {
		return new ItemInfo(s, s.getCount(), s.isDamageableItem(), s.getDamageValue(), s.getMaxDamage());
	}

	@Override
	public List<ItemInfo> armor(boolean includeHeld, boolean preview) {
		List<ItemInfo> list = new ArrayList<>();
		if (mc().player != null) {
			if (includeHeld && !mc().player.getMainHandItem().isEmpty()) list.add(info(mc().player.getMainHandItem()));
			for (EquipmentSlot slot : ARMOR) {
				ItemStack s = mc().player.getItemBySlot(slot);
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
		List<MobEffectInstance> raw = mc().player == null ? new ArrayList<>() : new ArrayList<>(mc().player.getActiveEffects());
		if (raw.isEmpty() && preview) {
			raw.add(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 90, 1));
			raw.add(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 45, 0));
		}
		List<EffectInfo> out = new ArrayList<>();
		for (MobEffectInstance e : raw) {
			boolean ending = !e.isInfiniteDuration() && e.getDuration() < 200;
			out.add(new EffectInfo(e.getEffect(), e.getEffect().getDisplayName().getString(), e.getAmplifier() + 1,
					MobEffectUtil.formatDuration(e, 1f).getString(), ending));
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
		HitResult hit = mc().hitResult;
		if (mc().player == null || !(hit instanceof EntityHitResult) || ((EntityHitResult) hit).getEntity() != target) return -1;
		return mc().player.getEyePosition().distanceTo(hit.getLocation());
	}

	@Override
	public double[] aboveHead(Object entity) {
		Entity e = (Entity) entity;
		return new double[] {e.getX(), e.getBoundingBox().maxY + 0.35, e.getZ()};
	}

	@Override
	public java.util.List<com.tatnat.client.platform.EntityInfo> entities(double range) {
		java.util.List<com.tatnat.client.platform.EntityInfo> out = new java.util.ArrayList<>();
		Minecraft m = mc();
		if (m.level == null || m.player == null) return out;
		double r2 = range * range;
		for (Entity e : m.level.entitiesForRendering()) {
			if (e == m.player || e.distanceToSqr(m.player) > r2) continue;
			double top = e.getBoundingBox().maxY;
			if (e instanceof net.minecraft.world.entity.item.PrimedTnt) {
				out.add(com.tatnat.client.platform.EntityInfo.tnt(e.getX(), top, e.getZ(), ((net.minecraft.world.entity.item.PrimedTnt) e).getFuse()));
			} else if (e instanceof net.minecraft.world.entity.item.ItemEntity) {
				net.minecraft.world.entity.item.ItemEntity it = (net.minecraft.world.entity.item.ItemEntity) e;
				out.add(com.tatnat.client.platform.EntityInfo.item(e.getX(), top, e.getZ(), it.getAge(), it.getItem().getHoverName().getString(), it.getItem().getCount()));
			} else if (e instanceof LivingEntity && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)) {
				LivingEntity l = (LivingEntity) e;
				out.add(com.tatnat.client.platform.EntityInfo.living(e.getX(), top, e.getZ(), e instanceof net.minecraft.world.entity.player.Player, l.getHealth(), l.getMaxHealth(), e.getName().getString()));
			}
		}
		return out;
	}

	@Override
	public java.util.Map<String, Integer> inventoryCounts() {
		java.util.Map<String, Integer> out = new java.util.HashMap<>();
		Minecraft m = mc();
		if (m.player == null) return out;
		net.minecraft.world.entity.player.Inventory inv = m.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			out.merge(s.getItem().getDescriptionId() + "|" + s.getHoverName().getString(), s.getCount(), Integer::sum);
		}
		return out;
	}

	@Override
	public int perspective() {
		return mc().options.getCameraType().ordinal();
	}

	@Override
	public void setPerspective(int perspective) {
		mc().options.setCameraType(net.minecraft.client.CameraType.values()[Math.max(0, Math.min(2, perspective))]);
	}

	@Override
	public float health() {
		return mc().player == null ? 20 : mc().player.getHealth();
	}

	@Override
	public boolean ridingOrFlying() {
		Minecraft m = mc();
		return m.player != null && (m.player.isPassenger() || m.player.getAbilities().flying || m.player.isFallFlying());
	}

	@Override
	public void setHudHidden(boolean hidden) {
		mc().options.hideGui = hidden;
	}

	@Override
	public java.util.List<String> resourcePacks() {
		return new java.util.ArrayList<>(mc().options.resourcePacks);
	}

	private static net.minecraft.client.multiplayer.ServerData lastServer;

	@Override
	public boolean disconnectedScreen() {
		Minecraft m = mc();
		if (m.getCurrentServer() != null) lastServer = m.getCurrentServer();
		return m.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen;
	}

	@Override
	public boolean reconnect() {
		net.minecraft.client.multiplayer.ServerData d = lastServer;
		if (d == null) return false;
		Minecraft m = mc();
		net.minecraft.client.gui.screens.Screen parent = new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(new net.minecraft.client.gui.screens.TitleScreen());
		net.minecraft.client.gui.screens.ConnectScreen.startConnecting(parent, m, net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(d.ip), d);
		return true;
	}

	@Override
	public void setAttackIndicator(int mode) {
		net.minecraft.client.AttackIndicatorStatus v = net.minecraft.client.AttackIndicatorStatus.values()[Math.max(0, Math.min(2, mode))];
		mc().options.attackIndicator().set(v);
	}

	@Override
	public int blockLight(int x, int y, int z) {
		Minecraft m = mc();
		return m.level == null ? 15 : m.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new net.minecraft.core.BlockPos(x, y, z));
	}

	@Override
	public boolean spawnSurface(int x, int y, int z) {
		Minecraft m = mc();
		if (m.level == null) return false;
		net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(x, y, z), below = p.below();
		return m.level.getBlockState(p).getCollisionShape(m.level, p).isEmpty() && m.level.getBlockState(below).isFaceSturdy(m.level, below, net.minecraft.core.Direction.UP);
	}

	@Override
	public void setGuiScale(int scale) {
		mc().options.guiScale().set(Math.max(0, scale));
		mc().resizeDisplay();
	}

	@Override
	public java.util.List<String[]> keyMappings() {
		java.util.List<String[]> out = new java.util.ArrayList<>();
		for (net.minecraft.client.KeyMapping k : mc().options.keyMappings) {
			Object key = k.getTranslatedKeyMessage();
			String keyName = key instanceof net.minecraft.network.chat.Component ? ((net.minecraft.network.chat.Component) key).getString() : String.valueOf(key);
			out.add(new String[] {k.getName(), net.minecraft.client.resources.language.I18n.get(k.getName()), net.minecraft.client.resources.language.I18n.get(k.getCategory()), keyName});
		}
		return out;
	}

	@Override
	public void rebindKey(String id, int key) {
		for (net.minecraft.client.KeyMapping k : mc().options.keyMappings) {
			if (k.getName().equals(id)) k.setKey(key < 0 ? com.mojang.blaze3d.platform.InputConstants.UNKNOWN : com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(key));
		}
		net.minecraft.client.KeyMapping.resetMapping();
		mc().options.save();
	}

	@Override
	public java.util.List<String[]> resourcePackList() {
		net.minecraft.server.packs.repository.PackRepository r = mc().getResourcePackRepository();
		r.reload();
		java.util.List<String[]> out = new java.util.ArrayList<>();
		java.util.Collection<String> on = r.getSelectedIds();
		for (String id : on) {
			net.minecraft.server.packs.repository.Pack p = r.getPack(id);
			if (p != null && !p.isRequired()) out.add(new String[] {id, "1", p.getTitle().getString()});
		}
		for (String id : r.getAvailableIds()) {
			net.minecraft.server.packs.repository.Pack p = r.getPack(id);
			if (!on.contains(id) && p != null && !p.isRequired()) out.add(new String[] {id, "0", p.getTitle().getString()});
		}
		return out;
	}

	@Override
	public void applyResourcePacks(java.util.List<String> ids) {
		net.minecraft.server.packs.repository.PackRepository r = mc().getResourcePackRepository();
		java.util.List<String> all = new java.util.ArrayList<>();
		for (String id : r.getSelectedIds()) {
			net.minecraft.server.packs.repository.Pack p = r.getPack(id);
			if (p != null && p.isRequired()) all.add(id);
		}
		for (String id : ids) if (!all.contains(id)) all.add(id);
		r.setSelected(all);
		mc().options.updateResourcePacks(r);
	}

	@Override
	public void postEffect(String name) {
		if (name == null) mc().gameRenderer.shutdownEffect();
		else ((com.tatnat.client.mixin.PostEffectAccess) mc().gameRenderer).tatnat$loadEffect(new net.minecraft.resources.ResourceLocation("shaders/post/" + name + ".json"));
	}

	@Override
	public float horseJump() {
		Minecraft m = mc();
		return m.player != null && m.player.jumpableVehicle() != null ? m.player.getJumpRidingScale() : -1;
	}

	@Override
	public void setChatLook(double opacity, double scale, double width) {
		mc().options.textBackgroundOpacity().set(opacity);
		mc().options.chatScale().set(scale);
		mc().options.chatWidth().set(width);
	}

	@Override
	public float masterVolume() {
		return mc().options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER);
	}

	@Override
	public void setMasterVolume(float volume) {
		mc().options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set((double) Math.max(0, Math.min(1, volume)));
	}

	@Override
	public boolean underwater() {
		Minecraft m = mc();
		return m.player != null && m.player.isUnderWater();
	}

	// ------------------------------------------------------------ camera

	@Override
	public CameraInfo camera() {
		Camera cam = mc().gameRenderer.getMainCamera();
		return new CameraInfo(cam.getPosition().x, cam.getPosition().y, cam.getPosition().z, cam.getYRot(), cam.getXRot(), lastFov);
	}

	// ------------------------------------------------------------ actions

	@Override
	public void sendChat(String message) {
		if (mc().player != null) mc().player.connection.sendChat(message);
	}

	@Override
	public void sendCommand(String command) {
		if (mc().player != null) mc().player.connection.sendCommand(command);
	}

	@Override
	public void openChat(String prefill) {
		mc().setScreen(new ChatScreen(prefill));
	}

	@Override
	public void openScreen(UiScreen screen) {
		mc().setScreen(new ScreenBridge(screen));
	}

	/** What the title-screen button does. */
	public static void openModMenu() {
		INSTANCE.openScreen(new com.tatnat.client.ui.clickgui.ClickGuiScreen());
	}

	@Override
	public void closeScreen() {
		mc().setScreen(null);
	}

	@Override
	public void openPath(Path folder) {
		Util.getPlatform().openFile(folder.toFile());
	}

	@Override
	public void openUrl(String url) {
		Util.getPlatform().openUri(URI.create(url));
	}

	@Override
	public void execute(Runnable r) {
		mc().execute(r);
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
		return SharedConstants.getCurrentVersion().getName();
	}
}
