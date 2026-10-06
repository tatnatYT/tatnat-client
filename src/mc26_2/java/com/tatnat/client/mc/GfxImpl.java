package com.tatnat.client.mc;

import java.util.HashMap;
import java.util.Map;

import com.tatnat.client.TatnatClient;
import com.tatnat.client.platform.Gfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

/** {@link Gfx} on top of 26.1's GuiGraphicsExtractor. One per frame (it just wraps the graphics). */
public final class GfxImpl implements Gfx {
	private static final Identifier LOGO = Identifier.fromNamespaceAndPath(TatnatClient.ID, "logo.png");
	private static final Map<Integer, Style> UI_STYLES = new HashMap<>();
	private static final Style BOLD = Style.EMPTY.withBold(true);

	private final GuiGraphicsExtractor g;
	private final Minecraft mc = Minecraft.getInstance();

	public GfxImpl(GuiGraphicsExtractor g) {
		this.g = g;
	}

	/** Inter at an exact pixel size: one font definition per size (assets/tatnatclient/font). */
	static Component uiComponent(int weight, int px, String s) {
		Style style = UI_STYLES.computeIfAbsent(weight * 100 + px, k -> Style.EMPTY.withFont(
				new FontDescription.Resource(Identifier.fromNamespaceAndPath(TatnatClient.ID, "w" + weight + "_" + px))));
		return Component.literal(s).withStyle(style);
	}

	static Component mcComponent(String s, boolean bold) {
		return bold ? Component.literal(s).withStyle(BOLD) : Component.literal(s);
	}

	@Override
	public void rect(int x1, int y1, int x2, int y2, int argb) {
		g.fill(x1, y1, x2, y2, argb);
	}

	@Override
	public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
		g.fillGradient(x1, y1, x2, y2, top, bottom);
	}

	@Override
	public void push() {
		g.pose().pushMatrix();
	}

	@Override
	public void pop() {
		g.pose().popMatrix();
	}

	@Override
	public void translate(float x, float y) {
		g.pose().translate(x, y);
	}

	@Override
	public void scale(float x, float y) {
		g.pose().scale(x, y);
	}

	@Override
	public void scissor(int x1, int y1, int x2, int y2) {
		g.enableScissor(x1, y1, x2, y2);
	}

	@Override
	public void endScissor() {
		g.disableScissor();
	}

	@Override
	public void uiText(int weight, int px, String s, int x, int y, int argb) {
		// Minecraft puts every font's baseline 7px below the given y; move it to ~80% of the em box.
		g.text(mc.font, uiComponent(weight, px, s), x, y + Math.round(px * 0.8f) - 7, argb, false);
	}

	@Override
	public int uiTextWidth(int weight, int px, String s) {
		return mc.font.width(uiComponent(weight, px, s));
	}

	@Override
	public void mcText(String s, int x, int y, int argb, boolean shadow, boolean bold) {
		if (bold) g.text(mc.font, mcComponent(s, true), x, y, argb, shadow);
		else g.text(mc.font, s, x, y, argb, shadow);
	}

	@Override
	public int mcTextWidth(String s, boolean bold) {
		return bold ? mc.font.width(mcComponent(s, true)) : mc.font.width(s);
	}

	@Override
	public void logo(int x, int y, int size, int argb) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(size / 64f, size / 64f);
		g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, 64, 64, 64, 64, argb);
		g.pose().popMatrix();
	}

	@Override
	public void item(Object stack, int x, int y) {
		ItemStack s = (ItemStack) stack;
		g.item(s, x, y);
		g.itemDecorations(mc.font, s, x, y, "");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void effectIcon(Object effect, int x, int y, int size) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, net.minecraft.client.gui.Hud.getMobEffectSprite((Holder<MobEffect>) effect), x, y, size, size);
	}
}
