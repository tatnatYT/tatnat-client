package com.tatnat.client.mc;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * The invisible "body" the camera follows in Freecam. Never added to the world, never sent to the
 * server: it only exists so the game has something to look through. Zero height puts its eye
 * exactly at its position.
 */
public class FreeCamera extends Entity {
	public FreeCamera(World world) {
		super(world);
		noClip = true;
		width = 0f;
		height = 0f;
	}

	/** Moves without interpolation lag: last position becomes "old" first. */
	public void moveBy(double dx, double dy, double dz) {
		prevX = prevTickX = x;
		prevY = prevTickY = y;
		prevZ = prevTickZ = z;
		updatePosition(x + dx, y + dy, z + dz);
	}

	@Override
	protected void initDataTracker() {
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound tag) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound tag) {
	}
}
