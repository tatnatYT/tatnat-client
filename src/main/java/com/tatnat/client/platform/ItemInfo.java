package com.tatnat.client.platform;

/** An item to draw on the HUD: an opaque stack handle plus the numbers we show next to it. */
public final class ItemInfo {
	public final Object stack;
	public final int count;
	public final boolean damageable;
	public final int damage;
	public final int maxDamage;

	public ItemInfo(Object stack, int count, boolean damageable, int damage, int maxDamage) {
		this.stack = stack;
		this.count = count;
		this.damageable = damageable;
		this.damage = damage;
		this.maxDamage = maxDamage;
	}

	public int left() {
		return maxDamage - damage;
	}
}
