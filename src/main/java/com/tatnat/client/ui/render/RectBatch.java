package com.tatnat.client.ui.render;

import com.tatnat.client.platform.Gfx;

/**
 * Collects consecutive solid rectangles and hands them to the platform in one {@link Gfx#rects}
 * call. The menus are built from thousands of tiny fills (anti-aliased corners, line-art icons),
 * and on many Minecraft versions every fill is its own draw call, which made the menu lag.
 * Anything that isn't a rectangle flushes first, so drawing order and transforms stay correct.
 */
public final class RectBatch implements Gfx {
	private final Gfx g;
	private int[] data = new int[5 * 512];
	private int count;

	private RectBatch(Gfx g) {
		this.g = g;
	}

	/** Wraps {@code g}; call {@link #flush()} when done drawing. */
	public static RectBatch of(Gfx g) {
		return g instanceof RectBatch ? (RectBatch) g : new RectBatch(g);
	}

	public void flush() {
		if (count == 0) return;
		g.rects(data, count);
		count = 0;
	}

	@Override
	public void rect(int x1, int y1, int x2, int y2, int argb) {
		if (x2 <= x1 || y2 <= y1 || (argb >>> 24) == 0) return;
		if ((count + 1) * 5 > data.length) data = java.util.Arrays.copyOf(data, data.length * 2);
		int i = count++ * 5;
		data[i] = x1;
		data[i + 1] = y1;
		data[i + 2] = x2;
		data[i + 3] = y2;
		data[i + 4] = argb;
	}

	@Override
	public void rects(int[] d, int n) {
		for (int i = 0; i < n; i++) rect(d[i * 5], d[i * 5 + 1], d[i * 5 + 2], d[i * 5 + 3], d[i * 5 + 4]);
	}

	@Override
	public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
		flush();
		g.gradient(x1, y1, x2, y2, top, bottom);
	}

	@Override
	public void push() {
		flush();
		g.push();
	}

	@Override
	public void pop() {
		flush();
		g.pop();
	}

	@Override
	public void translate(float x, float y) {
		flush();
		g.translate(x, y);
	}

	@Override
	public void scale(float x, float y) {
		flush();
		g.scale(x, y);
	}

	@Override
	public void scissor(int x1, int y1, int x2, int y2) {
		flush();
		g.scissor(x1, y1, x2, y2);
	}

	@Override
	public void endScissor() {
		flush();
		g.endScissor();
	}

	@Override
	public void uiText(int weight, int px, String s, int x, int y, int argb) {
		flush();
		g.uiText(weight, px, s, x, y, argb);
	}

	@Override
	public int uiTextWidth(int weight, int px, String s) {
		return g.uiTextWidth(weight, px, s);
	}

	@Override
	public void mcText(String s, int x, int y, int argb, boolean shadow, boolean bold) {
		flush();
		g.mcText(s, x, y, argb, shadow, bold);
	}

	@Override
	public int mcTextWidth(String s, boolean bold) {
		return g.mcTextWidth(s, bold);
	}

	@Override
	public void logo(int x, int y, int size, int argb) {
		flush();
		g.logo(x, y, size, argb);
	}

	@Override
	public void item(Object stack, int x, int y) {
		flush();
		g.item(stack, x, y);
	}

	@Override
	public void effectIcon(Object effect, int x, int y, int size) {
		flush();
		g.effectIcon(effect, x, y, size);
	}
}
