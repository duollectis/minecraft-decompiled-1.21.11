package com.mojang.blaze3d.buffers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface GpuFence extends AutoCloseable {
   @Override
   void close();

   boolean awaitCompletion(long timeoutNanos);
}
