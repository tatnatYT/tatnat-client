package com.tatnat.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

/** Lets the batched fills (mc.GuiRects) be submitted as one element. */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsAccess {
	@Accessor("guiRenderState")
	GuiRenderState tatnat$guiRenderState();
}
