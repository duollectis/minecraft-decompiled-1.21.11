package com.mojang.blaze3d.systems;

import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface GpuQuery extends AutoCloseable {
   OptionalLong getValue();

   @Override
   void close();
}
