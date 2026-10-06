package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;

/** Lets the batched fills (mc.GuiRects) be submitted as one element. */
@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccess {
	@Accessor("guiRenderState")
	GuiRenderState tatnat$guiRenderState();
}
