package com.tatnat.client.mixin;

import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tatnat.client.mc.FeaturesImpl;
import com.tatnat.client.mc.HitboxFilter;
import com.tatnat.client.modules.impl.utility.Freecam;
import com.tatnat.client.modules.impl.visual.BlockOverlay;
import com.tatnat.client.modules.impl.visual.ClearWater;
import com.tatnat.client.modules.impl.cosmetic.EnchantGlint;
import com.tatnat.client.modules.impl.visual.FullBright;
import com.tatnat.client.modules.impl.visual.Hitboxes;
import com.tatnat.client.modules.impl.visual.TimeChanger;
import com.tatnat.client.modules.impl.visual.Zoom;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.level.LevelProperties;

/** World look hooks for the legacy versions: lighting, fog, outline, hitboxes, hit flash, glint, time. */
public final class WorldMixins {
	private WorldMixins() {
	}

	/** Full Bright: the lightmap reads the gamma option; hand it the module's level instead. */
	@Mixin(GameRenderer.class)
	public static class Light {
		// Plain Mixin 0.7 redirects throughout (Forge 1.8.9 has no MixinExtras).
		@Redirect(method = "updateLightmap", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;gamma:F", opcode = Opcodes.GETFIELD))
		private float tatnat$fullBright(GameOptions options) {
			return FullBright.active() ? FullBright.INSTANCE.level.get().floatValue() : options.gamma;
		}

		/** Zoom: lower mouse sensitivity while zoomed in so aiming feels the same. */
		@Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;sensitivity:F", opcode = Opcodes.GETFIELD))
		private float tatnat$zoomSensitivity(GameOptions options) {
			float value = options.sensitivity;
			Zoom zoom = Zoom.INSTANCE;
			if (zoom != null && zoom.sensitivity.on()) {
				double factor = zoom.current();
				if (factor > 1.0) return (float) (value / Math.sqrt(factor) * 0.9);
			}
			return value;
		}

		/** Clear Water: underwater fog is exponential here; thin it out by the module strength. */
		@Redirect(method = "renderFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;fogDensity(F)V"))
		private void tatnat$clearWater(float density) {
			Entity cam = MinecraftClient.getInstance().getCameraEntity();
			if (ClearWater.active() && cam != null && cam.isSubmergedIn(Material.WATER)) {
				density *= 1f - 0.95f * ClearWater.INSTANCE.strength.floatValue() / 100f;
			}
			GlStateManager.fogDensity(density);
		}
	}

	/** Block Overlay: thick coloured outline and a tinted face instead of vanilla's thin lines. */
	@Mixin(WorldRenderer.class)
	public static class Outline {
		@Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
		private void tatnat$blockOverlay(PlayerEntity player, BlockHitResult hit, int pass, float partialTick, CallbackInfo ci) {
			BlockOverlay overlay = BlockOverlay.INSTANCE;
			if (overlay == null || !overlay.isEnabled() || pass != 0 || hit == null || hit.type != BlockHitResult.Type.BLOCK) return;
			ci.cancel();
			MinecraftClient mc = MinecraftClient.getInstance();
			BlockPos pos = hit.getBlockPos();
			Block block = mc.world.getBlockState(pos).getBlock();
			if (block.getMaterial() == Material.AIR || !mc.world.getWorldBorder().contains(pos)) return;
			block.setBoundingBox(mc.world, pos);
			double cx = player.prevTickX + (player.x - player.prevTickX) * partialTick;
			double cy = player.prevTickY + (player.y - player.prevTickY) * partialTick;
			double cz = player.prevTickZ + (player.z - player.prevTickZ) * partialTick;
			Box box = block.getSelectionBox(mc.world, pos).offset(-cx, -cy, -cz);
			double dist = Math.sqrt((box.minX + box.maxX) * (box.minX + box.maxX) / 4 + (box.minY + box.maxY) * (box.minY + box.maxY) / 4
					+ (box.minZ + box.maxZ) * (box.minZ + box.maxZ) / 4);
			float t = (float) (overlay.thickness.get() * 0.0028 * Math.max(1.0, dist));
			GlStateManager.disableTexture();
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
			GlStateManager.disableCull();
			GlStateManager.depthMask(false);
			Tessellator tes = Tessellator.getInstance();
			BufferBuilder b = tes.getBuffer();
			b.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
			com.tatnat.client.mc.LegacyOutline.edges(b, box, t, overlay.outline());
			if (overlay.fillFace.on() && overlay.fillOpacity.get() > 0) com.tatnat.client.mc.LegacyOutline.face(b, box, hit.direction, overlay.fill());
			tes.draw();
			GlStateManager.depthMask(true);
			GlStateManager.enableCull();
			GlStateManager.enableTexture();
			GlStateManager.disableBlend();
		}

	}

