package com.tatnat.client.platform;

/**
 * A nearby entity for world overlays (TNT Timer, Item Despawn, Damage Indicator, Loot Beams):
 * where it is and the few facts those need. {@code top} is the top of its hitbox.
 */
public final class EntityInfo {
	public enum Kind { TNT, ITEM, PLAYER, MOB }

	public final Kind kind;
	public final double x, top, z;
	/** TNT: fuse ticks left. Item: age in ticks. Otherwise 0. */
	public final int ticks;
	/** Item: display name and stack size. */
	public final String name;
	public final int count;
	/** Living entities: health. */
	public final float health, maxHealth;

	private EntityInfo(Kind kind, double x, double top, double z, int ticks, String name, int count, float health, float maxHealth) {
		this.kind = kind;
		this.x = x;
		this.top = top;
		this.z = z;
		this.ticks = ticks;
		this.name = name;
		this.count = count;
		this.health = health;
		this.maxHealth = maxHealth;
	}

	public static EntityInfo tnt(double x, double top, double z, int fuse) {
		return new EntityInfo(Kind.TNT, x, top, z, fuse, "", 0, 0, 0);
	}

	public static EntityInfo item(double x, double top, double z, int age, String name, int count) {
		return new EntityInfo(Kind.ITEM, x, top, z, age, name, count, 0, 0);
	}

	public static EntityInfo living(double x, double top, double z, boolean player, float health, float maxHealth) {
		return new EntityInfo(player ? Kind.PLAYER : Kind.MOB, x, top, z, 0, "", 0, health, maxHealth);
	}
}
