package com.tatnat.client.modules.impl.visual;

import org.lwjgl.glfw.GLFW;

import com.tatnat.client.event.Events;
import com.tatnat.client.event.Subscribe;
import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.BooleanSetting;
import com.tatnat.client.modules.settings.KeybindSetting;
import com.tatnat.client.modules.settings.SliderSetting;

/**
 * OptiFine-style zoom: hold the key (C by default) and the field of view eases down.
 *
 * The FOV is divided in {@code GameRendererMixin#getFov}; mouse sensitivity is divided by the
 * same factor in {@code MouseHandlerMixin} so aiming feels the same while zoomed.
 */
public class Zoom extends Module {
	public static Zoom INSTANCE;

	public final KeybindSetting key = add(new KeybindSetting("Zoom Key", "Hold this key to zoom", GLFW.GLFW_KEY_C));
	public final SliderSetting amount = add(new SliderSetting("Zoom Amount", "How far to zoom in", 4.0, 1.5, 50.0, 0.5, "x"));
	public final BooleanSetting smooth = add(new BooleanSetting("Smooth Zoom", "Glide in and out instead of snapping", true));
	public final BooleanSetting cinematic = add(new BooleanSetting("Cinematic Camera", "Smooth, floaty camera while zoomed", false));
	public final BooleanSetting scroll = add(new BooleanSetting("Scroll to Adjust", "Mouse wheel changes the zoom while held", true));
	public final BooleanSetting sensitivity = add(new BooleanSetting("Lower Sensitivity", "Slow the mouse down while zoomed so aiming stays accurate", true));

	/** Current zoom divisor (1 = no zoom), animated towards {@link #target()}. */
	private double current = 1.0;
	/** Extra multiplier from scrolling, reset when the key is released. */
	private double scrollFactor = 1.0;
	private long lastUpdate = System.nanoTime();
	private boolean wasZooming;
	private boolean savedSmoothCamera;

	public Zoom() {
		super("Zoom", "Hold C to zoom in like a spyglass", Category.VISUAL, true);
		icon = com.tatnat.client.ui.render.Icons.Icon.ZOOM;
		INSTANCE = this;
	}

	/** Dev test only: zoom as if the key were held. */
	public static boolean devForce;

	public boolean zooming() {
		return isEnabled() && mc.screen == null && (key.isDown() || devForce);
	}

	private double target() {
		return zooming() ? Math.max(1.0, amount.get() * scrollFactor) : 1.0;
	}

	/** Called every frame from the FOV hook. Returns the divisor to apply to the FOV. */
	public double update() {
		long now = System.nanoTime();
		double dt = Math.min(0.1, (now - lastUpdate) / 1e9);
		lastUpdate = now;

		boolean z = zooming();
		if (z != wasZooming) {
			wasZooming = z;
			if (z) {
				savedSmoothCamera = mc.options.smoothCamera;
				if (cinematic.on()) mc.options.smoothCamera = true;
			} else {
				mc.options.smoothCamera = savedSmoothCamera;
				scrollFactor = 1.0;
			}
		}
		double t = target();
		if (!smooth.on()) current = t;
		else current += (t - current) * (1 - Math.exp(-dt * 14));
		if (Math.abs(current - t) < 0.001) current = t;
		return current;
	}

	public double current() {
		return isEnabled() ? current : 1.0;
	}

	@Subscribe
	public void onScroll(Events.Scroll e) {
		if (!zooming() || !scroll.on()) return;
		scrollFactor = Math.max(0.25, Math.min(8.0, scrollFactor * (e.amount > 0 ? 1.2 : 1 / 1.2)));
		e.cancel();
	}

	@Override
	protected void onDisable() {
		if (wasZooming) mc.options.smoothCamera = savedSmoothCamera;
		wasZooming = false;
		current = 1.0;
	}
}
