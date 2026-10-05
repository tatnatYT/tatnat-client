package com.tatnat.client.mc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The invisible "body" the camera follows in Freecam. Never added to the world, never sent to the
 * server: it only exists so {@code Minecraft.setCameraEntity} has something to look through.
 */
public class FreeCamera extends Entity {
	public FreeCamera(ClientLevel level) {
		super(EntityType.MARKER, level);
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
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
