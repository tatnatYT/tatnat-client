package com.tatnat.client.modules.impl.utility;

import com.tatnat.client.modules.Category;
import com.tatnat.client.modules.Module;
import com.tatnat.client.modules.settings.ColorSetting;
import com.tatnat.client.modules.settings.SliderSetting;
import com.tatnat.client.ui.render.Icons;

/**
 * Lets you scroll tooltips that are taller than the screen (shulker boxes, books, heavily
 * enchanted gear) with the mouse wheel, and shows a scrollbar beside them. Hooks:
 * {@code AbstractContainerScreenMixin} for the wheel, {@code GuiGraphicsTooltipMixin} to shift
 * and decorate the tooltip.
 */
public class ScrollableTooltips extends Module {
	public static ScrollableTooltips INSTANCE;

	public final SliderSetting speed = add(new SliderSetting("Scroll Speed", "How far one wheel step moves", 12, 4, 40, 1, "px"));
	public final SliderSetting barWidth = add(new SliderSetting("Scrollbar Width", "Width of the scrollbar", 2, 1, 6, 1, "px"));
	public final ColorSetting barColor = add(new ColorSetting("Scrollbar Color", "Colour of the scrollbar", 0xFFE5323E, true));

	/** Current scroll offset in GUI units (negative = scrolled down). */
	public float offset;
	private Object lastStack;

	public ScrollableTooltips() {
		super("Scrollable Tooltips", "Scroll long tooltips with the mouse wheel", Category.UTILITY, true);
		icon = Icons.Icon.SCROLL;
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}

	/** Resets the scroll whenever you hover a different item. */
	public void hovering(Object stack) {
		if (stack != lastStack) {
			lastStack = stack;
			offset = 0;
		}
	}

	public void scroll(double amount) {
		offset += (float) (amount * speed.get());
	}
}
