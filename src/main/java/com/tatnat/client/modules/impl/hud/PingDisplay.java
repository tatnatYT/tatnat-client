package com.tatnat.client.modules.impl.hud;

import com.tatnat.client.modules.TextHudModule;
import com.tatnat.client.modules.settings.BooleanSetting;

import net.minecraft.client.multiplayer.PlayerInfo;

/** {@code [Ping: 24ms]}, taken from the server's own tab-list latency for you. */
public class PingDisplay extends TextHudModule {
	private final BooleanSetting colorCode = add(new BooleanSetting("Color Code", "Green under 80ms, yellow under 150ms, red above", true));

	public PingDisplay() {
		super("Ping Display", "Shows your connection latency to the server", false, 0.0, 0.30);
		icon = com.tatnat.client.ui.render.Icons.Icon.SIGNAL;
	}

	@Override
	public boolean hasContent() {
		return mc.getConnection() != null;
	}

	private int ping() {
		if (mc.getConnection() == null || mc.player == null) return 0;
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return info == null ? 0 : info.getLatency();
	}

	@Override
	protected String label() {
		return "Ping";
	}

	@Override
	protected String value(boolean preview) {
		return ping() + "ms";
	}

	@Override
	protected int valueColor(boolean preview) {
		if (!colorCode.on()) return 0;
		int p = ping();
		return p < 80 ? 0xFF55FF55 : p < 150 ? 0xFFFFFF55 : 0xFFFF5555;
	}
}
