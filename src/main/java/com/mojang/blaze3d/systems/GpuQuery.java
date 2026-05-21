package com.mojang.blaze3d.systems;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

import java.util.OptionalLong;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
/**
 * {@code GpuQuery}.
 */
public interface GpuQuery extends AutoCloseable {

	OptionalLong getValue();

	@Override
	void close();
}