	/** Hitboxes: switch on the game's own F3+B boxes for the chosen entity types, in our colour. */
	@Mixin(EntityRenderDispatcher.class)
	public static class HitboxesOn {
		@Shadow
		private boolean renderHitboxes;

		private static Entity tatnat$entity;

		@Inject(method = "method_6913", at = @At("HEAD"))
		private void tatnat$remember(Entity entity, double x, double y, double z, float yaw, float tickDelta, boolean hideLabel,
				CallbackInfoReturnable<Boolean> cir) {
			tatnat$entity = entity;
		}

		@Redirect(method = "method_6913", at = @At(value = "FIELD",
				target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderHitboxes:Z", opcode = Opcodes.GETFIELD))
		private boolean tatnat$hitboxes(EntityRenderDispatcher self) {
			return renderHitboxes || Hitboxes.active() && HitboxFilter.wanted(tatnat$entity);
		}

		/** No body in your own view while flying around in Freecam. */
		@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
		private void tatnat$hideBody(Entity entity, net.minecraft.client.render.CameraView view, double x, double y, double z,
				CallbackInfoReturnable<Boolean> cir) {
			if (Freecam.active() && Freecam.INSTANCE.hideBody() && entity == MinecraftClient.getInstance().player) cir.setReturnValue(false);
		}
	}

	/** Hit Color: hurt mobs are tinted with the first four values put in the overlay buffer. */
	@Mixin(LivingEntityRenderer.class)
	public static class HitFlash {
		private static final String M = "method_10252(Lnet/minecraft/entity/LivingEntity;FZ)Z";
		private static final String PUT = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;";

		@ModifyArg(method = M, at = @At(value = "INVOKE", target = PUT, ordinal = 0))
		private float tatnat$r(float v) {
			int c = FeaturesImpl.hurtColor;
			return c == 0 ? v : ((c >> 16) & 255) / 255f;
		}

		@ModifyArg(method = M, at = @At(value = "INVOKE", target = PUT, ordinal = 1))
		private float tatnat$g(float v) {
			int c = FeaturesImpl.hurtColor;
			return c == 0 ? v : ((c >> 8) & 255) / 255f;
		}

		@ModifyArg(method = M, at = @At(value = "INVOKE", target = PUT, ordinal = 2))
		private float tatnat$b(float v) {
			int c = FeaturesImpl.hurtColor;
			return c == 0 ? v : (c & 255) / 255f;
		}

		/** Hit Color stores opacity inverted (for the newer shader); here it is the tint strength. */
		@ModifyArg(method = M, at = @At(value = "INVOKE", target = PUT, ordinal = 3))
		private float tatnat$strength(float v) {
			int c = FeaturesImpl.hurtColor;
			return c == 0 ? v : 1f - ((c >>> 24) & 255) / 255f;
		}
	}

	/** Enchant Glint speed: the glint scrolls with the clock, so scale the clock it sees. */
	@Mixin(ItemRenderer.class)
	public static class GlintSpeed {
		@Redirect(method = "renderGlint", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getTime()J"))
		private long tatnat$speed() {
			long t = MinecraftClient.getTime();
			return EnchantGlint.active() ? (long) (t * EnchantGlint.INSTANCE.speed.get()) : t;
		}
	}

	/** Time Changer: the sky reads the level's time; only the client thread sees the change. */
	@Mixin(LevelProperties.class)
	public static class DayTime {
		// HEAD, not RETURN: Mixin 0.7 (Forge 1.8.9) emits a broken dup for RETURN injections on long methods.
		@Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
		private void tatnat$time(CallbackInfoReturnable<Long> cir) {
			if (TimeChanger.active() && MinecraftClient.getInstance().isOnThread()) cir.setReturnValue(TimeChanger.INSTANCE.time.get().longValue());
		}
	}

	@Mixin(World.class)
	public static class Weather {
		@Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
		private void tatnat$rain(float partialTick, CallbackInfoReturnable<Float> cir) {
			if (!((Object) this instanceof ClientWorld) || !TimeChanger.active() || TimeChanger.INSTANCE.weather.is("Server")) return;
			cir.setReturnValue(TimeChanger.INSTANCE.weather.is("Clear") ? 0f : 1f);
		}

		@Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
		private void tatnat$thunder(float partialTick, CallbackInfoReturnable<Float> cir) {
			if (!((Object) this instanceof ClientWorld) || !TimeChanger.active() || TimeChanger.INSTANCE.weather.is("Server")) return;
			cir.setReturnValue(TimeChanger.INSTANCE.weather.is("Thunder") ? 1f : 0f);
		}
	}
}
