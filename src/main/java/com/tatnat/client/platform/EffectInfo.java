package com.tatnat.client.platform;

/** An active potion effect: icon handle, translated name, level and remaining time. */
public final class EffectInfo {
	public final Object icon;
	public final String name;
	/** 1 = level I. */
	public final int level;
	public final String duration;
	/** Less than 10 seconds left (and not infinite). */
	public final boolean ending;

	public EffectInfo(Object icon, String name, int level, String duration, boolean ending) {
		this.icon = icon;
		this.name = name;
		this.level = level;
		this.duration = duration;
		this.ending = ending;
	}
}
