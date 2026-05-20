package net.minecraft.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public interface ContextSwapper {
   <T> DataResult<T> swapContext(Codec<T> codec, T value, RegistryWrapper.WrapperLookup registries);
}
