package com.mojang.blaze3d.pipeline;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
/**
 * {@code CompiledRenderPipeline}.
 */
public interface CompiledRenderPipeline {

	boolean isValid();
}
