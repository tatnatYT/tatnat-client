package com.tatnat.client.modules.impl.hud;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.ModeSetting;

/**
 * {@code CPU: 12%}: processor load, for the whole computer or just Minecraft. Java can't read GPU
 * load, so this shows the CPU only. Sampled once a second (the reading itself has a cost).
 */
public class SystemResources extends TextHudModule {
	private final ModeSetting source = add(new ModeSetting("Measure", "The whole computer, or only Minecraft", "Whole PC", "Whole PC", "Minecraft"));

	private long sampledAt;
	private int cpu = -1;

	public SystemResources() {
		super("System Resources", "Shows how busy your processor is", false, 0.32, 0.30);
		icon = com.tatnat.client.ui.render.Icons.Icon.CHIP;
	}

	@Override
	protected String label() {
		return "CPU";
	}

	@Override
	protected String value(boolean preview) {
		if (preview) return "12%";
		long now = System.currentTimeMillis();
		if (now - sampledAt > 1000) {
			sampledAt = now;
			cpu = read(source.is("Minecraft"));
		}
		return cpu < 0 ? "n/a" : cpu + "%";
	}

	@Override
	protected int valueColor(boolean preview) {
		int v = preview ? 12 : cpu;
		return v >= 90 ? 0xFFFF5555 : v >= 60 ? 0xFFFFFF55 : 0;
	}

	private static int read(boolean process) {
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		if (!(os instanceof com.sun.management.OperatingSystemMXBean)) return -1;
		com.sun.management.OperatingSystemMXBean sun = (com.sun.management.OperatingSystemMXBean) os;
		@SuppressWarnings("deprecation")
		double load = process ? sun.getProcessCpuLoad() : sun.getSystemCpuLoad();
		return load < 0 ? -1 : (int) Math.round(load * 100);
	}
}
