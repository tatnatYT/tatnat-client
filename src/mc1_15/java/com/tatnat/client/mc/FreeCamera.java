package com.tatnat.client.mc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;

/**
 * The invisible "body" the camera follows in Freecam. Never added to the world, never sent to the
 * server: it only exists so {@code Minecraft.setCameraEntity} has something to look through.
 */
public class FreeCamera extends Entity {
	public FreeCamera(ClientLevel level) {
		super(EntityType.AREA_EFFECT_CLOUD, level);
		noPhysics = true;
	}

	/** Moves without interpolation lag: last position becomes "old" first. */
	public void moveBy(double dx, double dy, double dz) {
		xo = getX();
		yo = getY();
		zo = getZ();
		setPos(getX() + dx, getY() + dy, getZ() + dz);
	}

	@Override
	protected void defineSynchedData() {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	public net.minecraft.network.protocol.Packet<?> getAddEntityPacket() {
		return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
	}
}
