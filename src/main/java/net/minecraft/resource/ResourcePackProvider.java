package net.minecraft.resource;

import java.util.function.Consumer;

@FunctionalInterface
public interface ResourcePackProvider {
   void register(Consumer<ResourcePackProfile> profileAdder);
}
